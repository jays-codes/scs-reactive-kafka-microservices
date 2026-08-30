package jayslabs.kafka.payment;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;

import jayslabs.kafka.common.events.order.OrderEvent;
import jayslabs.kafka.common.events.payment.PaymentEvent;
import jayslabs.kafka.payment.application.repository.CustomerRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;


@TestPropertySource(properties = {
    "spring.cloud.function.definition=processor;orderEvtProducer;paymentEvtConsumer",
    "spring.cloud.stream.bindings.orderEvtProducer-out-0.destination=order-events",
    "spring.cloud.stream.bindings.paymentEvtConsumer-in-0.destination=payment-events"
})
public class PaymentServiceTest extends AbstractIntegrationTest{

    private static final Sinks.Many<OrderEvent> reqSink = Sinks.many().unicast().onBackpressureBuffer();
    private static final Sinks.Many<PaymentEvent> respSink = Sinks.many().unicast().onBackpressureBuffer();
    private static final Flux<PaymentEvent> respFlux = respSink.asFlux().cache(0); //cache the response flux

    @Autowired
    private CustomerRepository custrepo;

    @Test
    public void deductAndRefundTest(){

        //deduct payment test
        //valid order created event
        var orderCreatedEvt = TestDataUtil.createOrderCreatedEvent(1, 1, 2, 3);
        expectEvent(orderCreatedEvt, PaymentEvent.PaymentDeducted.class, e -> {
            Assertions.assertNotNull(e.paymentId());
            Assertions.assertEquals(orderCreatedEvt.orderId(), e.orderId());
            Assertions.assertEquals(6, e.amount());
        });

        //check balance test
        this.custrepo.findById(1)
           .as(StepVerifier::create)
           .consumeNextWith(
            cust -> Assertions.assertEquals(94, cust.getBalance())
           )
           .verifyComplete();

        //check duplicate event test
        //pass same order created event again
        expectNoEvent(orderCreatedEvt);

        //test cancelled event and refund test
        var ordCancEvt = TestDataUtil.createOrderCancelledEvent(orderCreatedEvt.orderId());
        expectEvent(ordCancEvt, PaymentEvent.PaymentRefunded.class, e -> {
            Assertions.assertNotNull(e.paymentId());
            Assertions.assertEquals(ordCancEvt.orderId(), e.orderId());
            Assertions.assertEquals(6, e.amount());
        });

        //check balance test after refund
        this.custrepo.findById(1)
           .as(StepVerifier::create)
           .consumeNextWith(
            cust -> Assertions.assertEquals(100, cust.getBalance())
           )
           .verifyComplete();        
        
    }

    @Test
    public void refundWithoutDeductTest(){
         //OrderCancelled event with non-existing orderId
         var ordCancEvt = TestDataUtil.createOrderCancelledEvent(UUID.randomUUID());  
         expectNoEvent(ordCancEvt);         
    }

    @Test
    public void customerNotFoundTest(){
      //OrderCreatedEvent with non-existing customerId
      var orderCreatedEvt = TestDataUtil.createOrderCreatedEvent(19, 1, 2, 3);

      expectEvent(
         orderCreatedEvt, PaymentEvent.PaymentFailed.class, e -> {
               Assertions.assertEquals(orderCreatedEvt.orderId(), e.orderId());
               Assertions.assertEquals(6, e.amount());
               Assertions.assertEquals("Customer not found", e.message());
      });
    }

    @Test
    public void insufficientBalanceTest(){
      var orderCreatedEvt = TestDataUtil.createOrderCreatedEvent(1, 1, 50, 3);

      expectEvent(
         orderCreatedEvt, PaymentEvent.PaymentFailed.class, e -> {
               Assertions.assertEquals(orderCreatedEvt.orderId(), e.orderId());
               Assertions.assertEquals(150, e.amount());
               Assertions.assertEquals("Customer does not have sufficient balance", e.message());
      });
    }

