package jayslabs.kafka.order.common.dto;

import java.time.Instant;
import java.util.UUID;

import jayslabs.kafka.common.events.order.OrderStatus;
import lombok.Builder;

@Builder
public record PurchaseOrderDTO(
    UUID orderId,
    Integer customerId,
    Integer productId,
    Integer quantity,
    Integer unitPrice,
    Integer amount,
    OrderStatus status,
    Instant deliveryDate
) {

}
