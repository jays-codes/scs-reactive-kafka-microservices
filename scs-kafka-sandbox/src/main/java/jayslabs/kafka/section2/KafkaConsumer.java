package jayslabs.kafka.section2;

import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Configuration
public class KafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);
    

    // @Bean
    // public ReceiverOptionsCustomizer customizer(){
    //     return (opts, dest) -> opts.consumerProperty("group.instance.id", "234");
    // }

    @Bean
    public Consumer<Flux<Message<String>>> consumer() {
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
