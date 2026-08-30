package jayslabs.kafka.payment.application.entity;

import java.util.UUID;

import org.springframework.data.annotation.Id;

import jayslabs.kafka.common.events.payment.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPayment {

    @Id
    private UUID paymentId;
    private UUID orderId;
    private Integer customerId;
    private PaymentStatus status;
    private Integer amount;

}
