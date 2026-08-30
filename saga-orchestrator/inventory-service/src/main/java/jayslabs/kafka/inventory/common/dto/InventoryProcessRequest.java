package jayslabs.kafka.inventory.common.dto;

import lombok.Builder;
import java.util.UUID;

@Builder    
public record InventoryProcessRequest(
    Integer productId,
    UUID orderId,
    Integer quantity){

}

