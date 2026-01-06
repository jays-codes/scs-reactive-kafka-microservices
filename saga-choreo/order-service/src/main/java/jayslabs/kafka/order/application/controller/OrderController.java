package jayslabs.kafka.order.application.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jayslabs.kafka.order.common.dto.OrderCreateRequest;
import jayslabs.kafka.order.common.dto.OrderDetailsDTO;
import jayslabs.kafka.order.common.dto.PurchaseOrderDTO;
import jayslabs.kafka.order.common.service.OrderService;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public Mono<ResponseEntity<PurchaseOrderDTO>> placeOrder(@RequestBody Mono<OrderCreateRequest> monoreq) {
        return monoreq.flatMap(orderService::placeOrder)
        .map(ResponseEntity.accepted()::body);
    }

    @GetMapping("all")
    public Flux<PurchaseOrderDTO> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("{id}")
    public Mono<OrderDetailsDTO> getOrderDetails(@PathVariable UUID orderId) {
        return orderService.getOrderDetails(orderId);
    }
}
