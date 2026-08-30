package jayslabs.kafka.payment.common.dto;

import java.util.UUID;

import jayslabs.kafka.common.events.payment.PaymentStatus;
import lombok.Builder;

@Builder
public record PaymentDTO(
    UUID paymentId, 
    UUID orderId, 
    Integer customerId, 
    Integer amount,
    PaymentStatus status) {

}
