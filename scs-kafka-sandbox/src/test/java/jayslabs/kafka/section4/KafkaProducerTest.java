package jayslabs.kafka.section4;

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
 * Tests MessageConverter DTO/Mapper pattern with key-value verification.
 * Uses ConcurrentLinkedQueue for clean, thread-safe record capture.
 */
@ContextConfiguration(classes = KafkaProducerTestConfiguration.class)
@TestPropertySource(properties = {
    "sec=section4",
    "spring.cloud.function.definition=producer;testConsumer",
    "spring.cloud.stream.bindings.testConsumer-in-0.destination=input-topic"
})
public class KafkaProducerTest extends AbstractIntegrationTest {

    @BeforeEach
    void setUp() {
        // Clear records for test isolation
        KafkaProducerTestConfiguration.CAPTURED_RECORDS.clear();
    }

    @Test
    public void testKafkaProducerWithKeyValue() {
        // Wait for producer to send messages, then verify what we captured
        StepVerifier.create(
            Mono.delay(Duration.ofSeconds(3)) // Allow time for message flow
                .then(Mono.fromCallable(() -> KafkaProducerTestConfiguration.CAPTURED_RECORDS.size()))
        )
        .expectNextMatches(recordCount -> {
            // Verify we received at least 2 records
            Assertions.assertTrue(recordCount >= 2, 
                "Expected at least 2 records, but got: " + recordCount);
            
            // Verify specific record content (payload and key)
            var recordsList = KafkaProducerTestConfiguration.CAPTURED_RECORDS.stream().toList();
            
            // Verify first record
            Assertions.assertEquals("message-0", recordsList.get(0).message(), "First record payload should match");
            Assertions.assertEquals("key-0", recordsList.get(0).key(), "First record key should match");
            
            // Verify second record
            Assertions.assertEquals("message-1", recordsList.get(1).message(), "Second record payload should match");
            Assertions.assertEquals("key-1", recordsList.get(1).key(), "Second record key should match");
            
            return true;
        })
        .verifyComplete();
    }

    // @Test
    // public void testKafkaProducerSingleRecord() {
    //     // Test single record verification (similar to KafkaProducerWithKeyTest approach)
    //     StepVerifier.create(
    //         Mono.delay(Duration.ofSeconds(2))
    //             .then(Mono.fromCallable(() -> KafkaProducerTestConfiguration.CAPTURED_RECORDS.size()))
    //     )
    //     .expectNextMatches(recordCount -> {
    //         // Verify we received at least 1 record
    //         Assertions.assertTrue(recordCount >= 1,
    //             "Expected at least 1 record, but got: " + recordCount +
    //             ". Records in queue: " + KafkaProducerTestConfiguration.CAPTURED_RECORDS);

    //         // Verify first record details
    //         var recordsList = KafkaProducerTestConfiguration.CAPTURED_RECORDS.stream().toList();
    //         CustomRecord<String> firstRecord = recordsList.get(0);
            
    //         Assertions.assertEquals("message-0", firstRecord.message(), "First record message should be 'message-0'");
    //         Assertions.assertEquals("key-0", firstRecord.key(), "First record key should be 'key-0'");
    //         Assertions.assertNotNull(firstRecord.acknowledgement(), "Acknowledgement should not be null");

    //         return true;
    //     })
    //     .verifyComplete();
    // }

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
