package jayslabs.kafka.section8.processor;

import java.util.function.Function;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import jayslabs.kafka.common.CustomRecord;
import jayslabs.kafka.common.MessageConverter;
import jayslabs.kafka.section8.config.DeliveryChannelProperties;
import jayslabs.kafka.section8.dto.DigitalDelivery;
import jayslabs.kafka.section8.dto.OrderEvent;
import jayslabs.kafka.section8.dto.PhysicalDelivery;
import reactor.core.publisher.Flux;
/*
digital order goes to digital-delivery-topic
physical order goes to physical-delivery-topic AND digital-delivery-topic
*/
@Configuration
public class FanOutProcessorMessageBuilder {


    private final Function<OrderEvent, Message<DigitalDelivery>> digitalSend = this::toDigitalDelivery;
    private final Function<OrderEvent, Message<PhysicalDelivery>> physicalSend = this::toPhysicalDelivery;

    private static final String DESTINATION_HEADER = "spring.cloud.stream.sendto.destination";

    @Bean
    public Function<Flux<Message<OrderEvent>>, Flux<Message<?>>> processorMsgBuilder(){
        return flux -> flux
            .map(MessageConverter::toRecord)
            .flatMap(this::routeToMultipleMessages);
    }

    private Flux<Message<?>> routeToMultipleMessages(CustomRecord<OrderEvent> record){
        var event = record.message();
        record.acknowledgement().acknowledge(); // Acknowledge once per input message
        
        return switch(event.orderType()){
            case DIGITAL -> Flux.just(this.digitalSend.apply(event));
            case PHYSICAL -> Flux.just(
                this.digitalSend.apply(event),
                this.physicalSend.apply(event)
            );
        };
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
