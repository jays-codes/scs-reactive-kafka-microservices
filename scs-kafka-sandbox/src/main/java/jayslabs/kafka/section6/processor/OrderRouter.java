package jayslabs.kafka.section6.processor;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import jayslabs.kafka.common.CustomRecord;
import jayslabs.kafka.common.MessageConverter;
import jayslabs.kafka.section6.config.DeliveryChannelProperties;
import jayslabs.kafka.section6.dto.DigitalDelivery;
import jayslabs.kafka.section6.dto.OrderEvent;
import jayslabs.kafka.section6.dto.PhysicalDelivery;
import reactor.core.publisher.Flux;


@Configuration
public class OrderRouter {

    private static final Logger log = LoggerFactory.getLogger(OrderRouter.class);

    private static final String DESTINATION_HEADER = "spring.cloud.stream.sendto.destination";    
    
    @Bean
    public Function<Flux<Message<OrderEvent>>, Flux<Message<?>>> orderProcessor(){
        return flux -> flux
            .map(MessageConverter::toRecord)
            .map(this::route);
    }

    private Message<?> route(CustomRecord<OrderEvent> record){
        var msg = 
        switch(record.message().orderType()){
            case DIGITAL -> this.toDigitalDelivery(record.message());
            case PHYSICAL -> this.toPhysicalDelivery(record.message());
        };
        record.acknowledgement().acknowledge();
        return msg;
    }

    private Message<DigitalDelivery> toDigitalDelivery(OrderEvent event){
        var dd = new DigitalDelivery(event.productId(), "%s@gmail.com".formatted(event.customerId()));
        return MessageBuilder.withPayload(dd)
            .setHeader(DESTINATION_HEADER, DeliveryChannelProperties.getDigital())
            .build();
    }

    private Message<PhysicalDelivery> toPhysicalDelivery(OrderEvent event){
        var pd = new PhysicalDelivery(
            event.productId(), 
            "%s St".formatted(event.customerId()), 
            "%s city".formatted(event.customerId()), "Canada");
        return MessageBuilder.withPayload(pd)
            .setHeader(DESTINATION_HEADER, DeliveryChannelProperties.getPhysical())
            .build();
    }
}
