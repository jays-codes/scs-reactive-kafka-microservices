package jayslabs.kafka.section8.consumer;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import jayslabs.kafka.common.CustomRecord;
import jayslabs.kafka.common.MessageConverter;
import jayslabs.kafka.section8.dto.DigitalDelivery;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Configuration
public class DigitalDeliveryConsumer {

    private static final Logger log = LoggerFactory.getLogger(DigitalDeliveryConsumer.class);

    @Bean
    public Function<Flux<Message<DigitalDelivery>>, Mono<Void>> digitalDlvryCnsmr(){
        return flux -> flux
            .map(MessageConverter::toRecord)
            .doOnNext(this::printMsgDetails)
            .then();

    }

    private void printMsgDetails(CustomRecord<DigitalDelivery> rec){
        log.info("Digital Consumer: {}", rec.message());
        //log.info("key: {}", rec.key());
        rec.acknowledgement().acknowledge();
    }
}
