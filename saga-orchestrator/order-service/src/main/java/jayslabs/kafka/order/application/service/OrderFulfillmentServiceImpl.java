package jayslabs.kafka.order.application.service;

import java.util.UUID;
import java.util.function.Function;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import jayslabs.kafka.common.events.order.OrderStatus;
import jayslabs.kafka.order.application.entity.PurchaseOrder;
import jayslabs.kafka.order.application.mapper.EntityDTOMapper;
import jayslabs.kafka.order.application.repository.PurchaseOrderRepository;
import jayslabs.kafka.order.common.dto.PurchaseOrderDTO;
import jayslabs.kafka.order.common.service.OrderFulfillmentService;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
@RequiredArgsConstructor
public class OrderFulfillmentServiceImpl implements OrderFulfillmentService{

    private final PurchaseOrderRepository porepo;

    @Override
    public Mono<PurchaseOrderDTO> completeOrder(UUID orderId) {
        /*
        1. PurchaseOrder status must be PENDING
        2. Inventory and Payment components must be SUCCESS
        
        */
        return this.porepo.getWhenOrderComponentsAreSuccess(orderId)
        .transform(updateStatus(OrderStatus.COMPLETED));
    }

    @Override
    public Mono<PurchaseOrderDTO> cancelOrder(UUID orderId) {
        return this.porepo.findByOrderIdAndStatus(orderId, OrderStatus.PENDING)
        .transform(updateStatus(OrderStatus.CANCELLED));
    }

    private Function<Mono<PurchaseOrder>, Mono<PurchaseOrderDTO>> updateStatus(OrderStatus status){
        return mono -> mono
        .doOnNext(po -> po.setStatus(status))
        .flatMap(this.porepo::save)
        .retryWhen(Retry.max(1).filter(OptimisticLockingFailureException.class::isInstance))
        .map(EntityDTOMapper::toPurchaseOrderDTO);
    }
}
