package jayslabs.kafka.order.common.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder
public record OrderShipmentDTO(
    UUID orderId,
    Instant deliveryDate
) {
}

