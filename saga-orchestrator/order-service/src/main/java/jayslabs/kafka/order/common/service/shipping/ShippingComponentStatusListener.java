package jayslabs.kafka.order.common.service.shipping;

import jayslabs.kafka.order.common.dto.OrderShipmentDTO;
import jayslabs.kafka.order.common.service.OrderComponentStatusListener;

public interface ShippingComponentStatusListener extends OrderComponentStatusListener<OrderShipmentDTO>{
    
}