package jayslabs.kafka.order.application.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import jayslabs.kafka.order.application.entity.OrderPayment;
import reactor.core.publisher.Mono;

@Repository
public interface OrderPaymentRepository extends ReactiveCrudRepository<OrderPayment, Integer> {
    Mono<OrderPayment> findByOrderId(UUID orderId);

}

