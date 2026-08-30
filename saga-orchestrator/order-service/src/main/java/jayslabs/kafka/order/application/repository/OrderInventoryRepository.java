package jayslabs.kafka.order.application.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import jayslabs.kafka.order.application.entity.OrderInventory;
import reactor.core.publisher.Mono;

@Repository
public interface OrderInventoryRepository extends ReactiveCrudRepository<OrderInventory, Integer> {
    Mono<OrderInventory> findByOrderId(UUID orderId);

}