    /*
    */
    private <T> void expectEvent(OrderEvent evt, Class<T> type, Consumer<T> assertion){
      respFlux //start listening for response (PaymentEvent)
         .doFirst(() -> reqSink.tryEmitNext(evt)) //use sink to emit the order event (OrderCreated/OrderCancelled)
         .next() //wait for Mono<PaymentEvent> to be emitted
         .timeout(Duration.ofSeconds(2), Mono.empty()) //timeout if no event is emitted in 2 second
         .cast(type) //cast PaymentEvent to the expected type (PaymentDeducted/PaymentRefunded/PaymentFailed)
         .as(StepVerifier::create)
         .consumeNextWith(assertion)
         .verifyComplete();
    }

    private void expectNoEvent(OrderEvent evt){
      respFlux //start listening for response (PaymentEvent)
         .doFirst(() -> reqSink.tryEmitNext(evt)) //use sink to emit the order event (OrderCreated/OrderCancelled)
         .next() //wait for Mono<PaymentEvent> to be emitted
         .timeout(Duration.ofSeconds(2), Mono.empty()) //timeout if no event is emitted in 2 second
         .as(StepVerifier::create)
         .verifyComplete();
    }

    @TestConfiguration
    static class TestConfig{

        @Bean
        public Supplier<Flux<OrderEvent>> orderEvtProducer(){
            return reqSink::asFlux; //converts messages from reqSink to a kafka message stream (flux)
        }
        @Bean
        public Consumer<Flux<PaymentEvent>> paymentEvtConsumer(){

            //Consumes messages from payment-events topic
            return f -> f
            //For each PaymentEvent, emits it into respSink
            .doOnNext(respSink::tryEmitNext)
            .subscribe(); //activates the reactive stream
        }

    }
}

/** Message Flow Diagram
 * 
┌─────────────────────────────────────────────────────────────────────────┐
│                           TEST EXECUTION FLOW                           │
└─────────────────────────────────────────────────────────────────────────┘

1. TEST STARTS
   ↓
2. orderCreatedEvt = TestDataUtil.createOrderCreatedEvent(1, 1, 2, 3)
   ↓
3. respSink.asFlux().doFirst(() -> reqSink.tryEmitNext(orderCreatedEvt))
   ↓
┌──────────────────────────────────────────────────────────────────────────┐
│ KAFKA MESSAGE FLOW                                                       │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  reqSink                                                                 │
│     ↓                                                                    │
│  orderEvtProducer (TestConfig)                                           │
│     ↓                                                                    │
│  order-events topic (Embedded Kafka)                                     │
│     ↓                                                                    │
│  processor (OrderEventProcessorConfig) ← SYSTEM UNDER TEST               │
│     ├─ MessageConverter.toRecord()                                       │
│     ├─ OrderEventProcessorImpl.process()                                 │
│     │   └─ handle(OrderCreated)                                          │
│     │       ├─ PaymentServiceImpl.processPayment()                       │
│     │       │   ├─ DuplicateEventValidator.validate()                    │
│     │       │   ├─ CustomerRepository.findById(1)                        │
│     │       │   ├─ filter(balance >= amount)                             │
│     │       │   ├─ deductPayment()                                       │
│     │       │   │   ├─ CustomerRepository.save()                         │
│     │       │   │   └─ PaymentRepository.save()                          │
│     │       │   └─ map(EntityDTOMapper::toPaymentDTO)                    │
│     │       └─ map(EventDTOMapper::toPaymentDeductedEvent)               │
│     ├─ doOnSuccess(acknowledge())                                        │
│     └─ toMessage(PaymentEvent)                                           │
│     ↓                                                                    │
│  payment-events topic (Embedded Kafka)                                   │
│     ↓                                                                    │
│  paymentEvtConsumer (TestConfig)                                         │
│     ↓                                                                    │
│  respSink                                                                │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
   ↓
4. respSink.asFlux().next() receives PaymentEvent.PaymentDeducted
   ↓
5. StepVerifier assertions execute
   ↓
6. TEST PASSES ✓
    */

