package jayslabs.kafka.shipping.common.dto;

import lombok.Builder;
import java.time.Instant;
import java.util.UUID;
import jayslabs.kafka.common.events.shipping.ShippingStatus;

@Builder
public record ShipmentDTO(
    UUID id,
    UUID orderId,
    Integer productId,
    Integer customerId,
    Integer quantity,
    ShippingStatus status,
    Instant deliveryDate
) {
    
}
