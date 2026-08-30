package jayslabs.kafka.order.messaging.mapper;

import jayslabs.kafka.common.events.shipping.ShippingEvent;
import jayslabs.kafka.order.common.dto.OrderShipmentDTO;

/*
As Shipping Events are consumed by the Order Service, 
we need to map them to OrderShipmentDTOs.
*/
public class ShippingEventMapper {

    public static OrderShipmentDTO toOrderShipmentDTO(ShippingEvent.ShippingScheduled evt){
        return OrderShipmentDTO.builder()
        .orderId(evt.orderId())
        .deliveryDate(evt.expectedDeliveryDate())
        .build();
    }
}
