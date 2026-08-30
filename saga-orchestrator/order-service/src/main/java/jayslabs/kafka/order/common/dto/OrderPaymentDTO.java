package jayslabs.kafka.order.common.dto;

import java.util.UUID;

import jayslabs.kafka.common.events.payment.PaymentStatus;
import lombok.Builder;

@Builder
public record OrderPaymentDTO(
    UUID paymentId,
    UUID orderId,
    PaymentStatus status,
    String message
) {
}
