package jayslabs.kafka.inventory.common.dto;

import lombok.Builder;
import java.util.UUID;
import jayslabs.kafka.common.events.inventory.InventoryStatus;

@Builder    
public record InventoryDTO(
    UUID inventoryId,
    UUID orderId,
    Integer productId,
    Integer quantity,
    InventoryStatus status) {

}
