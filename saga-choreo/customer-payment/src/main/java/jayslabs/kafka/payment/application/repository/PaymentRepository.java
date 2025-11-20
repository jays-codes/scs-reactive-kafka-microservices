package jayslabs.kafka.payment.application.repository;

import java.util.UUID;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import jayslabs.kafka.common.events.payment.PaymentStatus;
import jayslabs.kafka.payment.application.entity.CustomerPayment;
import reactor.core.publisher.Mono;

@Repository
public interface PaymentRepository extends ReactiveCrudRepository<CustomerPayment, UUID> {
    
    //to check if a payment already exists for an order
    Mono<Boolean> existsByOrderId(UUID orderId);

    //to find a payment by orderId
    Mono<CustomerPayment> findByOrderIdAndStatus(UUID orderId, PaymentStatus status);
}