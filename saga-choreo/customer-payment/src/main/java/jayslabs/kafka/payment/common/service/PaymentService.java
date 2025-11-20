package jayslabs.kafka.payment.common.service;

import java.util.UUID;

import jayslabs.kafka.payment.common.dto.PaymentDTO;
import jayslabs.kafka.payment.common.dto.PaymentProcessRequest;
import reactor.core.publisher.Mono;

public interface PaymentService {

    Mono<PaymentDTO> processPayment(PaymentProcessRequest request);

    Mono<PaymentDTO> processRefund(UUID orderId);
}
