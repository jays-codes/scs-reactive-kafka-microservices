package jayslabs.kafka.section3;

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
 * Integration test for KafkaProcessor using industry best practices.
 * Tests the complete producer → processor → consumer pipeline.
 * Uses separate TestConfiguration with ConcurrentLinkedQueue for clean separation of concerns.
 */
@ContextConfiguration(classes = KafkaProcessorTestConfiguration.class)
@TestPropertySource(properties = {
    "sec=section3",
    "spring.cloud.function.definition=testProducer;testConsumer;processor",
    "spring.cloud.stream.bindings.testProducer-out-0.destination=input-topic",
    "spring.cloud.stream.bindings.testConsumer-in-0.destination=output-topic"
})
public class KafkaProcessorTest extends AbstractIntegrationTest {

    @BeforeEach
    void setUp() {
        // Clear message queue for test isolation
        KafkaProcessorTestConfiguration.OUTPUT_MESSAGES.clear();
        KafkaProcessorTestConfiguration.reset();
    }

    @Test
    public void testKafkaProcessor() {
        // Arrange: Emit test messages to input stream
        KafkaProcessorTestConfiguration.emitMessage("hworld1");
        KafkaProcessorTestConfiguration.emitMessage("hworld2");

        // Act & Assert: Wait for processor to transform messages and verify output
        StepVerifier.create(
            Mono.delay(Duration.ofSeconds(3)) // Allow time for message flow through pipeline
                .then(Mono.fromCallable(() -> KafkaProcessorTestConfiguration.OUTPUT_MESSAGES.size()))
        )
        .expectNextMatches(messageCount -> {
            // Verify we received the expected number of messages
            Assertions.assertTrue(messageCount >= 2,
                "Expected at least 2 processed messages, but got: " + messageCount + 
                ". Messages in queue: " + KafkaProcessorTestConfiguration.OUTPUT_MESSAGES);

            // Verify specific message transformation (lowercase → UPPERCASE)
            String[] messages = KafkaProcessorTestConfiguration.OUTPUT_MESSAGES.toArray(new String[0]);
            Assertions.assertEquals("HWORLD1", messages[0], "First message should be transformed to uppercase");
            Assertions.assertEquals("HWORLD2", messages[1], "Second message should be transformed to uppercase");

            return true;
        })
        .verifyComplete();
    }

    @Test
    public void testKafkaProcessorWithSingleMessage() {
        // Arrange: Test with single message
        KafkaProcessorTestConfiguration.emitMessage("test");

        // Act & Assert: Verify single message processing
        StepVerifier.create(
            Mono.delay(Duration.ofSeconds(2))
                .then(Mono.fromCallable(() -> KafkaProcessorTestConfiguration.OUTPUT_MESSAGES.size()))
        )
        .expectNextMatches(messageCount -> {
            Assertions.assertTrue(messageCount >= 1,
                "Expected at least 1 processed message, but got: " + messageCount +
                ". Messages in queue: " + KafkaProcessorTestConfiguration.OUTPUT_MESSAGES);

            String[] messages = KafkaProcessorTestConfiguration.OUTPUT_MESSAGES.toArray(new String[0]);
            Assertions.assertEquals("TEST", messages[0], "Message should be transformed to uppercase");

            return true;
        })
        .verifyComplete();
    }
}
