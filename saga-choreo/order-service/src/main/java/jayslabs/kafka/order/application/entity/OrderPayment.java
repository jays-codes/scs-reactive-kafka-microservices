package jayslabs.kafka.order.application.entity;

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
public class OrderPayment {

    @Id
    private Integer id;
    private UUID orderId;
    private UUID paymentId;
    private Boolean success;
    private String message;
    private PaymentStatus status;
}
