package jayslabs.kafka.order.application.entity;

import java.util.UUID;

import org.springframework.data.annotation.Id;

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
    private Integer id;
    private UUID orderId;
    private UUID inventoryId;
    private Boolean success;
    private InventoryStatus status;
    private String message;
}
