package jayslabs.kafka.order.messaging.mapper;

import java.time.Instant;

import jayslabs.kafka.common.events.order.OrderEvent;
import jayslabs.kafka.order.common.dto.PurchaseOrderDTO;

/*
As Order Events are emitted by the Order Service, 
we need to map them to OrderEvents.
*/
public class OrderEventMapper {

    public static OrderEvent toOrderCreatedEvent(PurchaseOrderDTO purchaseOrderDTO){
        return OrderEvent.OrderCreated.builder()
        .orderId(purchaseOrderDTO.orderId())
        .customerId(purchaseOrderDTO.customerId())
        .productId(purchaseOrderDTO.productId())
        .quantity(purchaseOrderDTO.quantity())
        .price(purchaseOrderDTO.unitPrice())
        .totalAmount(purchaseOrderDTO.amount())
        .createdAt(Instant.now())
        .build();
    }

    public static OrderEvent toOrderCancelledEvent(PurchaseOrderDTO purchaseOrderDTO){
        return OrderEvent.OrderCancelled.builder()
        .orderId(purchaseOrderDTO.orderId())
        .createdAt(Instant.now())
        .build();
    }

    public static OrderEvent toOrderCompletedEvent(PurchaseOrderDTO purchaseOrderDTO){
        return OrderEvent.OrderCompleted.builder()
        .orderId(purchaseOrderDTO.orderId())
        .createdAt(Instant.now())
        .build();
    }
}
