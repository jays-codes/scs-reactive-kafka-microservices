package jayslabs.kafka.order.messaging.mapper;

import jayslabs.kafka.common.events.inventory.InventoryEvent;       
import jayslabs.kafka.common.events.inventory.InventoryStatus;
import jayslabs.kafka.order.common.dto.OrderInventoryDTO;

/*
As Inventory Events are consumed by the Order Service, 
we need to map them to OrderInventoryDTOs.
*/
public class InventoryEventMapper {

    public static OrderInventoryDTO toOrderInventoryDTO(InventoryEvent.InventoryDeducted evt){
        return OrderInventoryDTO.builder()
        .orderId(evt.orderId())
        .inventoryId(evt.inventoryId())
        .status(InventoryStatus.DEDUCTED)
        .build();
    }

    public static OrderInventoryDTO toOrderInventoryDTO(InventoryEvent.InventoryFailed evt){
        return OrderInventoryDTO.builder()
        .orderId(evt.orderId())
        .status(InventoryStatus.DECLINED)
        .message(evt.message())
        .build();
    }

    public static OrderInventoryDTO toOrderInventoryDTO(InventoryEvent.InventoryRestored evt){
        return OrderInventoryDTO.builder()
        .orderId(evt.orderId())
        .status(InventoryStatus.RESTORED)
        .build();
    }


}
