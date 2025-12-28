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

/*
Communication Flow:

┌─────────────────────────────────────────────────────────────────┐
│                    REST CONTROLLER LAYER                        │
│  POST /orders → placeOrder(OrderCreateRequest)                  │
│  GET /orders → getAllOrders()                                   │
│  GET /orders/{id} → getOrderDetails(orderId)                    │
└─────────────────────────────────────────────────────────────────┘
                            ↓ uses
┌─────────────────────────────────────────────────────────────────┐
│                     SERVICE LAYER                               │
│  OrderService (interface)                                       │
│    ├─ placeOrder() → saves to DB → emits event                  │
│    ├─ getAllOrders() → queries DB                               │
│    └─ getOrderDetails() → aggregates components                 │
└─────────────────────────────────────────────────────────────────┘
         ↓ emits events              ↓ fetches components
┌──────────────────────┐    ┌────────────────────────────────────┐
│ OrderEventListener   │    │ Component Fetchers                 │
│  emitOrderCreated()  │    │  PaymentComponentFetcher           │
└──────────────────────┘    │  InventoryComponentFetcher         │
         ↓                  └────────────────────────────────────┘
┌──────────────────────┐              ↓ queries
│  KAFKA (order-events)│    ┌────────────────────────────────────┐
└──────────────────────┘    │  DATABASE REPOSITORIES             │
         ↓                  │   OrderPaymentRepository           │
┌──────────────────────┐    │   OrderInventoryRepository         │
│ Payment/Inventory/   │    │   PurchaseOrderRepository          │
│ Shipping Services    │    └────────────────────────────────────┘
│ (process events)     │
└──────────────────────┘
         ↓ publish responses
┌──────────────────────┐
│ KAFKA (payment/      │
│ inventory/shipping   │
│ events topics)       │
└──────────────────────┘
         ↓ consumed by
┌──────────────────────────────────────────────────────────────────┐
│              MESSAGING LAYER (Order Service)                     │
│  Component Status Listeners (handle inbound events)              │
│    PaymentComponentStatusListener                                │
│      ├─ onSuccess(OrderPaymentDTO) → update order_payment table  │
│      ├─ onFailure(OrderPaymentDTO) → cancel order                │
│      └─ onRollback(OrderPaymentDTO) → update status              │
│    InventoryComponentStatusListener                              │
│      ├─ onSuccess(OrderInventoryDTO) → update order_inventory    │
│      ├─ onFailure(OrderInventoryDTO) → cancel order              │
│      └─ onRollback(OrderInventoryDTO) → update status            │
│    ShippingComponentStatusListener                               │
│      └─ onSuccess(OrderShippingDTO) → update deliveryDate        │
└──────────────────────────────────────────────────────────────────┘
*/