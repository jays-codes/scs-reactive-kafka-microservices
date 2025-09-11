package jayslabs.kafka.section8.processor;

import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import jayslabs.kafka.common.MessageConverter;
import jayslabs.kafka.section8.config.DeliveryChannelProperties;
import jayslabs.kafka.section8.dto.DigitalDelivery;
import jayslabs.kafka.section8.dto.OrderEvent;
import jayslabs.kafka.section8.dto.PhysicalDelivery;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
/*
digital order goes to digital-delivery-topic
physical order goes to physical-delivery-topic AND digital-delivery-topic
*/
@Configuration
public class FanOutProcessor {

    private static final Logger log = LoggerFactory.getLogger(FanOutProcessor.class);

    private static final String DIGITAL_DELIVERY_CHANNEL = "digital-delivery-topic";
    private static final String PHYSICAL_DELIVERY_CHANNEL = "physical-delivery-topic";

    private final Consumer<OrderEvent> digitalSend = this::toDigitalDelivery;
    private final Consumer<OrderEvent> physicalSend = this::toPhysicalDelivery;
    private final Consumer<OrderEvent> fanOut = this.digitalSend.andThen(this.physicalSend);

    @Autowired
    private StreamBridge streamBridge;
    
    @Bean
    public Function<Flux<Message<OrderEvent>>, Mono<Void>> processor(){
        return flux -> flux
            .map(MessageConverter::toRecord)
            .doOnNext(r -> this.route(r.message()))
            .doOnNext(r -> r.acknowledgement().acknowledge())
            .then();
    }

    private void route(OrderEvent event){
        switch(event.orderType()){
            case DIGITAL -> this.digitalSend.accept(event);
            case PHYSICAL -> this.fanOut.accept(event);
        };
    }

    private void toDigitalDelivery(OrderEvent event){
        var dd = new DigitalDelivery(event.productId(), "%s@gmail.com".formatted(event.customerId()));
        this.streamBridge.send(DeliveryChannelProperties.getDigital(), dd);
    }

    private void toPhysicalDelivery(OrderEvent event){
        var pd = new PhysicalDelivery(
            event.productId(), 
            "%s St".formatted(event.customerId()), 
            "%s city".formatted(event.customerId()), "Canada");
        this.streamBridge.send(DeliveryChannelProperties.getPhysical(), pd);
    }
}
