package jayslabs.kafka.section3;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Configuration
public class KafkaProcessor {

    private static final Logger log = LoggerFactory.getLogger(KafkaProcessor.class);

    @Bean
    public Function<Flux<String>, Flux<String>> processor() {
        return flux -> flux
            .doOnNext(str -> log.info("Processor Received message: {}", str))
            .flatMap(this::process)
            .doOnNext(str -> log.info("After processing message: {}", str));
    }

    //service layer simulation
    private Mono<String> process(String input) {
        return Mono.just(input)
            .map(String::toUpperCase);
    }
}
