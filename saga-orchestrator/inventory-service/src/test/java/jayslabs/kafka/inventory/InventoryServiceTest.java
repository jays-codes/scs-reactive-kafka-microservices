package jayslabs.kafka.inventory;

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

import jayslabs.kafka.common.events.inventory.InventoryEvent;
import jayslabs.kafka.common.events.order.OrderEvent;
import jayslabs.kafka.inventory.application.repository.ProductRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

@TestPropertySource(properties = {
    "spring.cloud.function.definition=processor;orderEvtProducer;inventoryEvtConsumer",
    "spring.cloud.stream.bindings.orderEvtProducer-out-0.destination=order-events",
    "spring.cloud.stream.bindings.inventoryEvtConsumer-in-0.destination=inventory-events"
})
public class InventoryServiceTest extends AbstractIntegrationTest{

    private static final Sinks.Many<OrderEvent> reqSink = Sinks.many().unicast().onBackpressureBuffer();
    private static final Sinks.Many<InventoryEvent> respSink = Sinks.many().unicast().onBackpressureBuffer();
    private static final Flux<InventoryEvent> respFlux = respSink.asFlux().cache(0); //cache the response flux

    @Autowired
    private ProductRepository prodrepo;

    @Test
    public void reserveAndRestoreTest(){

        //reserve inventory test
        //valid order created event
        var orderCreatedEvt = TestDataUtil.createOrderCreatedEvent(1, 1, 2, 3);
        expectEvent(orderCreatedEvt, InventoryEvent.InventoryDeducted.class, e -> {
            Assertions.assertNotNull(e.inventoryId());
            Assertions.assertEquals(orderCreatedEvt.orderId(), e.orderId());
            Assertions.assertEquals(3, e.quantity());
        });

        //check product remaining quantity test
        this.prodrepo.findById(1)
           .as(StepVerifier::create)
           .consumeNextWith(
            prod -> Assertions.assertEquals(7, prod.getAvailableQuantity())
           )
           .verifyComplete();

        //check duplicate event test
        //pass same order created event again
        expectNoEvent(orderCreatedEvt);

        //test order cancelled event and restore inventory test
        var ordCancEvt = TestDataUtil.createOrderCancelledEvent(orderCreatedEvt.orderId());
        expectEvent(ordCancEvt, InventoryEvent.InventoryRestored.class, e -> {
            Assertions.assertNotNull(e.inventoryId());
            Assertions.assertEquals(ordCancEvt.orderId(), e.orderId());
            Assertions.assertEquals(3, e.quantity());
        });
        
        //check product remaining quantity test after restore
        this.prodrepo.findById(1)
           .as(StepVerifier::create)
           .consumeNextWith(
            prod -> Assertions.assertEquals(10, prod.getAvailableQuantity())
           )
           .verifyComplete();
        
    }

    private void printProductInfo(Integer productId){
        this.prodrepo.findById(productId)
           .as(StepVerifier::create)
           .consumeNextWith(
            prod -> System.out.println("Product ID: " + prod.getId() + " Description: " + prod.getDescription() + " Available Quantity: " + prod.getAvailableQuantity())
           )
           .verifyComplete();
    }

    private <T> void expectEvent(OrderEvent evt, Class<T> type, Consumer<T> assertion){
        respFlux //start listening for response (InventoryEvent)
           .doFirst(() -> reqSink.tryEmitNext(evt)) //use sink to emit the order event (OrderCreated/OrderCancelled)
           .next() //wait for Mono<InventoryEvent> to be emitted
           .timeout(Duration.ofSeconds(2), Mono.empty()) //timeout if no event is emitted in 2 second
           .cast(type) //cast InventoryEvent to the expected type (InventoryDeducted/InventoryRestored/InventoryFailed)
           .as(StepVerifier::create)
           .consumeNextWith(assertion)
           .verifyComplete();
      }
  
    private void expectNoEvent(OrderEvent evt){
        respFlux //start listening for response (InventoryEvent)
           .doFirst(() -> reqSink.tryEmitNext(evt)) //use sink to emit the order event (OrderCreated/OrderCancelled)
           .next() //wait for Mono<InventoryEvent> to be emitted
           .timeout(Duration.ofSeconds(2), Mono.empty()) //timeout if no event is emitted in 2 second
           .as(StepVerifier::create)
           .verifyComplete();
    }

    @Test
    public void  restoreWithoutReserveTest(){
        //OrderCancelled event with non-existing orderId
        var ordCancEvt = TestDataUtil.createOrderCancelledEvent(UUID.randomUUID());
        expectNoEvent(ordCancEvt);
    }

    @Test
    public void outOfStockTest(){
        //OrderCreated Evt with quantity greater than available quantity
        var orderCreatedEvt = TestDataUtil.createOrderCreatedEvent(1, 1, 10, 11);
    
        expectEvent(orderCreatedEvt, InventoryEvent.InventoryFailed.class, e -> {
            Assertions.assertEquals(orderCreatedEvt.orderId(), e.orderId());
            Assertions.assertEquals(11, e.quantity());
            Assertions.assertEquals("Out of stock", e.message());
        });

        //check product remaining quantity test
        this.printProductInfo(1);
    }


    @TestConfiguration
    static class TestConfig{

        @Bean
        public Supplier<Flux<OrderEvent>> orderEvtProducer(){
            return reqSink::asFlux; //converts messages from reqSink to a kafka message stream (flux)
        }
        @Bean
        public Consumer<Flux<InventoryEvent>> inventoryEvtConsumer(){

            //Consumes messages from inventory-events topic
            return f -> f
            //For each InventoryEvent, emits it into respSink
            .doOnNext(respSink::tryEmitNext)
            .subscribe(); //activates the reactive stream
        }

    }
}
