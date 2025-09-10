package jayslabs.kafka.section7;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import jayslabs.kafka.common.CustomRecord;
import jayslabs.kafka.common.MessageConverter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Configuration
public class CharFinder {

    private static final Logger log = LoggerFactory.getLogger(CharFinder.class);

    @Autowired
    private StreamBridge streamBridge;
    
    private static final String DLT_CHARFINDER_TOPIC = "dlt-charfinder-topic";

    @Bean 
    public Function<Flux<Message<String>>, Flux<Character>> charProcessor(){
        return flux -> flux
            .map(MessageConverter::toRecord)
            .concatMap(r -> this.find(r.message())
                            .onErrorResume(ex -> Mono.fromRunnable(() -> this.handleError(ex, r)))
                            .doAfterTerminate(() -> r.acknowledgement().acknowledge())
        );
    }

    private Mono<Character> find(String message){
        return Mono.just(message).map(m -> m.charAt(4));

    }

    private void handleError(Throwable ex, CustomRecord<String> record){
        log.error(ex.getMessage());
        this.streamBridge.send(DLT_CHARFINDER_TOPIC, 
            MessageBuilder.withPayload(record.message())
            .setHeader(KafkaHeaders.KEY, record.key())
            .build()
        );
    }
}
