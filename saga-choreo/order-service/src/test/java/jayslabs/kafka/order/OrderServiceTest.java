package jayslabs.kafka.order;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.context.TestPropertySource;

import reactor.core.publisher.Sinks;
import reactor.core.publisher.Flux;
import jayslabs.kafka.common.events.order.OrderEvent;
import java.util.function.Consumer;
import java.time.Duration;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.util.concurrent.atomic.AtomicReference;
import java.util.UUID;



@AutoConfigureWebTestClient
@TestPropertySource(properties = {
    "spring.cloud.function.definition=orderEventProducer;inventoryProcessor;paymentProcessor;shippingProcessor;orderEvtConsumer",
    "spring.cloud.stream.bindings.orderEvtConsumer-in-0.destination=order-events",
})
public class OrderServiceTest extends AbstractIntegrationTest{

    @Autowired
    private WebTestClient client;

    private static final Sinks.Many<OrderEvent> respSink = Sinks.many().unicast().onBackpressureBuffer();
    private static final Flux<OrderEvent> respFlux = respSink.asFlux().cache(0); //cache the response flux

    @Test
    public void orderCompleteWorkflowTest(){

        var req = TestDataUtil.toOrderCreateRequest(1, 1, 2, 3);
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

        expectEvent(OrderEvent.OrderCreated.class, e -> {
            Assertions.assertEquals(6, e.totalAmount());
            Assertions.assertEquals(ordIdRef.get(), e.orderId());
        });
    }


    private <T> void expectEvent(Class<T> type, Consumer<T> assertion){
        respFlux //start listening for response (InventoryEvent)
           .next() //wait for Mono<InventoryEvent> to be emitted
           .timeout(Duration.ofSeconds(2), Mono.empty()) //timeout if no event is emitted in 2 second
           .cast(type) //cast InventoryEvent to the expected type (InventoryDeducted/InventoryRestored/InventoryFailed)
           .as(StepVerifier::create)
           .consumeNextWith(assertion)
           .verifyComplete();
    }
  
    private void expectNoEvent(){
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
