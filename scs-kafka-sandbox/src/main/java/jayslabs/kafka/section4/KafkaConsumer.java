package jayslabs.kafka.section4;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import jayslabs.kafka.common.CustomRecord;
import jayslabs.kafka.common.MessageConverter;
import reactor.core.publisher.Flux;

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
            .map(MessageConverter::toRecord)
            .doOnNext(this::printMsgDetails)
            .subscribe();
    }

    // @Bean
    // public Function<Flux<Message<String>>, Mono<Void>> function() {
    //     return flux -> flux
    //         .map(MessageConverter::toRecord)
    //         .doOnNext(this::printMsgDetails)
    //         .then();
    // }

    private void printMsgDetails(CustomRecord<String> rec){
        log.info("payload: {}", rec.message());
        log.info("key: {}", rec.key());
        rec.acknowledgement().acknowledge();
    }

}
