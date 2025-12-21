package jayslabs.kafka.shipping.application.mapper;

import jayslabs.kafka.shipping.application.entity.Shipment;
import jayslabs.kafka.shipping.common.dto.CreateShippingRequest;
import jayslabs.kafka.shipping.common.dto.ShipmentDTO;

public class EntityDTOMapper {
    public static Shipment toShipment(CreateShippingRequest request){
        return Shipment.builder()
        .orderId(request.orderId())
        .productId(request.productId())
        .customerId(request.customerId())
        .quantity(request.quantity())
        .build();
    }

    public static ShipmentDTO toShipmentDTO(Shipment shipment){
        return ShipmentDTO.builder()
        .shipmentId(shipment.getId())
        .orderId(shipment.getOrderId())
        .productId(shipment.getProductId())
        .customerId(shipment.getCustomerId())
        .quantity(shipment.getQuantity())
        .status(shipment.getStatus())
        .deliveryDate(shipment.getDeliveryDate())
        .build();
    }
}
