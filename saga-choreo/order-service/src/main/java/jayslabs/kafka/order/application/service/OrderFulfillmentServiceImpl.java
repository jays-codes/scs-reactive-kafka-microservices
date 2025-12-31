package jayslabs.kafka.order.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import jayslabs.kafka.common.events.order.OrderStatus;
import jayslabs.kafka.order.application.mapper.EntityDTOMapper;
import jayslabs.kafka.order.application.repository.PurchaseOrderRepository;
import jayslabs.kafka.order.common.dto.PurchaseOrderDTO;
import jayslabs.kafka.order.common.service.OrderFulfillmentService;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

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
        .doOnNext(entity -> entity.setStatus(OrderStatus.COMPLETED))
        .flatMap(this.porepo::save)
        .map(EntityDTOMapper::toPurchaseOrderDTO);
    }

    @Override
    public Mono<PurchaseOrderDTO> cancelOrder(UUID orderId) {
        return this.porepo.findByOrderIdAndStatus(orderId, OrderStatus.PENDING)
        .doOnNext(entity -> entity.setStatus(OrderStatus.CANCELLED))
        .flatMap(this.porepo::save)
        .map(EntityDTOMapper::toPurchaseOrderDTO);
    }
}
