package jayslabs.kafka.section2;

import java.time.Duration;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.publisher.Flux;

@Configuration
public class KafkaProducer {
    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);

    @Bean
    public Supplier<Flux<String>> producer() {
        return () -> Flux.interval(Duration.ofSeconds(1))
            .take(15)
            .map(i -> "message-" + i)
            .doOnNext(i -> log.info("Produced message: {}", i));
    }
}
