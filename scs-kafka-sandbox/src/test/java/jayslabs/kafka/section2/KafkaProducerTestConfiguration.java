package jayslabs.kafka.section2;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import reactor.core.publisher.Flux;

/**
 * Test configuration for KafkaProducerTest using ConcurrentLinkedQueue.
 * Provides clean separation of concerns with thread-safe message capture.
 */
@TestConfiguration
public class KafkaProducerTestConfiguration {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerTestConfiguration.class);
    
    // Thread-safe message collector - accessible from test class
    public static final ConcurrentLinkedQueue<String> CAPTURED_MESSAGES = new ConcurrentLinkedQueue<>();

    @Bean
    @Primary
    public Consumer<Flux<String>> testConsumer() {
        return flux -> flux
            .doOnNext(message -> {
                log.info("Test consumer received: {}", message);
                CAPTURED_MESSAGES.offer(message); // Thread-safe message capture
            })
            .doOnError(error -> log.error("Error in test consumer", error))
            .subscribe();
    }
}
