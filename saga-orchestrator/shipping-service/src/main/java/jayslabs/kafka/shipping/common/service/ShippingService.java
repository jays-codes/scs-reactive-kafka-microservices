package jayslabs.kafka.shipping.common.service;

import java.util.UUID;

import jayslabs.kafka.shipping.common.dto.CreateShippingRequest;
import jayslabs.kafka.shipping.common.dto.ShipmentDTO;
import reactor.core.publisher.Mono;

public interface ShippingService {

    Mono<Void> createShipmentRecord(CreateShippingRequest request);


    //orderId is used instead of shipmentId because cancel is being invoked from an 
    //OrderEvent.OrderCancelled event and that contains an orderId, not a shipmentId.
    Mono<Void> cancelShipment(UUID orderId);

    Mono<ShipmentDTO> scheduleShipment(UUID orderId);

}
