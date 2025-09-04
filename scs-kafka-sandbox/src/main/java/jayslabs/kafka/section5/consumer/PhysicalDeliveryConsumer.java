package jayslabs.kafka.section5.consumer;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import jayslabs.kafka.common.CustomRecord;
import jayslabs.kafka.common.MessageConverter;
import jayslabs.kafka.section5.dto.PhysicalDelivery;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Configuration
public class PhysicalDeliveryConsumer {

    private static final Logger log = LoggerFactory.getLogger(PhysicalDeliveryConsumer.class);

    @Bean
    public Function<Flux<Message<PhysicalDelivery>>, Mono<Void>> physicalDlvryCnsmr(){
        return flux -> flux
            .map(MessageConverter::toRecord)
            .doOnNext(this::printMsgDetails)
            .then();

    }

    private void printMsgDetails(CustomRecord<PhysicalDelivery> rec){
        log.info("Physical Consumer: {}", rec.message());
        //log.info("key: {}", rec.key());
        rec.acknowledgement().acknowledge();
    }
}
