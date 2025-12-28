package jayslabs.kafka.order.common.service.shipping;

import jayslabs.kafka.order.common.dto.OrderShippingDTO;
import jayslabs.kafka.order.common.service.OrderComponentStatusListener;

public interface ShippingComponentStatusListener extends OrderComponentStatusListener<OrderShippingDTO>{
    
}

