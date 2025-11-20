package jayslabs.kafka.payment.messaging.mapper;

import java.time.Instant;

import jayslabs.kafka.common.events.order.OrderEvent;
import jayslabs.kafka.common.events.payment.PaymentEvent;
import jayslabs.kafka.payment.common.dto.PaymentDTO;
import jayslabs.kafka.payment.common.dto.PaymentProcessRequest;

public class EventDTOMapper {
    public static PaymentProcessRequest toPaymentProcessRequest(OrderEvent.OrderCreated evt){
        return PaymentProcessRequest.builder()
        .customerId(evt.customerId())
        .orderId(evt.orderId())
        .amount(evt.totalAmount())
        .build();
    }

    public static PaymentEvent toPaymentDeductedEvent(PaymentDTO pymtDTO){
        return PaymentEvent.PaymentDeducted.builder()
        .orderId(pymtDTO.orderId())
        .paymentId(pymtDTO.paymentId())
        .customerId(pymtDTO.customerId())
        .amount(pymtDTO.amount())
        .createdAt(Instant.now())
        .build();
    }
}
