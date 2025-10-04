package jayslabs.kafka.section11;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jayslabs.kafka.section11.dto.ContactMethod;
import reactor.core.publisher.Flux;

/**
Native encoding/decoding of messages

 */

@Configuration
public class KafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

    @Bean
    public Consumer<Flux<ContactMethod>> consumer() {
        return flux -> flux
            .doOnNext(str -> log.info("Consumer Received message: {}", str))
            .subscribe();
    }
}
