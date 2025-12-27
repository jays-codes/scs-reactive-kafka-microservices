package jayslabs.kafka.order.application.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import jayslabs.kafka.order.application.entity.PurchaseOrder;
import jayslabs.kafka.common.events.order.OrderStatus;
import reactor.core.publisher.Mono;

@Repository
public interface PurchaseOrderRepository extends ReactiveCrudRepository<PurchaseOrder, UUID> {
    Mono<PurchaseOrder> findByOrderIdAndStatus(UUID orderId, OrderStatus status);
}

