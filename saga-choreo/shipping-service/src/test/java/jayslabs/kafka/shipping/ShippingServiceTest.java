package jayslabs.kafka.shipping;

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
import jayslabs.kafka.common.events.shipping.ShippingEvent;
import jayslabs.kafka.common.events.shipping.ShippingStatus;
import jayslabs.kafka.shipping.application.repository.ShipmentRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

@TestPropertySource(properties = {
    "spring.cloud.function.definition=processor;orderEvtProducer;shippingEvtConsumer",
    "spring.cloud.stream.bindings.orderEvtProducer-out-0.destination=order-events",
    "spring.cloud.stream.bindings.shippingEvtConsumer-in-0.destination=shipping-events"
})
public class ShippingServiceTest extends AbstractIntegrationTest{

    private static final Sinks.Many<OrderEvent> reqSink = Sinks.many().unicast().onBackpressureBuffer();
    private static final Sinks.Many<ShippingEvent> respSink = Sinks.many().unicast().onBackpressureBuffer();
    private static final Flux<ShippingEvent> respFlux = respSink.asFlux().cache(0); //cache the response flux

    @Autowired
    private ShipmentRepository shiprepo;

    @Test
    public void planAndCancelTest(){

        //emit created event, expect no event
        var orderCreatedEvt = TestDataUtil.createOrderCreatedEvent(1, 1, 2, 3);
        expectNoEvent(orderCreatedEvt);

        // duplicate event, expect no event
        expectNoEvent(orderCreatedEvt);

        //check Shipment table for 1 record
        this.shiprepo.findByOrderIdAndStatus(orderCreatedEvt.orderId(), ShippingStatus.PENDING)
           .as(StepVerifier::create)
           .consumeNextWith(
            shipment -> {                
                Assertions.assertEquals(orderCreatedEvt.orderId(), shipment.getOrderId());
                //quantity
                Assertions.assertEquals(orderCreatedEvt.quantity(), shipment.getQuantity());
                // delivery date is null
                Assertions.assertNull(shipment.getDeliveryDate());
                // status is PENDING
                Assertions.assertEquals(ShippingStatus.PENDING, shipment.getStatus());
            }
           )
           .verifyComplete();

        //emit cancelled event, expect no event
        var orderCancelledEvt = TestDataUtil.createOrderCancelledEvent(orderCreatedEvt.orderId());
        expectNoEvent(orderCancelledEvt);

        //check Shipment table for 0 records
        this.shiprepo.findByOrderIdAndStatus(orderCreatedEvt.orderId(), ShippingStatus.PENDING)
           .as(StepVerifier::create)
           .verifyComplete();

    }
    
    @Test
    public void createAndScheduleTest(){

        //reserve inventory test
        //valid order created event
        var orderCreatedEvt = TestDataUtil.createOrderCreatedEvent(1, 1, 2, 3);
        expectNoEvent(orderCreatedEvt);

        //check if record added to shipment table
        this.shiprepo.findByOrderIdAndStatus(orderCreatedEvt.orderId(), ShippingStatus.PENDING)
           .as(StepVerifier::create)
           .consumeNextWith(
            shipment -> Assertions.assertEquals(orderCreatedEvt.orderId(), shipment.getOrderId())
           )
           .verifyComplete();

        //check duplicate event test
        //pass same order created event again
        expectNoEvent(orderCreatedEvt);

        //test order completed event and schedule shipment test
        var ordCompletedEvt = TestDataUtil.createOrderCompletedEvent(orderCreatedEvt.orderId());
        expectEvent(ordCompletedEvt, ShippingEvent.ShippingScheduled.class, e -> {
            Assertions.assertEquals(ordCompletedEvt.orderId(), e.orderId());
            Assertions.assertNotNull(e.shipmentId());
            Assertions.assertNotNull(e.expectedDeliveryDate());
            Assertions.assertNotNull(e.createdAt());
        });

        //test duplicate order completed event
        expectNoEvent(ordCompletedEvt);
                
    }

    private <T> void expectEvent(OrderEvent evt, Class<T> type, Consumer<T> assertion){
        respFlux //start listening for response (ShippingEvent)
           .doFirst(() -> reqSink.tryEmitNext(evt)) //use sink to emit the order event (OrderCreated/OrderCancelled)
           .next() //wait for Mono<ShippingEvent> to be emitted
           .timeout(Duration.ofSeconds(2), Mono.empty()) //timeout if no event is emitted in 2 second
           .cast(type) //cast ShippingEvent to the expected type (ShippingScheduled)
           .as(StepVerifier::create)
           .consumeNextWith(assertion)
           .verifyComplete();
      }
  
    private void expectNoEvent(OrderEvent evt){
        respFlux //start listening for response (ShippingEvent)
           .doFirst(() -> reqSink.tryEmitNext(evt)) //use sink to emit the order event - OrderCreated/OrderCancelled
           .next() //wait for Mono<ShippingEvent> to be emitted
           .timeout(Duration.ofSeconds(2), Mono.empty()) //timeout if no event is emitted in 2 second
           .as(StepVerifier::create)
           .verifyComplete();
    }

    @Test
    public void  scheduleWithoutCreateTest(){
        //OrderCancelled event with non-existing orderId
        var ordCancEvt = TestDataUtil.createOrderCancelledEvent(UUID.randomUUID());
        expectNoEvent(ordCancEvt);
    }



    @TestConfiguration
    static class TestConfig{

        @Bean
        public Supplier<Flux<OrderEvent>> orderEvtProducer(){
            return reqSink::asFlux; //converts messages from reqSink to a kafka message stream (flux)
        }

        @Bean
        public Consumer<Flux<ShippingEvent>> shippingEvtConsumer(){

            //Consumes messages from shipping-events topic
            return f -> f
            //For each ShippingEvent, emits it into respSink
            .doOnNext(respSink::tryEmitNext)
            .subscribe(); //activates the reactive stream
        }

    }
}
