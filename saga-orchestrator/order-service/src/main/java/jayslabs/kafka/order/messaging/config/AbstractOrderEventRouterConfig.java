package jayslabs.kafka.order.messaging.config;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import jayslabs.kafka.common.events.DomainEvent;
import jayslabs.kafka.common.events.order.OrderEvent;
import jayslabs.kafka.common.processor.EventProcessor;
import jayslabs.kafka.common.util.MessageConverter;
import reactor.core.publisher.Flux;

/*
 Provides reusable message wrapping logic
*/
public abstract class AbstractOrderEventRouterConfig {

    private static final Logger log = LoggerFactory.getLogger(AbstractOrderEventRouterConfig.class);
    
    private static final String DESTINATION_HEADER = "spring.cloud.stream.sendto.destination";
    private static final String ORDER_EVENTS_CHANNEL = "order-events-channel";

    protected <T extends DomainEvent> Function<Flux<Message<T>>, Flux<Message<OrderEvent>>> processor(EventProcessor<T, OrderEvent> evtProcessor){
        return flux -> flux.map(MessageConverter::toRecord)
        .doOnNext(cr -> log.info("received in order-service: {}", cr.message()))
        .concatMap(cr -> evtProcessor.process(cr.message())
        .doOnSuccess(evt -> cr.acknowledgement().acknowledge())
        ).map(this::toMessage);
    }

    protected Message<OrderEvent> toMessage(OrderEvent evt){
        log.info("order-service produced: {}", evt);
        return MessageBuilder.withPayload(evt)
        .setHeader(KafkaHeaders.KEY, evt.orderId().toString())
        //Dynamic Routing: Uses spring.cloud.stream.sendto.destination header 
        //to route all output to order-events-channel
        .setHeader(DESTINATION_HEADER, ORDER_EVENTS_CHANNEL)
        .build();
    }
}

/*
Purpose: Template Method Pattern - provides reusable reactive pipeline for 
all event processors.

Key Features:
Generic Processing: <T extends DomainEvent> allows processing any event type 
(PaymentEvent, InventoryEvent, ShippingEvent)

Standard Pipeline:
- MessageConverter::toRecord → Extract payload, key, acknowledgment
- concatMap → Sequential processing (preserves order per partition)
- doOnSuccess(acknowledge()) → Manual Kafka offset commit
- toMessage() → Add routing headers

Dynamic Routing: Uses spring.cloud.stream.sendto.destination header to route 
all output to order-events-channel

Why Abstract Class?
- Provides shared implementation (processor(), toMessage())
- Subclass (ProcessorConfig) provides concrete beans
*/