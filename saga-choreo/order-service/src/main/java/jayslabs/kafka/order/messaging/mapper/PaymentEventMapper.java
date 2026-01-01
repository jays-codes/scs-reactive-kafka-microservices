package jayslabs.kafka.order.messaging.mapper;

import jayslabs.kafka.common.events.payment.PaymentEvent;
import jayslabs.kafka.common.events.payment.PaymentStatus;
import jayslabs.kafka.order.common.dto.OrderPaymentDTO;

/*
As Payment Events are consumed by the Order Service, 
we need to map them to OrderPaymentDTOs.
*/
public class PaymentEventMapper {

    public static OrderPaymentDTO toOrderPaymentDTO(PaymentEvent.PaymentDeducted evt){
        return OrderPaymentDTO.builder()
        .orderId(evt.orderId())
        .paymentId(evt.paymentId())
        .status(PaymentStatus.DEDUCTED)
        .build();
    }

    public static OrderPaymentDTO toOrderPaymentDTO(PaymentEvent.PaymentFailed evt){
        return OrderPaymentDTO.builder()
        .orderId(evt.orderId())
        .status(PaymentStatus.FAILED)
        .message(evt.message())
        .build();
    }

    public static OrderPaymentDTO toOrderPaymentDTO(PaymentEvent.PaymentRefunded evt){
        return OrderPaymentDTO.builder()
        .orderId(evt.orderId())
        .status(PaymentStatus.REFUNDED)
        .build();
    }   

}
