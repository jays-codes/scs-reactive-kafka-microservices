package jayslabs.kafka.order.common.service;

import java.util.UUID;

import jayslabs.kafka.order.common.dto.OrderCreateRequest;
import jayslabs.kafka.order.common.dto.OrderDetailsDTO;
import jayslabs.kafka.order.common.dto.PurchaseOrderDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderService {

    Mono<PurchaseOrderDTO> placeOrder(OrderCreateRequest request);

    Flux<PurchaseOrderDTO> getAllOrders();

    Mono<OrderDetailsDTO> getOrderDetails(UUID orderId);
}
