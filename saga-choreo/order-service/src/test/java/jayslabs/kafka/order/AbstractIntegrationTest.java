package jayslabs.kafka.order;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Flux;
import jayslabs.kafka.common.events.order.OrderEvent;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.time.Duration;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import jayslabs.kafka.order.common.dto.OrderCreateRequest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Assertions;
import org.springframework.cloud.stream.function.StreamBridge;
import jayslabs.kafka.common.events.payment.PaymentEvent;
import jayslabs.kafka.common.events.inventory.InventoryEvent;
import jayslabs.kafka.common.events.shipping.ShippingEvent;

@DirtiesContext
@AutoConfigureWebTestClient
@SpringBootTest(properties={
    "logging.level.root=ERROR",
    "logging.level.jayslabs.kafka*=INFO",
    "spring.cloud.stream.kafka.binder.configuration.auto.offset.reset=earliest",
    "spring.cloud.function.definition=orderEventProducer;inventoryProcessor;paymentProcessor;shippingProcessor;orderEvtConsumer",
    "spring.cloud.stream.bindings.orderEvtConsumer-in-0.destination=order-events"
})
@EmbeddedKafka(
    partitions = 1, 
    bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@Import(AbstractIntegrationTest.TestConfig.class)
public abstract class AbstractIntegrationTest {

    private static final Sinks.Many<OrderEvent> respSink = Sinks.many().unicast().onBackpressureBuffer();
    private static final Flux<OrderEvent> respFlux = respSink.asFlux().cache(0); //cache the response flux

    @Autowired
    private WebTestClient client;

    @Autowired
    private StreamBridge strmbrdge;

    protected void emitEvent(PaymentEvent evt){
        strmbrdge.send("payment-events", evt);
    }

    protected void emitEvent(InventoryEvent evt){
        strmbrdge.send("inventory-events", evt);
    }

    protected void emitEvent(ShippingEvent evt){
        strmbrdge.send("shipping-events", evt);
    }

    protected UUID initiateOrder(OrderCreateRequest req){

        var ordIdRef = new AtomicReference<UUID>();
        var response = client.post()
        .uri("/order")
        .bodyValue(req)
        .exchange()
        .expectStatus().isAccepted()
        .expectBody()
        .jsonPath("$.orderId").exists()
        .jsonPath("$.orderId").value(id -> ordIdRef.set(UUID.fromString(id.toString())))
        .jsonPath("$.status").isEqualTo("PENDING");
        return ordIdRef.get();
    }

    protected void verifyOrderCreatedEvent(UUID ordId, int totalAmount){
        expectEvent(OrderEvent.OrderCreated.class, e -> {
            Assertions.assertEquals(totalAmount, e.totalAmount());
            Assertions.assertEquals(ordId, e.orderId());
        });
    }

    protected void verifyOrderCancelledEvent(UUID ordId){
        expectEvent(OrderEvent.OrderCancelled.class, e -> {
            Assertions.assertEquals(ordId, e.orderId());
        });
    }

    protected void verifyOrderCompletedEvent(UUID ordId){
        expectEvent(OrderEvent.OrderCompleted.class, e -> {
            Assertions.assertEquals(ordId, e.orderId());
        });
    }

    protected <T> void expectEvent(Class<T> type, Consumer<T> assertion){
        respFlux //start listening for response (InventoryEvent)
           .next() //wait for Mono<InventoryEvent> to be emitted
           .timeout(Duration.ofSeconds(2), Mono.empty()) //timeout if no event is emitted in 2 second
           .cast(type) //cast InventoryEvent to the expected type (InventoryDeducted/InventoryRestored/InventoryFailed)
           .as(StepVerifier::create)
           .consumeNextWith(assertion)
           .verifyComplete();
    }
  
    protected void expectNoEvent(){
        respFlux //start listening for response (InventoryEvent)
           .next() //wait for Mono<InventoryEvent> to be emitted
           .timeout(Duration.ofSeconds(2), Mono.empty()) //timeout if no event is emitted in 2 second
           .as(StepVerifier::create)
           .verifyComplete();
    }

    @TestConfiguration
    static class TestConfig{

        @Bean
        public Consumer<Flux<OrderEvent>> orderEvtConsumer(){

            //Consumes messages from inventory-events topic
            return f -> f
            //For each InventoryEvent, emits it into respSink
            .doOnNext(respSink::tryEmitNext)
            .subscribe(); //activates the reactive stream
        }
    }

}
