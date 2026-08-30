package jayslabs.kafka.inventory.messaging.config;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import jayslabs.kafka.common.events.inventory.InventoryEvent;
import jayslabs.kafka.common.events.order.OrderEvent;
import jayslabs.kafka.common.processor.OrderEventProcessor;
import jayslabs.kafka.common.util.MessageConverter;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@Configuration
@RequiredArgsConstructor
public class OrderEventProcessorConfig {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProcessorConfig.class);
    private final OrderEventProcessor<InventoryEvent> evtProcessor;

    @Bean
    public Function<Flux<Message<OrderEvent>>, Flux<Message<InventoryEvent>>> processor(){
        return flux -> flux.map(MessageConverter::toRecord)
        .doOnNext(cr -> log.info("inventory processed: {}", cr.message()))
        .concatMap(cr -> this.evtProcessor.process(cr.message())
        .doOnSuccess(evt -> cr.acknowledgement().acknowledge())
        ).map(this::toMessage);
    }

    private Message<InventoryEvent> toMessage(InventoryEvent evt){
        return MessageBuilder.withPayload(evt)
        .setHeader(KafkaHeaders.KEY, evt.orderId().toString())
        .build();
    }
}
