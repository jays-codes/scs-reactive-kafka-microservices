package jayslabs.kafka.section12;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.publisher.Flux;

/**
 Objective: to consume messages from multiple topics
 


 */

@Configuration
public class KafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);
    
    @Bean
    public Consumer<Flux<String>> consumer() {
        return flux -> flux
            .doOnNext(str -> log.info("Consumer Received message: {}", str))
            .subscribe();
    }


}
