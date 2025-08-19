package jayslabs.kafka.section2;

import java.time.Duration;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;

import jayslabs.kafka.AbstractIntegrationTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/*
 * TODO: create test for KafkaConsumer.function()
 */

@TestPropertySource(properties = {
    "sec=section2",
    "spring.cloud.function.definition=consumer;testProducer",
    "spring.cloud.stream.bindings.testProducer-out-0.destination=input-topic",
    "logging.level.root=ERROR",
    "logging.level.jayslabs.kafka*=INFO"
})
@ExtendWith(OutputCaptureExtension.class)
public class KafkaConsumerTest extends AbstractIntegrationTest {

    /*
     * Create a test that uses a reactive stream to consume a message from a Kafka topic.
     * 
     * 1. Create a test that uses a reactive stream to consume a message from a Kafka topic.
     * 2. Use the @ExtendWith(OutputCaptureExtension.class) annotation to capture the output of the test.
     * 3. Use the @TestConfiguration annotation to create a test configuration.
     * 4. Use the @Bean annotation to create a test producer.
     */

    @Test
    public void testKafkaConsumer(CapturedOutput output){
        
        Mono.delay(Duration.ofSeconds(1))
        .then(Mono.fromSupplier(output::getOut))
        .as(StepVerifier::create)
        .consumeNextWith(s -> Assertions.assertTrue(s.contains("Consumer Received message: Hello World")))
        .verifyComplete();
    }

    @TestConfiguration
    static class TestConfig{

        @Bean
        public Supplier<Flux<String>> testProducer(){
            return () -> Flux.just("Hello World");
        }
    }
}
