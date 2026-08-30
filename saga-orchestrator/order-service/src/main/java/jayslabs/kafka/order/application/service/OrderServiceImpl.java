package jayslabs.kafka.order.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import jayslabs.kafka.order.application.mapper.EntityDTOMapper;
import jayslabs.kafka.order.application.repository.PurchaseOrderRepository;
import jayslabs.kafka.order.common.dto.OrderCreateRequest;
import jayslabs.kafka.order.common.dto.OrderDetailsDTO;
import jayslabs.kafka.order.common.dto.PurchaseOrderDTO;
import jayslabs.kafka.order.common.service.OrderEventListener;
import jayslabs.kafka.order.common.service.OrderService;
import jayslabs.kafka.order.common.service.inventory.InventoryComponentFetcher;
import jayslabs.kafka.order.common.service.payment.PaymentComponentFetcher;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final PurchaseOrderRepository porepo;
    private final OrderEventListener ordEvtLstnr;
    private final PaymentComponentFetcher paymentCompFetcher;
    private final InventoryComponentFetcher inventoryCompFetcher;


    @Override
    public Mono<PurchaseOrderDTO> placeOrder(OrderCreateRequest request) {
        
        //Create PurchaseOrder entity 
        var purchaseOrder = EntityDTOMapper.toPurchaseOrder(request);
        return this.porepo.save(purchaseOrder) //save to database
        .map(EntityDTOMapper::toPurchaseOrderDTO) //convert to DTO
        .doOnNext(ordEvtLstnr::emitOrderCreated); //and emit event to Kafka
    }

    @Override
    public Flux<PurchaseOrderDTO> getAllOrders() {
        return this.porepo.findAll()
        .map(EntityDTOMapper::toPurchaseOrderDTO);
    }

    @Override
    public Mono<OrderDetailsDTO> getOrderDetails(UUID orderId) {
        return this.porepo.findById(orderId)
        .map(EntityDTOMapper::toPurchaseOrderDTO)
        .flatMap(podto -> this.paymentCompFetcher.getComponent(orderId)
            .zipWith(this.inventoryCompFetcher.getComponent(orderId))
            .map(tup -> EntityDTOMapper.toOrderDetailsDTO(podto, tup.getT1(), tup.getT2()))
        );
    }

/*
Flow Diagram for getOrderDetails():

porepo.findById(abc-123)
  ↓
  PurchaseOrderDTO (podto)
  ↓
  flatMap() starts two parallel operations:
  
  ┌─────────────────────────────────┐  ┌──────────────────────────────────┐
  │ paymentCompFetcher              │  │ inventoryCompFetcher             │
  │   .getComponent(abc-123)        │  │   .getComponent(abc-123)         │
  │                                 │  │                                  │
  │ SELECT * FROM order_payment     │  │ SELECT * FROM order_inventory    │
  │ WHERE order_id = 'abc-123'      │  │ WHERE order_id = 'abc-123'       │
  │                                 │  │                                  │
  │ ↓ (50ms)                        │  │ ↓ (60ms)                         │
  │ OrderPaymentDTO                 │  │ OrderInventoryDTO                │
  └─────────────────────────────────┘  └──────────────────────────────────┘
                  ↓                                ↓
                  └────────────┬───────────────────┘
                               ↓
                        zipWith() waits for BOTH
                               ↓
                  Tuple2<OrderPaymentDTO, OrderInventoryDTO>
                               ↓
                      toOrderDetailsDTO(...)
                               ↓
                       OrderDetailsDTO

*/
}

/*
Flow Diagram for placeOrder():

┌─────────────────────────────────────────────────────────────────────┐
│                    REST CONTROLLER                                  │
│  POST /orders                                                       │
│  Body: OrderCreateRequest(customerId=1, productId=100, qty=2, ...)  │
└─────────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────────┐
│                  OrderServiceImpl.placeOrder()                      │
├─────────────────────────────────────────────────────────────────────┤
│  Step 1: EntityDTOMapper.toPurchaseOrder(request)                   │
│    → PurchaseOrder entity (orderId=null, status=PENDING)            │
│                                                                     │
│  Step 2: porepo.save(purchaseOrder)                                 │
│    → INSERT INTO purchase_order                                     │
│    → Mono<PurchaseOrder> (orderId=abc-123, status=PENDING)          │
│                                                                     │
│  Step 3: .map(EntityDTOMapper::toPurchaseOrderDTO)                  │
│    → Mono<PurchaseOrderDTO> (immutable)                             │
│                                                                     │
│  Step 4: .doOnNext(ordEvtLstnr::emitOrderCreated)                   │
│    → Emit OrderEvent.OrderCreated to Kafka                          │
└─────────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────────┐
│                    KAFKA (order-events topic)                       │
│  OrderEvent.OrderCreated published                                  │
└─────────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────────┐
│              PARALLEL PROCESSING (Saga Choreography)                │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────────┐     │
│  │ Payment Service │  │Inventory Service│  │Shipping Service  │     │
│  │ (processes      │  │ (reserves stock)│  │ (creates record) │     │
│  │  payment)       │  │                 │  │                  │     │
│  └─────────────────┘  └─────────────────┘  └──────────────────┘     │
└─────────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────────┐
│                    RESPONSE TO CLIENT                               │
│  HTTP 200 OK                                                        │
│  Body: PurchaseOrderDTO(orderId=abc-123, status=PENDING, ...)       │
└─────────────────────────────────────────────────────────────────────┘

*/
