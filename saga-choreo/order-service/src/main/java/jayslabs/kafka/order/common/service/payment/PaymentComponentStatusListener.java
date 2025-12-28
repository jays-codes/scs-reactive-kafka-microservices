package jayslabs.kafka.order.common.service.payment;

import jayslabs.kafka.order.common.dto.OrderPaymentDTO;
import jayslabs.kafka.order.common.service.OrderComponentStatusListener;

public interface PaymentComponentStatusListener extends OrderComponentStatusListener<OrderPaymentDTO>{
    
}

