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

        //line below updates order_inventory table (component tracking) by
        //invoking onSuccess() in InventoryComponentServiceImpl
        return this.statusListener.onSuccess(dto) 

        //line below invokes completeOrder() in OrderFulfillmentServiceImpl
        //which updates purchase_order table (order tracking) to COMPLETED
        //if inventory is deducted successfully and payment is deducted successfully
        //else Mono.empty() is returned
        .then(this.fulfillmentService.completeOrder(event.orderId())) 
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
        .then(Mono.empty());
    }
}

/*
┌─────────────────────────────────────────────────────────────────┐
│                     INVENTORY SERVICE                           │
│  (Processes inventory reservation/restoration)                  │
└─────────────────────────────────────────────────────────────────┘
                            ↓ publishes
┌─────────────────────────────────────────────────────────────────┐
│              Kafka Topic: inventory-events                      │
│  (InventoryDeducted, InventoryFailed, InventoryRestored)        │
└─────────────────────────────────────────────────────────────────┘
                            ↓ consumes
┌─────────────────────────────────────────────────────────────────┐
│                 ORDER SERVICE (Saga Coordinator)                │
├─────────────────────────────────────────────────────────────────┤
│  InventoryEventProcessorConfig                                  │
│    ↓ (Spring Cloud Stream Function)                             │
│  InventoryEventProcessorImpl ← YOU ARE HERE                     │
│    ├─ Consumes InventoryEvent                                   │
│    ├─ Updates order_inventory table (component tracking)        │
│    ├─ Decides: Complete or Cancel order?                        │
│    └─ Emits OrderEvent (OrderCompleted/OrderCancelled)          │
└─────────────────────────────────────────────────────────────────┘
                            ↓ publishes
┌─────────────────────────────────────────────────────────────────┐
│              Kafka Topic: order-events                          │
│  (OrderCompleted, OrderCancelled)                               │
└─────────────────────────────────────────────────────────────────┘
*/