package jayslabs.kafka.payment.application.entity;

import java.util.UUID;

import org.springframework.data.annotation.Id;

import jayslabs.kafka.common.events.payment.PaymentStatus;

public class CustomerPayment {

    @Id
    private UUID paymentId;
    private UUID orderId;
    private Integer customerId;
    private PaymentStatus status;
    private Integer amount;

}
