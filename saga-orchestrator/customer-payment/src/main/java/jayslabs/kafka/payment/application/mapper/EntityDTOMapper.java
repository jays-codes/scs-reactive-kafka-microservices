package jayslabs.kafka.payment.application.mapper;

import jayslabs.kafka.payment.application.entity.CustomerPayment;
import jayslabs.kafka.payment.common.dto.PaymentDTO;
import jayslabs.kafka.payment.common.dto.PaymentProcessRequest;

public class EntityDTOMapper {
    public static CustomerPayment toCustomerPayment(PaymentProcessRequest request){
        return CustomerPayment.builder()
        .customerId(request.customerId())
        .orderId(request.orderId())
        .amount(request.amount())
        .build();
    }

    public static PaymentDTO toPaymentDTO(CustomerPayment payment){
        return PaymentDTO.builder()
        .paymentId(payment.getPaymentId())
        .orderId(payment.getOrderId())
        .customerId(payment.getCustomerId())
        .amount(payment.getAmount())
        .status(payment.getStatus())
        .build();
    }
}
