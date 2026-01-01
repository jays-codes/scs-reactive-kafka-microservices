package jayslabs.kafka.order.application.service;

import org.springframework.stereotype.Service;

import jayslabs.kafka.order.application.repository.PurchaseOrderRepository;
import jayslabs.kafka.order.common.dto.OrderShipmentDTO;
import jayslabs.kafka.order.common.service.shipping.ShippingComponentStatusListener;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ShippingComponentServiceImpl implements ShippingComponentStatusListener {

    private final PurchaseOrderRepository porepo;

    private static final OrderShipmentDTO DEFAULT_DTO = OrderShipmentDTO.builder().build();

    /*
    local copy of deliveryDate is in purchase_order table as there is 
    no separate shipping table
    */
    @Override
    public Mono<Void> onSuccess(OrderShipmentDTO event) {
        return this.porepo.findById(event.orderId())
        .doOnNext(entity -> entity.setDeliveryDate(event.deliveryDate()))
        .flatMap(this.porepo::save)
        .then();
    }

    //not applicable for shipping component
    @Override
    public Mono<Void> onFailure(OrderShipmentDTO event) {
        return Mono.empty();
    }

    //not applicable for shipping component
    @Override
    public Mono<Void> onRollback(OrderShipmentDTO event) {
        return Mono.empty();
    }
}
