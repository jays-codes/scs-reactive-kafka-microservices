package jayslabs.kafka.inventory.application.mapper;

import jayslabs.kafka.inventory.application.entity.OrderInventory;
import jayslabs.kafka.inventory.common.dto.InventoryDTO;
import jayslabs.kafka.inventory.common.dto.InventoryProcessRequest;

public class EntityDTOMapper {
    public static OrderInventory toOrderInventory(InventoryProcessRequest request){
        return OrderInventory.builder()
        .productId(request.productId())
        .orderId(request.orderId())
        .quantity(request.quantity())
        .build();
    }

    public static InventoryDTO toInventoryDTO(OrderInventory inventory){
        return InventoryDTO.builder()
        .inventoryId(inventory.getInventoryId())
        .orderId(inventory.getOrderId())
        .productId(inventory.getProductId())
        .quantity(inventory.getQuantity())
        .status(inventory.getStatus())
        .build();
    }
}
