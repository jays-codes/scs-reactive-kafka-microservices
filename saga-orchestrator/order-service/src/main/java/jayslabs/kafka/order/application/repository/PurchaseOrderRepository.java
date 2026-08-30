package jayslabs.kafka.order.application.repository;

import java.util.UUID;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import jayslabs.kafka.common.events.order.OrderStatus;
import jayslabs.kafka.order.application.entity.PurchaseOrder;
import reactor.core.publisher.Mono;


@Repository
public interface PurchaseOrderRepository extends ReactiveCrudRepository<PurchaseOrder, UUID> {
    Mono<PurchaseOrder> findByOrderIdAndStatus(UUID orderId, OrderStatus status);

    @Query("""
            SELECT po.*
            FROM   purchase_order po
            WHERE  po.order_id = :orderId
                AND po.status = 'PENDING'
                AND EXISTS
                (
                            SELECT 1
                            FROM   order_payment op,
                                    order_inventory oi
                            WHERE  op.order_id = po.order_id
                                    AND oi.order_id = po.order_id
                                    AND op.success
                                    AND oi.success
                )
    """)
    Mono<PurchaseOrder> getWhenOrderComponentsAreSuccess(UUID orderId);    
}

