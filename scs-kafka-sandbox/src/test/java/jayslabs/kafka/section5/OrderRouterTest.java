package jayslabs.kafka.section5;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;

import jayslabs.kafka.AbstractIntegrationTest;
import jayslabs.kafka.section5.dto.DigitalDelivery;
import jayslabs.kafka.section5.dto.OrderEvent;
import jayslabs.kafka.section5.dto.OrderType;
import jayslabs.kafka.section5.dto.PhysicalDelivery;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

@TestPropertySource(properties = {
    "sec=section5",
    "spring.profiles.active=section5",  // ✅ Explicitly set active profile
    "spring.cloud.function.definition=orderProcessor;testProducer;testDDConsumer;testPDConsumer",
    "spring.cloud.stream.bindings.testProducer-out-0.destination=order-events-topic",
    "spring.cloud.stream.bindings.testDDConsumer-in-0.destination=digital-delivery-topic",
    "spring.cloud.stream.bindings.testPDConsumer-in-0.destination=physical-delivery-topic"
})
public class OrderRouterTest extends AbstractIntegrationTest{

    private static final Sinks.Many<OrderEvent> orderSink = Sinks.many().unicast().onBackpressureBuffer();
    private static final Sinks.Many<DigitalDelivery> ddSink = Sinks.many().unicast().onBackpressureBuffer();
    private static final Sinks.Many<PhysicalDelivery> pdSink = Sinks.many().unicast().onBackpressureBuffer();

    @Test
    public void routerTest(){
        //produce
        orderSink.tryEmitNext(new OrderEvent(1, 1, OrderType.DIGITAL));
        orderSink.tryEmitNext(new OrderEvent(2, 2, OrderType.PHYSICAL));
        orderSink.tryEmitNext(new OrderEvent(3, 3, OrderType.DIGITAL));
        
        //consume - wait for 2 digital deliveries
        ddSink.asFlux()
        .take(2)  // Take exactly 2 messages
        .timeout(Duration.ofSeconds(2))  // Timeout if not received in 5 seconds
        .as(StepVerifier::create)
        .consumeNextWith(d -> Assertions.assertEquals("1@gmail.com", d.email()))
        .consumeNextWith(d -> Assertions.assertEquals("3@gmail.com", d.email()))
        .verifyComplete();

        // Wait for 1 physical delivery
        pdSink.asFlux()
        .take(1)  // Take exactly 1 message
        .timeout(Duration.ofSeconds(2))  // Timeout if not received in 5 seconds
        .as(StepVerifier::create)
        .consumeNextWith(p -> Assertions.assertEquals("2 St", p.street()))
        .verifyComplete();
    }

    @TestConfiguration
    static class TestConfig{

        @Bean
        public Supplier<Flux<OrderEvent>> testProducer(){
            return orderSink::asFlux;
        }

        @Bean
        public Consumer<Flux<DigitalDelivery>> testDDConsumer(){
            return f -> f.doOnNext(ddSink::tryEmitNext).subscribe();
        }

        @Bean
        public Consumer<Flux<PhysicalDelivery>> testPDConsumer(){
            return f -> f.doOnNext(pdSink::tryEmitNext).subscribe();
        }

    }
}
