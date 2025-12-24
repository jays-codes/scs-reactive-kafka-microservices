package jayslabs.kafka.shipping.messaging.processor;

import java.util.function.UnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jayslabs.kafka.common.events.order.OrderEvent;
import jayslabs.kafka.common.events.shipping.ShippingEvent;
import jayslabs.kafka.common.exception.EventAlreadyProcessedException;
import jayslabs.kafka.common.processor.OrderEventProcessor;
import jayslabs.kafka.shipping.common.service.ShippingService;
import jayslabs.kafka.shipping.messaging.mapper.EventDTOMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;



@Service
@RequiredArgsConstructor
public class OrderEventProcessorImpl implements OrderEventProcessor<ShippingEvent>{

    private final static Logger log = LoggerFactory.getLogger(OrderEventProcessorImpl.class);

    private final ShippingService service;

    @Override
    public Mono<ShippingEvent> handle(OrderEvent.OrderCreated event) {
        return this.service.createShipmentRecord(EventDTOMapper.toCreateShippingRequest(event))
        .doOnNext(v -> log.info("Shipment record created for orderId: {}", event.orderId()))
        .transform(exceptionHandler())
        .then(Mono.empty());
    }

    @Override
    public Mono<ShippingEvent> handle(OrderEvent.OrderCancelled event) {
        return this.service.cancelShipment(event.orderId())
        .doOnNext(v -> log.info("Shipment cancelled for orderId: {}", event.orderId()))
        .then(Mono.empty());
    }
    
    @Override
    public Mono<ShippingEvent> handle(OrderEvent.OrderCompleted event) {
        return this.service.scheduleShipment(event.orderId())
        .map(EventDTOMapper::toShippingScheduledEvent)
        .doOnNext(evt -> log.info("Shipment scheduled: {}", evt));
    }

    private <T> UnaryOperator<Mono<T>> exceptionHandler(){
        return mono -> mono.onErrorResume(EventAlreadyProcessedException.class, e-> Mono.empty())
        .doOnError(ex -> log.error(ex.getMessage()));
    }
}
