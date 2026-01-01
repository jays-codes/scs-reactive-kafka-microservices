package jayslabs.kafka.common.events.order;

import java.time.Instant;
import java.util.UUID;

import jayslabs.kafka.common.events.DomainEvent;
import jayslabs.kafka.common.events.OrderSaga;
import lombok.Builder;

public sealed interface OrderEvent extends DomainEvent, OrderSaga 
{

    @Builder
    record OrderCreated(
        UUID orderId, 
        Integer productId,
        Integer customerId,
        Integer quantity,
        Integer price,
        Integer totalAmount,
        Instant createdAt) implements OrderEvent{
    }

    @Builder
    record OrderCancelled(
        UUID orderId,
        Instant createdAt) implements OrderEvent{
    }

    @Builder
    record OrderCompleted(
        UUID orderId,
        Instant createdAt) implements OrderEvent{
    }
}
