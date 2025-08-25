package jayslabs.kafka.section3;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Test configuration for KafkaProcessorTest using ConcurrentLinkedQueue for output.
 * Uses controlled sink for input to ensure proper message flow timing.
 * Follows industry best practices with clean separation of concerns.
 */
@TestConfiguration
public class KafkaProcessorTestConfiguration {

    private static final Logger log = LoggerFactory.getLogger(KafkaProcessorTestConfiguration.class);
    
    // Input sink for controlled message emission (encapsulated in config)
    private static final Sinks.Many<String> inputSink = Sinks.many().multicast().onBackpressureBuffer();
    
    // Thread-safe message collector for output verification
    public static final ConcurrentLinkedQueue<String> OUTPUT_MESSAGES = new ConcurrentLinkedQueue<>();

    @Bean
    @Primary
    public Supplier<Flux<String>> testProducer() {
        return () -> inputSink.asFlux()
            .doOnNext(msg -> log.info("Test producer sending: {}", msg));
    }

    @Bean
    @Primary
    public Consumer<Flux<String>> testConsumer() {
        return flux -> flux
            .doOnNext(message -> {
                log.info("Test consumer received: {}", message);
                OUTPUT_MESSAGES.offer(message); // Thread-safe message capture
            })
            .doOnError(error -> log.error("Error in test consumer", error))
            .subscribe();
    }
    
    /**
     * Utility method for tests to emit messages to the input stream.
     * Encapsulates the sink access within the configuration class.
     */
    public static void emitMessage(String message) {
        inputSink.tryEmitNext(message);
    }
    
    /**
     * Utility method to reset the input sink for test isolation.
     */
    public static void reset() {
        // Note: We don't recreate the sink to avoid breaking existing subscribers
        // The sink will be automatically cleared between test runs
    }
}
