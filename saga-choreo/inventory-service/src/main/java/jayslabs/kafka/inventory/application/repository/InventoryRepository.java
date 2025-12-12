package jayslabs.kafka.inventory.application.repository;

import java.util.UUID;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import jayslabs.kafka.common.events.inventory.InventoryStatus;
import jayslabs.kafka.inventory.application.entity.OrderInventory;
import reactor.core.publisher.Mono;

@Repository
public interface InventoryRepository extends ReactiveCrudRepository<OrderInventory, UUID> {

    //to check if an inventory already exists for an order
    Mono<Boolean> existsByOrderId(UUID orderId);

    //to find an inventory by orderId and status
    Mono<OrderInventory> findByOrderIdAndStatus(UUID orderId, InventoryStatus status);
}
