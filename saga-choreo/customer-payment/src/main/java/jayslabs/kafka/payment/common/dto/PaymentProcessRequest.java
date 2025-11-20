package jayslabs.kafka.payment.common.dto;

import java.util.UUID;

import lombok.Builder;

@Builder
public record PaymentProcessRequest(
    Integer customerId, 
    UUID orderId, 
    Integer amount) {

}
