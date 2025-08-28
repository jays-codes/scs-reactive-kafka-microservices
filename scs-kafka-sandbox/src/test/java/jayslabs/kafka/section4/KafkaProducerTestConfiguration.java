package jayslabs.kafka.section4;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.Message;

import jayslabs.kafka.common.CustomRecord;
import jayslabs.kafka.common.MessageConverter;
import reactor.core.publisher.Flux;

/**
 * Test configuration for KafkaProducerTest using ConcurrentLinkedQueue.
 * Tests MessageConverter DTO/Mapper pattern with key-value verification.
 * Provides clean separation of concerns with thread-safe message capture.
 */
@TestConfiguration
public class KafkaProducerTestConfiguration {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerTestConfiguration.class);
    
    // Thread-safe message collector for CustomRecord<String> - accessible from test class
    public static final ConcurrentLinkedQueue<CustomRecord<String>> CAPTURED_RECORDS = new ConcurrentLinkedQueue<>();

    @Bean
    @Primary
    public Consumer<Flux<Message<String>>> testConsumer() {
        return flux -> flux
            .map(MessageConverter::toRecord)  // Transform Message<String> to CustomRecord<String>
            .doOnNext(record -> {
                log.info("Test consumer received record - payload: {}, key: {}", record.message(), record.key());
                CAPTURED_RECORDS.offer(record); // Thread-safe record capture
            })
            .doOnError(error -> log.error("Error in test consumer", error))
            .subscribe();
    }
}
