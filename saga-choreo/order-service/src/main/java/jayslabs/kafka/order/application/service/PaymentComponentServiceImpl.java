package jayslabs.kafka.order.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import jayslabs.kafka.order.application.entity.OrderPayment;
import jayslabs.kafka.order.application.mapper.EntityDTOMapper;
import jayslabs.kafka.order.application.repository.OrderPaymentRepository;
import jayslabs.kafka.order.common.dto.OrderPaymentDTO;
import jayslabs.kafka.order.common.service.payment.PaymentComponentFetcher;
import jayslabs.kafka.order.common.service.payment.PaymentComponentStatusListener;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class PaymentComponentServiceImpl implements PaymentComponentFetcher, PaymentComponentStatusListener {

    private final OrderPaymentRepository pymtrepo;

    private static final OrderPaymentDTO DEFAULT_DTO = OrderPaymentDTO.builder().build();

    @Override
    public Mono<OrderPaymentDTO> getComponent(UUID orderId) {
        return this.pymtrepo.findByOrderId(orderId)
        .map(EntityDTOMapper::toOrderPaymentDTO)
        .defaultIfEmpty(DEFAULT_DTO);
    }

    @Override
    public Mono<Void> onSuccess(OrderPaymentDTO event) {
        return this.pymtrepo.findByOrderId(event.orderId())
        .switchIfEmpty(Mono.defer(() -> this.add(event, true)))
        .then();
    }

    @Override
    public Mono<Void> onFailure(OrderPaymentDTO event) {
        return this.pymtrepo.findByOrderId(event.orderId())
        .switchIfEmpty(Mono.defer(() -> this.add(event, false)))
        .then();
    }
    
    @Override
    public Mono<Void> onRollback(OrderPaymentDTO event) {
        return this.pymtrepo.findByOrderId(event.orderId())
        .doOnNext(entity -> entity.setStatus(event.status()))
        .flatMap(this.pymtrepo::save)
        .then();
    }

    private Mono<OrderPayment> add(OrderPaymentDTO dto, boolean isSuccess) {
        var entity = EntityDTOMapper.toOrderPayment(dto);
        entity.setSuccess(isSuccess);
        return this.pymtrepo.save(entity);
    }
}

/*
Flow Diagram for getComponent()

getComponent(orderId = "abc-123")
        ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 1: pymtrepo.findByOrderId("abc-123")                       │
│   SQL: SELECT * FROM order_payment WHERE order_id = 'abc-123'   │
└─────────────────────────────────────────────────────────────────┘
        ↓
   ┌────────┴────────┐
   │                 │
   ↓ (Found)         ↓ (Not Found)
┌──────────────┐  ┌──────────────────┐
│OrderPayment  │  │ Mono.empty()     │
│entity        │  │                  │
└──────────────┘  └──────────────────┘
   ↓                 ↓
┌──────────────┐  ┌──────────────────┐
│Step 2:       │  │Step 3:           │
│.map(toDTO)   │  │.defaultIfEmpty() │
│              │  │                  │
│OrderPayment  │  │DEFAULT_DTO       │
│DTO           │  │(empty DTO)       │
└──────────────┘  └──────────────────┘
   ↓                 ↓
   └────────┬────────┘
            ↓
    Mono<OrderPaymentDTO>
    (always emits a value)
*/

/*
Flow Diagram for onSuccess()

Kafka: payment-events topic
  ↓
PaymentEvent.PaymentDeducted received
  ↓
Messaging layer converts to OrderPaymentDTO
  ↓
onSuccess(OrderPaymentDTO) called
        ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 1: pymtrepo.findByOrderId(abc-123)                         │
│   Check if record already exists (idempotency)                  │
└─────────────────────────────────────────────────────────────────┘
            ↓
   ┌────────┴────────┐
   │                 │
   ↓ (Found)         ↓ (Not Found)
┌──────────────┐  ┌──────────────────────────────────────────────┐
│OrderPayment  │  │ Step 2: Mono.defer(() -> add(event, true))   │
│already exists│  │                                              │
│              │  │ add() method:                                │
│Do nothing    │  │   1. EntityDTOMapper.toOrderPayment(dto)     │
│(idempotent)  │  │   2. entity.setSuccess(true)                 │
│              │  │   3. pymtrepo.save(entity)                   │
│              │  │      → INSERT INTO order_payment             │
└──────────────┘  └──────────────────────────────────────────────┘
   ↓                 ↓
   └────────┬────────┘
            ↓
    Step 3: .then()
    (Convert to Mono<Void>)
            ↓
        Mono<Void>
    (completion signal)
*/

/*
Scenario for Successful Payment

Time 0s:  Order created (orderId=abc-123)
          ↓
Time 0.1s: OrderCreated event → Kafka (order-events)
          ↓
Time 0.2s: Payment service processes event
          → Deducts balance
          → Emits PaymentDeducted event → Kafka (payment-events)
          ↓
Time 0.3s: Order service consumes PaymentDeducted
          → Messaging layer converts to OrderPaymentDTO
          → Calls onSuccess(OrderPaymentDTO)
          ↓
Time 0.4s: onSuccess() execution:
          1. findByOrderId(abc-123) → Mono.empty() (first time)
          2. add(event, true) executes
          3. INSERT INTO order_payment (
               payment_id='xyz-789',
               order_id='abc-123',
               status='DEDUCTED',
               success=true
             )
          ↓
Time 0.5s: Record created in order_payment table
          ↓
Time 1s:  Client calls GET /orders/abc-123
          → getComponent(abc-123) returns actual payment status
*/

//-------------------------------------

