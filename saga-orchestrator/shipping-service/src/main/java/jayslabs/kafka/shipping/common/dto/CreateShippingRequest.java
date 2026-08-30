package jayslabs.kafka.shipping.common.dto;

import java.util.UUID;

import lombok.Builder;

@Builder
public record CreateShippingRequest(
    UUID orderId,
    Integer productId,
    Integer customerId,
    Integer quantity
) {

}
