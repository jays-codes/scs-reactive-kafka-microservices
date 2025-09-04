package jayslabs.kafka.section5.processor;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import jayslabs.kafka.common.MessageConverter;
import jayslabs.kafka.section5.config.DeliveryChannelProperties;
import jayslabs.kafka.section5.dto.DigitalDelivery;
import jayslabs.kafka.section5.dto.OrderEvent;
import jayslabs.kafka.section5.dto.PhysicalDelivery;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Configuration
public class OrderRouter {

    private static final Logger log = LoggerFactory.getLogger(OrderRouter.class);

    @Autowired
    private StreamBridge streamBridge;
    
    // No need to inject properties anymore - using static constants

    @Bean
    public Function<Flux<Message<OrderEvent>>, Mono<Void>> orderProcessor(){
        return flux -> flux
            .map(MessageConverter::toRecord)
            .doOnNext(r -> this.route(r.message()))
            .doOnNext(r -> r.acknowledgement().acknowledge())
            .then();
    }

    private void route(OrderEvent event){
        // Simple, direct routing using configuration properties
        switch(event.orderType()){
            case DIGITAL -> {
                var delivery = new DigitalDelivery(event.productId(), "%s@gmail.com".formatted(event.customerId()));
                this.sendToChannel(delivery, DeliveryChannelProperties.getDigital());
            }
            case PHYSICAL -> {
                var delivery = new PhysicalDelivery(
                    event.productId(), 
                    "%s St".formatted(event.customerId()), 
                    "%s city".formatted(event.customerId()), 
                    "Canada");
                this.sendToChannel(delivery, DeliveryChannelProperties.getPhysical());
            }
        }
    }

    /**
     * Generic method to send any delivery object via the explicit binding defined in yaml.
     * Provides consistent logging and error handling.
     */
    private void sendToChannel(Object delivery, String topic){
        try {
            this.streamBridge.send(topic, delivery);
            log.info("Successfully sent {} delivery to channel: {}", 
                    delivery.getClass().getSimpleName(), topic);
        } catch (Exception e) {
            log.error("Failed to send {} delivery to channel: {}", 
                     delivery.getClass().getSimpleName(), topic, e);
            throw e;
        }
    }


}
