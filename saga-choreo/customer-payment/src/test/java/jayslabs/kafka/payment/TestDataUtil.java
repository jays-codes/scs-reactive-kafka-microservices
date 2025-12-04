package jayslabs.kafka.payment;

import java.time.Instant;
import java.util.UUID;

import jayslabs.kafka.common.events.order.OrderEvent;

public class TestDataUtil {

    public static OrderEvent.OrderCreated createOrderCreatedEvent(
        int custId, int prodId, int unitPrice, int qty){
            return OrderEvent.OrderCreated.builder()
                .orderId(UUID.randomUUID())
                .createdAt(Instant.now())
                .totalAmount(unitPrice * qty)
                .price(unitPrice)
                .quantity(qty)
                .customerId(custId)
                .productId(prodId)
                .build();
    }

    public static OrderEvent.OrderCancelled createOrderCancelledEvent(
        UUID orderId, String msg){
            return OrderEvent.OrderCancelled.builder()
                .orderId(orderId)
                .createdAt(Instant.now())
                .build();
    }    
}
