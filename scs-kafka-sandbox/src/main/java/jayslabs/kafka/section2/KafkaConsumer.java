package jayslabs.kafka.section2;

import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Configuration
public class KafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);
    
    @Bean
    public Consumer<Flux<String>> consumer() {
        return flux -> flux
            .doOnNext(str -> log.info("Consumer Received message: {}", str))
            .subscribe();
    }

    @Bean
    public Function<Flux<String>, Mono<Void>> function() {
        return flux -> flux
            .doOnNext(str -> log.info("Function Received message: {}", str))
            .then();
    }

}
