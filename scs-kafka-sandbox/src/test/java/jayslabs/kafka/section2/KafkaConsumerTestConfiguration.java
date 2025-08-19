package jayslabs.kafka.section2;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import reactor.core.publisher.Flux;

/**
 * Test configuration for KafkaConsumerTest.
 * Provides testProducer bean for testing consumer functionality.
 */
@TestConfiguration
public class KafkaConsumerTestConfiguration {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerTestConfiguration.class);

    @Bean
    @Primary
    public Supplier<Flux<String>> testProducer(){
        return () -> Flux.just("Hello World")
            .doOnNext(msg -> log.info("Test producer sending: {}", msg));
    }
}
