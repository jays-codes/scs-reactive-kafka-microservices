package jayslabs.kafka.common.events.payment;

import java.time.Instant;
import java.util.UUID;

import jayslabs.kafka.common.events.DomainEvent;
import jayslabs.kafka.common.events.OrderSaga;
import lombok.Builder;

public sealed interface PaymentEvent extends DomainEvent, OrderSaga {
    
    //PaymentDedeucted, PaymentRefunded
    @Builder
    record PaymentDeducted(
        UUID orderId,
        UUID paymentId,
        Integer customerId,
        Integer amount,
        Instant createdAt) implements PaymentEvent{
    }

    @Builder
    record PaymentRefunded(
        UUID orderId,
        UUID paymentId,
        Integer customerId,
        Integer amount,
        Instant createdAt) implements PaymentEvent{
    }

    @Builder
    record PaymentFailed(
        UUID orderId,
        Integer customerId,
        Integer amount,
        String message,
        Instant createdAt) implements PaymentEvent{
    }
}
