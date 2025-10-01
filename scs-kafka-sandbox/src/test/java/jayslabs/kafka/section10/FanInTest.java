package jayslabs.kafka.section10;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;

import jayslabs.kafka.AbstractIntegrationTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

@TestPropertySource(properties = {
    "sec=section10",
    //"spring.profiles.active=section8",  // ✅ Explicitly set active profile
    "spring.cloud.function.definition=processor;tempProducer;humidProducer;heatIndexConsumer",
    "spring.cloud.stream.bindings.tempProducer-out-0.destination=temp-topic",
    "spring.cloud.stream.bindings.humidProducer-out-0.destination=humid-topic",
    "spring.cloud.stream.bindings.processor-in-0.destination=temp-topic",
    "spring.cloud.stream.bindings.processor-in-1.destination=humid-topic",
    "spring.cloud.stream.bindings.processor-out-0.destination=heat-index-topic",
    "spring.cloud.stream.bindings.heatIndexConsumer-in-0.destination=heat-index-topic"
})

public class FanInTest extends AbstractIntegrationTest{
    private static final Logger log = LoggerFactory.getLogger(FanInTest.class);

    private static final Sinks.Many<Integer> tempSink = Sinks.many().unicast().onBackpressureBuffer();
    private static final Sinks.Many<Integer> humidSink = Sinks.many().unicast().onBackpressureBuffer();
    private static final Sinks.Many<Long> hiSink = Sinks.many().unicast().onBackpressureBuffer();

    @Test
    public void heatIndexTest(){

        hiSink.asFlux()
        .take(3)
        .timeout(Duration.ofSeconds(5))
        .as(StepVerifier::create)
        .then(() -> tempSink.tryEmitNext(90))
        .then(() -> humidSink.tryEmitNext(55))
        .consumeNextWith(hi -> Assertions.assertEquals(97, hi))
        .then(() -> humidSink.tryEmitNext(60))
        .consumeNextWith(hi -> Assertions.assertEquals(100, hi))
        .then(() -> tempSink.tryEmitNext(94))
        .consumeNextWith(hi -> Assertions.assertEquals(110, hi))
        .verifyComplete();
        

    }

    @TestConfiguration
    static class TestConfig{

        @Bean
        public Supplier<Flux<Integer>> tempProducer(){
            return tempSink::asFlux;
        }

        @Bean
        public Supplier<Flux<Integer>> humidProducer(){
            return humidSink::asFlux;
        }

        @Bean
        public Consumer<Flux<Long>> heatIndexConsumer(){
            return f -> f.doOnNext(hiSink::tryEmitNext).subscribe();
        }

    }
}
