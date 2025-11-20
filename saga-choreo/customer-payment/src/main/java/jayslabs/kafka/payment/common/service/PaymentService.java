package jayslabs.kafka.payment.common.service;

import java.util.UUID;

import jayslabs.kafka.payment.common.dto.PaymentDTO;
import jayslabs.kafka.payment.common.dto.PaymentProcessRequestDTO;
import reactor.core.publisher.Mono;

public interface PaymentService {

    Mono<PaymentDTO> processPayment(PaymentProcessRequestDTO request);

    Mono<PaymentDTO> processRefund(UUID orderId);
}
