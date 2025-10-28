package jayslabs.kafka.common.events.inventory;

import java.time.Instant;
import java.util.UUID;

import jayslabs.kafka.common.events.DomainEvent;
import jayslabs.kafka.common.events.OrderSaga;
import lombok.Builder;

public sealed interface InventoryEvent extends DomainEvent, OrderSaga {

        @Builder
        record InventoryDeducted(
            UUID orderId,
            UUID inventoryId,
            Integer productId,
            Integer quantity,
            Instant createdAt) implements InventoryEvent{
        }

        @Builder
        record InventoryRestored(
            UUID orderId,
            UUID inventoryId,
            Integer productId,
            Integer quantity,
            Instant createdAt) implements InventoryEvent{
        }

        //InventoryFailed
        @Builder
        record InventoryFailed(
            UUID orderId,
            Integer productId,
            Integer quantity,
            String message,
            Instant createdAt) implements InventoryEvent{
        }
}
