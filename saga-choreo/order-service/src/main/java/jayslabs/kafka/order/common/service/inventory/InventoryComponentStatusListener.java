package jayslabs.kafka.order.common.service.inventory;

import jayslabs.kafka.order.common.dto.OrderInventoryDTO;
import jayslabs.kafka.order.common.service.OrderComponentStatusListener;

public interface InventoryComponentStatusListener extends OrderComponentStatusListener<OrderInventoryDTO>{
    
}
