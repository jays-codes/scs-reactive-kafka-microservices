package jayslabs.kafka.payment.application.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import jayslabs.kafka.payment.application.entity.CustomerPayment;
import reactor.core.publisher.Mono;

@Repository
public interface PaymentRepository extends ReactiveCrudRepository<CustomerPayment, UUID> {
    
    //to check if a payment already exists for an order
    Mono<Boolean> existsByOrderId(UUID orderId);
}