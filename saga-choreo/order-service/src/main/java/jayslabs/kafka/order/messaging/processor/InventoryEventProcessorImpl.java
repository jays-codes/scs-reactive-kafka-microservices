package jayslabs.kafka.order.messaging.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jayslabs.kafka.common.events.inventory.InventoryEvent;
import jayslabs.kafka.common.events.order.OrderEvent;
import jayslabs.kafka.common.processor.InventoryEventProcessor;
import jayslabs.kafka.order.common.service.OrderFulfillmentService;
import jayslabs.kafka.order.common.service.inventory.InventoryComponentStatusListener;
import jayslabs.kafka.order.messaging.mapper.InventoryEventMapper;
import jayslabs.kafka.order.messaging.mapper.OrderEventMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class InventoryEventProcessorImpl implements InventoryEventProcessor<OrderEvent> {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventProcessorImpl.class);

    private final OrderFulfillmentService fulfillmentService;
    private final InventoryComponentStatusListener statusListener;

    @Override
    public Mono<OrderEvent> handle(InventoryEvent.InventoryDeducted event) {
        var dto = InventoryEventMapper.toOrderInventoryDTO(event);
        return this.statusListener.onSuccess(dto)
        .then(this.fulfillmentService.completeOrder(event.orderId()))
        //.doOnNext(evt -> log.info("Inventory deducted successfully for orderId: {}", evt.orderId()))
        .map(OrderEventMapper::toOrderCompletedEvent);
    }
    
    @Override
    public Mono<OrderEvent> handle(InventoryEvent.InventoryFailed event) {
        var dto = InventoryEventMapper.toOrderInventoryDTO(event);
        return this.statusListener.onFailure(dto)
        .then(this.fulfillmentService.cancelOrder(event.orderId()))
        .map(OrderEventMapper::toOrderCancelledEvent);
    }
    
    @Override
    public Mono<OrderEvent> handle(InventoryEvent.InventoryRestored event) {
        var dto = InventoryEventMapper.toOrderInventoryDTO(event);
        return this.statusListener.onRollback(dto)
        .then(this.fulfillmentService.completeOrder(event.orderId()))
        .map(OrderEventMapper::toOrderCompletedEvent);
    }
}
