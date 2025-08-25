package jayslabs.kafka.section3;

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

//@ContextConfiguration(classes = KafkaProcessorTestConfiguration.class)
@TestPropertySource(properties = {
    "sec=section3",
    "spring.cloud.function.definition=testProducer;testConsumer;processor",
    "spring.cloud.stream.bindings.testProducer-out-0.destination=input-topic",
    "spring.cloud.stream.bindings.testConsumer-in-0.destination=output-topic"

})
public class KafkaProcessorTest extends AbstractIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(KafkaProcessorTest.class);
    
    private static final Sinks.Many<String> reqSink = Sinks.many().unicast().onBackpressureBuffer();
    private static final Sinks.Many<String> resSink = Sinks.many().unicast().onBackpressureBuffer();

    @Test
    public void testKafkaProcessor() {
        
        //produce some data using reqSink
        reqSink.tryEmitNext("hworld1");
        reqSink.tryEmitNext("hworld2");


        //consume the data using resSink
        resSink.asFlux().take(2)
        .timeout(Duration.ofSeconds(5))
        .doOnNext(msg -> log.info("Test received: {}", msg))
        .as(StepVerifier::create)
        .consumeNextWith(m -> Assertions.assertEquals("HWORLD1", m))
        .consumeNextWith(m -> Assertions.assertEquals("HWORLD2", m))
        .verifyComplete();
    }

    @TestConfiguration
    static class KafkaProcessorTestConfiguration {
        @Bean
        public Supplier<Flux<String>> testProducer() {
            return reqSink::asFlux;
        }

        @Bean
        public Consumer<Flux<String>> testConsumer() {
            return flux -> flux
                .doOnNext(resSink::tryEmitNext).subscribe();  // Write to resSink, not reqSink!
        }
    }
}
