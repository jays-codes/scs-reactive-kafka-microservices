package jayslabs.kafka.shipping.application.repository;

import java.util.UUID;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import jayslabs.kafka.common.events.shipping.ShippingStatus;
import jayslabs.kafka.shipping.application.entity.Shipment;
import reactor.core.publisher.Mono;

@Repository
public interface ShipmentRepository extends ReactiveCrudRepository<Shipment, UUID> {

    Mono<Boolean> existsByOrderId(UUID orderId);
    Mono<Shipment> findByOrderIdAndStatus(UUID orderId, ShippingStatus status);
    
    Mono<Void> deleteByOrderId(UUID orderId);

}
