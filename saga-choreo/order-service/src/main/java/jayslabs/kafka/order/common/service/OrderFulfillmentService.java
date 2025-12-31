package jayslabs.kafka.order.common.service;

import java.util.UUID;

import jayslabs.kafka.order.common.dto.PurchaseOrderDTO;
import reactor.core.publisher.Mono;

public interface OrderFulfillmentService {

    Mono<PurchaseOrderDTO> completeOrder(UUID orderId);

    Mono<PurchaseOrderDTO> cancelOrder(UUID orderId);
}
