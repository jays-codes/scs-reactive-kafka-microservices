package jayslabs.kafka.order.common.dto;

import java.util.UUID;

import jayslabs.kafka.common.events.inventory.InventoryStatus;
import lombok.Builder;

@Builder    
public record OrderInventoryDTO(
    UUID inventoryId,
    UUID orderId,
    InventoryStatus status,
    String message
) {

}
