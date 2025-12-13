package jayslabs.kafka.inventory.messaging.processor;

import java.util.function.UnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jayslabs.kafka.common.events.inventory.InventoryEvent;
import jayslabs.kafka.common.events.order.OrderEvent;
import jayslabs.kafka.common.exception.EventAlreadyProcessedException;
import jayslabs.kafka.common.processor.OrderEventProcessor;
import jayslabs.kafka.inventory.common.service.InventoryService;
import jayslabs.kafka.inventory.messaging.mapper.EventDTOMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OrderEventProcessorImpl implements OrderEventProcessor<InventoryEvent>{

    private final static Logger log = LoggerFactory.getLogger(OrderEventProcessorImpl.class);

    private final InventoryService service;

    @Override
    public Mono<InventoryEvent> handle(OrderEvent.OrderCreated event) {
        return this.service.processInventory(EventDTOMapper.toInventoryProcessRequest(event))
        .map(EventDTOMapper::toInventoryDeductedEvent)
        .doOnNext(evt -> log.info("Inventory deducted successfully for orderId: {}", evt.orderId()))
        .transform(exceptionHandler(event));
    }

    @Override
    public Mono<InventoryEvent> handle(OrderEvent.OrderCancelled event) {
        return this.service.processRestore(event.orderId())
        .map(EventDTOMapper::toInventoryRestoredEvent)
        .doOnNext(evt -> log.info("Inventory restored successfully for orderId: {}", evt.orderId()))
        .doOnError(ex -> log.error("error restoring inventory", ex));
    }

    private UnaryOperator<Mono<InventoryEvent>> exceptionHandler(OrderEvent.OrderCreated evt){
        return mono -> mono.onErrorResume(EventAlreadyProcessedException.class, e-> Mono.empty())
        .onErrorResume(EventDTOMapper.toInventoryFailedEvent(evt));
    }

}
