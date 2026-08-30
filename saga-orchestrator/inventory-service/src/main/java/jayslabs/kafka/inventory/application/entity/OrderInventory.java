package jayslabs.kafka.inventory.application.entity;

import org.springframework.data.annotation.Id;

import java.util.UUID;
import jayslabs.kafka.common.events.inventory.InventoryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderInventory {

    @Id
    private UUID inventoryId;
    private UUID orderId;
    private Integer productId;
    private InventoryStatus status;
    private Integer quantity;
}
