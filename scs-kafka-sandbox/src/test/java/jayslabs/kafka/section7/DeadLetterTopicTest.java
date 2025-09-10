package jayslabs.kafka.section7;

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
    "sec=section7",
    "spring.profiles.active=section7",  // ✅ Explicitly set active profile
    "spring.cloud.function.definition=charProcessor;testStringProducer;testCharConsumer;testDltConsumer",
    "spring.cloud.stream.bindings.testStringProducer-out-0.destination=string-topic",
    "spring.cloud.stream.bindings.charProcessor-in-0.destination=string-topic",
    "spring.cloud.stream.bindings.charProcessor-out-0.destination=char-topic",
    "spring.cloud.stream.bindings.testCharConsumer-in-0.destination=char-topic",
    "spring.cloud.stream.bindings.testDltConsumer-in-0.destination=dlt-charfinder-topic"
})
public class DeadLetterTopicTest extends AbstractIntegrationTest{

    private static final Logger log = LoggerFactory.getLogger(DeadLetterTopicTest.class);

    private static final Sinks.Many<String> reqSink = Sinks.many().unicast().onBackpressureBuffer();
    private static final Sinks.Many<Character> charSink = Sinks.many().unicast().onBackpressureBuffer();
    private static final Sinks.Many<String> dltSink = Sinks.many().unicast().onBackpressureBuffer();

    @Test
    public void charFinderTest(){
        //produce string
        reqSink.tryEmitNext("Hello, World!");
        reqSink.tryEmitNext("Anya");
        reqSink.tryEmitNext("Becky");
        reqSink.tryEmitNext("a7");
        reqSink.tryEmitNext("b8");

        //consume character
        charSink.asFlux()
        .take(Duration.ofSeconds(2))
        .doOnNext(r -> log.info("char consumer received {}", r))
        .as(StepVerifier::create)
        .consumeNextWith(r -> Assertions.assertEquals('o', r))
        .consumeNextWith(r -> Assertions.assertEquals('y', r))
        .verifyComplete();

        //consume dlt
        dltSink.asFlux()
        .doOnNext(r -> log.info("dlt consumer received {}", r))
        .as(StepVerifier::create)
        .consumeNextWith(r -> Assertions.assertEquals("Anya", r))
        .consumeNextWith(r -> Assertions.assertEquals("a7", r))
        .consumeNextWith(r -> Assertions.assertEquals("b8", r))
        .verifyComplete();
        
    }

    
    @TestConfiguration
    static class TestConfig{

        @Bean
        public Supplier<Flux<String>> testStringProducer(){
            return reqSink::asFlux
            ;
        }

        @Bean
        public Consumer<Flux<Character>> testCharConsumer(){
            return f -> f.doOnNext(charSink::tryEmitNext).subscribe();
        }

        @Bean
        public Consumer<Flux<String>> testDltConsumer(){
            return f -> f.doOnNext(dltSink::tryEmitNext).subscribe();
        }
    }
}
