package jayslabs.kafka.shipping.messaging.mapper;

import java.time.Instant;

import jayslabs.kafka.common.events.order.OrderEvent;
import jayslabs.kafka.common.events.shipping.ShippingEvent;
import jayslabs.kafka.shipping.common.dto.CreateShippingRequest;
import jayslabs.kafka.shipping.common.dto.ShipmentDTO;


public class EventDTOMapper {

    public static CreateShippingRequest toCreateShippingRequest(OrderEvent.OrderCreated evt){
        return CreateShippingRequest.builder()
        .orderId(evt.orderId())
        .productId(evt.productId())
        .customerId(evt.customerId())
        .quantity(evt.quantity())
        .build();
    }

    public static ShippingEvent toShippingScheduledEvent(ShipmentDTO shipmentDTO){
        return ShippingEvent.ShippingScheduled.builder()
        .orderId(shipmentDTO.orderId())
        .shipmentId(shipmentDTO.shipmentId())
        .expectedDeliveryDate(shipmentDTO.deliveryDate())
        .createdAt(Instant.now())
        .build();
    }

}
