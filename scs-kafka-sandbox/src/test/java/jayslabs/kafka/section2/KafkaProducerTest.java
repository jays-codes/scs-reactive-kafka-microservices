package jayslabs.kafka.section2;

import java.time.Duration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import jayslabs.kafka.AbstractIntegrationTest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Test class for KafkaProducer using separate TestConfiguration.
 * Uses ConcurrentLinkedQueue for clean, thread-safe message capture.
 */
@ContextConfiguration(classes = KafkaProducerTestConfiguration.class)
@TestPropertySource(properties = {
    "sec=section2",
    "spring.cloud.function.definition=producer;testConsumer",
    "spring.cloud.stream.bindings.testConsumer-in-0.destination=input-topic",
    "logging.level.root=ERROR",
    "logging.level.jayslabs.kafka*=INFO"
})
public class KafkaProducerTest extends AbstractIntegrationTest {

    @BeforeEach
    void setUp() {
        // Clear messages for test isolation
        KafkaProducerTestConfiguration.CAPTURED_MESSAGES.clear();
    }

    @Test
    public void testKafkaProducer() {
        // Wait for producer to send messages, then verify what we captured
        StepVerifier.create(
            Mono.delay(Duration.ofSeconds(3)) // Allow time for message flow
                .then(Mono.fromCallable(() -> KafkaProducerTestConfiguration.CAPTURED_MESSAGES.size()))
        )
        .expectNextMatches(messageCount -> {
            // Verify we received at least 2 messages
            Assertions.assertTrue(messageCount >= 2, 
                "Expected at least 2 messages, but got: " + messageCount);
            
            // Verify specific message content
            String[] messages = KafkaProducerTestConfiguration.CAPTURED_MESSAGES.toArray(new String[0]);
            Assertions.assertEquals("message-0", messages[0]);
            Assertions.assertEquals("message-1", messages[1]);
            
            return true;
        })
        .verifyComplete();
    }

    // @Test
    // public void testKafkaProducerMultipleMessages() {
    //     // Test to verify we can capture more messages
    //     StepVerifier.create(
    //         Mono.delay(Duration.ofSeconds(5)) // Wait longer for more messages
    //             .then(Mono.fromCallable(() -> KafkaProducerTestConfiguration.CAPTURED_MESSAGES.size()))
    //     )
    //     .expectNextMatches(messageCount -> {
    //         // Verify we received at least 4 messages
    //         Assertions.assertTrue(messageCount >= 4, 
    //             "Expected at least 4 messages, but got: " + messageCount);
            
    //         // Verify message format
    //         String[] messages = KafkaProducerTestConfiguration.CAPTURED_MESSAGES.toArray(new String[0]);
    //         for (int i = 0; i < Math.min(4, messages.length); i++) {
    //             Assertions.assertEquals("message-" + i, messages[i]);
    //         }
            
    //         return true;
    //     })
    //     .verifyComplete();
    // }
}
