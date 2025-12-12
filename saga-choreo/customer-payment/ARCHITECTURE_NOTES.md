# Customer Payment Service - Architecture & Design Notes

## Overview

The **customer-payment** service is a reactive microservice implementing the Saga Choreography pattern for distributed payment processing. It consumes `OrderEvent` messages from Kafka, processes payments/refunds, and publishes `PaymentEvent` messages for downstream services (inventory, shipping).

**Key Technologies:**
- Spring Boot 3.5.6
- Spring Cloud Stream (Kafka integration)
- Project Reactor (reactive programming)
- Spring Data R2DBC (reactive database access)
- H2 Database (embedded)
- Lombok (boilerplate reduction)
- Java 21 (sealed interfaces, pattern matching, records)

---

## Database Schema

### Table 1: `customer`
| Column | Type | Description |
|--------|------|-------------|
| `id` | Integer | Primary key |
| `name` | String | Customer name |
| `balance` | Integer | Available balance (e.g., 100 = $100) |

**Purpose:** Stores customer account information and current balance

**Data Population:**
- Pre-populated on application startup via `data.sql`
- Sample data: anya (100), becky (100), bondo (100)
- No REST endpoints for customer creation (read-only from service perspective)

### Table 2: `customer_payment`
| Column | Type | Description |
|--------|------|-------------|
| `payment_id` | UUID | Primary key (auto-generated) |
| `order_id` | UUID | Foreign key to order (saga correlation ID) |
| `customer_id` | Integer | Foreign key to customer |
| `status` | Enum | Payment status (DEDUCTED, REFUNDED, FAILED) |
| `amount` | Integer | Payment amount |

**Purpose:** Audit trail of all payment transactions (deductions and refunds)

**Initial State:** Empty table (populated as events are processed)

**Idempotency:** `order_id` is used to prevent duplicate payment processing via `existsByOrderId()` query

---

## Business Logic Flow

### Scenario 1: Order Created - Payment Deduction (Happy Path)

**Input:** `OrderEvent.OrderCreated`
```java
OrderCreated(
  orderId: abc-123,
  customerId: 1 (anya),
  productId: 100,
  quantity: 2,
  unitPrice: 5,
  totalAmount: 10,
  createdAt: 2025-12-12T10:00:00Z
)
```

**Processing Steps:**
1. **Kafka → Messaging Layer:** `OrderEventProcessorConfig.processor()` receives `Message<OrderEvent>`
2. **Convert to Domain:** `MessageConverter.toRecord()` extracts payload, key, acknowledgment
3. **Route Event:** `OrderEventProcessorImpl.handle(OrderCreated)` via pattern matching
4. **Map to DTO:** `EventDTOMapper.toPaymentProcessRequest()` → `PaymentProcessRequest(customerId=1, orderId=abc-123, amount=10)`
5. **Service Layer:** `PaymentServiceImpl.processPayment()` executes 5-phase reactive pipeline:
   - **Phase 1 (Idempotency):** `pymtRepo.existsByOrderId(abc-123)` → false (new event)
   - **Phase 2 (Customer Validation):** `custRepo.findById(1)` → `Customer(id=1, name="anya", balance=100)`
   - **Phase 3 (Balance Validation):** `filter(100 >= 10)` → passes
   - **Phase 4 (Payment Deduction):** 
     - Create `CustomerPayment` entity (orderId, customerId, amount)
     - Update customer balance: `100 - 10 = 90` (in-memory)
     - Set payment status: `DEDUCTED`
     - `custRepo.save(customer)` → UPDATE customer SET balance=90
     - `.then(pymtRepo.save(payment))` → INSERT INTO customer_payment
     - Auto-generated `paymentId: xyz-789`
   - **Phase 5 (Logging):** `doOnNext(log.info("Payment deducted..."))`
6. **Map to Event:** `EventDTOMapper.toPaymentDeductedEvent()` → `PaymentEvent.PaymentDeducted`
7. **Kafka Output:** `toMessage()` sets `KafkaHeaders.KEY = orderId` for partitioning
8. **Acknowledge:** `ReceiverOffset.acknowledge()` commits Kafka offset

**Output:** `PaymentEvent.PaymentDeducted`
```java
PaymentDeducted(
  orderId: abc-123,
  paymentId: xyz-789,
  customerId: 1,
  amount: 10,
  createdAt: 2025-12-12T10:00:05Z
)
```

**Result:**
- Customer balance: 100 → 90
- Payment record created with status `DEDUCTED`
- Downstream services (inventory, shipping) react to `PaymentDeducted` event

---

### Scenario 2: Customer Not Found - Error Recovery

**Input:** `OrderEvent.OrderCreated` with `customerId: 999` (non-existent)

**Processing Steps:**
1. Service Layer: `custRepo.findById(999)` → `Mono.empty()`
2. `.switchIfEmpty(CUSTOMER_NOT_FOUND)` → `Mono.error(CustomerNotFoundException)`
3. Error propagates, skipping `.map()` and `.doOnNext()`
4. **Exception Handler:** `transform(exceptionHandler(event))`
   - Tier 1: `onErrorResume(EventAlreadyProcessedException)` → no match
   - Tier 3 (Catch-All): `onErrorResume(EventDTOMapper.toPaymentFailedEvent(evt))`
5. **Error → Event Transformation:** Closure captures original event context
6. Creates `PaymentEvent.PaymentFailed(orderId, customerId, amount, message="Customer not found")`

**Output:** `PaymentEvent.PaymentFailed`
```java
PaymentFailed(
  orderId: def-456,
  customerId: 999,
  amount: 50,
  message: "Customer not found",
  createdAt: 2025-12-12T10:00:05Z
)
```

**Result:**
- No database changes
- Error recovered (no exception thrown to Kafka)
- Order service reacts to `PaymentFailed` → marks order as `CANCELLED`

---

### Scenario 3: Insufficient Balance - Business Rule Violation

**Input:** `OrderEvent.OrderCreated` with `amount: 150`, customer balance: 100

**Processing Steps:**
1. Service Layer: `filter(cust -> 100 >= 150)` → false
2. `.switchIfEmpty(INSUFFICIENT_BALANCE)` → `Mono.error(InsufficientBalanceException)`
3. Exception handler transforms to `PaymentEvent.PaymentFailed(message="Customer does not have sufficient balance")`

**Result:**
- No database changes
- Payment declined gracefully
- Saga compensation not needed (no deduction occurred)

---

### Scenario 4: Duplicate Event - Idempotency Protection

**Input:** Same `OrderEvent.OrderCreated` received twice (Kafka at-least-once delivery)

**Processing Steps:**
1. **First Processing:** Payment deducted successfully, `customer_payment` record created
2. **Second Processing (Duplicate):**
   - Phase 1: `pymtRepo.existsByOrderId(abc-123)` → **true** (duplicate detected)
   - `DuplicateEventValidator.validate()` → `Mono.error(EventAlreadyProcessedException)`
   - Exception handler Tier 1: `onErrorResume(EventAlreadyProcessedException, e -> Mono.empty())`
   - Returns `Mono.empty()` (silent swallow, no event emitted)

**Result:**
- No database changes (idempotent)
- No duplicate `PaymentDeducted` event published
- Kafka offset still acknowledged (message consumed successfully)

---

### Scenario 5: Order Cancelled - Refund (Compensating Transaction)

**Input:** `OrderEvent.OrderCancelled`
```java
OrderCancelled(
  orderId: abc-123,
  message: "Out of stock",
  createdAt: 2025-12-12T10:05:00Z
)
```

**Processing Steps:**
1. **Route Event:** `OrderEventProcessorImpl.handle(OrderCancelled)`
2. **Service Layer:** `PaymentServiceImpl.processRefund(orderId)` executes 4-phase pipeline:
   - **Phase 1 (Find Payment):** `pymtRepo.findByOrderIdAndStatus(abc-123, DEDUCTED)` → finds payment
   - **Phase 2 (Zip with Customer):** `.zipWhen(custPymt -> custRepo.findById(customerId))` → `Tuple2<CustomerPayment, Customer>`
   - **Phase 3 (Refund Payment):** 
     - Update customer balance: `90 + 10 = 100` (in-memory)
     - Set payment status: `REFUNDED`
     - `custRepo.save(customer)` → UPDATE customer SET balance=100
     - `.then(pymtRepo.save(payment))` → UPDATE customer_payment SET status='REFUNDED'
   - **Phase 4 (Logging):** `doOnNext(log.info("Refunded amount..."))`
3. **Map to Event:** `EventDTOMapper.toPaymentRefundedEvent()` → `PaymentEvent.PaymentRefunded`

**Output:** `PaymentEvent.PaymentRefunded`
```java
PaymentRefunded(
  orderId: abc-123,
  paymentId: xyz-789,
  customerId: 1,
  amount: 10,
  createdAt: 2025-12-12T10:05:05Z
)
```

**Result:**
- Customer balance: 90 → 100 (restored)
- Payment status: DEDUCTED → REFUNDED
- Compensating transaction completed
- Order service marks order as `CANCELLED`

---

### Scenario 6: Refund Without Deduction - No-Op

**Input:** `OrderEvent.OrderCancelled` with `orderId: xyz-999` (no prior payment)

**Processing Steps:**
1. Service Layer: `pymtRepo.findByOrderIdAndStatus(xyz-999, DEDUCTED)` → `Mono.empty()`
2. No downstream processing (reactive chain completes empty)
3. No event emitted

**Result:**
- No database changes
- No `PaymentRefunded` event (nothing to refund)
- Silent completion (idiomatic reactive behavior)

---

## Layered Architecture (Separation of Concerns)

### Layer 1: Messaging Layer (Kafka Integration)
**Package:** `messaging` (config, processor, mapper)

**Responsibilities:**
- Consume events from Kafka topics
- Handle Kafka-specific concerns (acknowledgment, offset management, partitioning)
- Implement `OrderEventProcessor<PaymentEvent>` interface
- Map domain events to DTOs (messaging ↔ service boundary)
- Delegate business logic to Service Layer
- Transform exceptions to domain events (error recovery)
- Publish outbound events with correlation keys

**Key Classes:**

#### `OrderEventProcessorConfig` (@Configuration)
Spring Cloud Stream integration bridge connecting Kafka to domain event processing.

```java
@Configuration
@RequiredArgsConstructor
public class OrderEventProcessorConfig {
    private final OrderEventProcessor<PaymentEvent> evtProcessor;
    
    @Bean 
    public Function<Flux<Message<OrderEvent>>, Flux<Message<PaymentEvent>>> processor(){
        return flux -> flux
            .map(MessageConverter::toRecord) // Extract payload, key, ack
            .doOnNext(r -> log.info("customer payment received {}", r.message()))
            .concatMap(r -> this.evtProcessor.process(r.message())
                .doOnSuccess(evt -> r.acknowledgement().acknowledge()) // Manual ack
            )
            .map(this::toMessage); // Add Kafka key for partitioning
    }
    
    private Message<PaymentEvent> toMessage(PaymentEvent evt){
        return MessageBuilder.withPayload(evt)
            .setHeader(KafkaHeaders.KEY, evt.orderId().toString()) // Partition by orderId
            .build();
    }
}
```

**Key Concepts:**
- **`concatMap()`**: Sequential processing (preserves order per partition)
- **Manual Acknowledgment**: `doOnSuccess(acknowledge())` commits offset only after successful processing
- **Kafka Partitioning**: `KafkaHeaders.KEY = orderId` ensures all events for same order go to same partition

---

#### `OrderEventProcessorImpl` (@Service)
Implements `OrderEventProcessor<PaymentEvent>` for type-safe event routing.

```java
@Service
@RequiredArgsConstructor
public class OrderEventProcessorImpl implements OrderEventProcessor<PaymentEvent> {
    private final PaymentService service;
    
    @Override
    public Mono<PaymentEvent> handle(OrderEvent.OrderCreated event) {
        return this.service.processPayment(EventDTOMapper.toPaymentProcessRequest(event))
            .map(EventDTOMapper::toPaymentDeductedEvent)
            .doOnNext(evt -> log.info("payment processed {}", evt))
            .transform(exceptionHandler(event)); // Error recovery
    }
    
    @Override
    public Mono<PaymentEvent> handle(OrderEvent.OrderCancelled event) {
        return this.service.processRefund(event.orderId())
            .map(EventDTOMapper::toPaymentRefundedEvent)
            .doOnNext(evt -> log.info("payment refunded {}", evt))
            .doOnError(ex -> log.error("error refunding payment", ex));
    }
    
    @Override
    public Mono<PaymentEvent> handle(OrderEvent.OrderCompleted event) {
        return Mono.empty(); // Terminal event, no action needed
    }
    
    // Three-tier exception handling strategy
    private UnaryOperator<Mono<PaymentEvent>> exceptionHandler(OrderEvent.OrderCreated evt){
        return mono -> mono
            .onErrorResume(EventAlreadyProcessedException.class, e -> Mono.empty()) // Tier 1: Idempotency
            .onErrorResume(EventDTOMapper.toPaymentFailedEvent(evt)); // Tier 3: Catch-all
    }
}
```

**Exception Handling Strategy:**
- **Tier 1 (Idempotency)**: `EventAlreadyProcessedException` → `Mono.empty()` (silent swallow)
- **Tier 2 (Specific - Optional)**: `CustomerNotFoundException`, `InsufficientBalanceException` → `PaymentFailed`
- **Tier 3 (Catch-All)**: Any remaining exception → `PaymentFailed` with error message

---

#### `EventDTOMapper` (Utility)
Static mapper for event ↔ DTO transformations.

```java
public class EventDTOMapper {
    // Event → DTO (inbound)
    public static PaymentProcessRequest toPaymentProcessRequest(OrderEvent.OrderCreated evt){
        return PaymentProcessRequest.builder()
            .customerId(evt.customerId())
            .orderId(evt.orderId())
            .amount(evt.totalAmount())
            .build();
    }
    
    // DTO → Event (outbound success)
    public static PaymentEvent toPaymentDeductedEvent(PaymentDTO dto){
        return PaymentEvent.PaymentDeducted.builder()
            .orderId(dto.orderId())
            .paymentId(dto.paymentId())
            .customerId(dto.customerId())
            .amount(dto.amount())
            .createdAt(Instant.now())
            .build();
    }
    
    // DTO → Event (outbound refund)
    public static PaymentEvent toPaymentRefundedEvent(PaymentDTO dto){
        return PaymentEvent.PaymentRefunded.builder()
            .orderId(dto.orderId())
            .paymentId(dto.paymentId())
            .customerId(dto.customerId())
            .amount(dto.amount())
            .createdAt(Instant.now())
            .build();
    }
    
    // Exception → Event (error recovery with closure)
    public static Function<Throwable, Mono<PaymentEvent>> toPaymentFailedEvent(OrderEvent.OrderCreated evt){
        return ex -> Mono.fromSupplier(() -> 
            PaymentEvent.PaymentFailed.builder()
                .orderId(evt.orderId())
                .customerId(evt.customerId())
                .amount(evt.totalAmount())
                .message(ex.getMessage())
                .createdAt(Instant.now())
                .build()
        );
    }
}
```

**Key Pattern:** Closure captures original event context for error recovery

---

**What Messaging Layer DOES:**
- ✅ Kafka message consumption/production
- ✅ Manual offset acknowledgment
- ✅ Event routing via pattern matching
- ✅ DTO transformations (event ↔ service boundary)
- ✅ Exception → Event transformation (saga error recovery)
- ✅ Logging and observability

**What it DOES NOT do:**
- ❌ Direct database access
- ❌ Business logic (balance validation, calculations)
- ❌ Entity manipulation
- ❌ Transaction management

---

### Layer 2: Service Layer (Business Logic)
**Package:** `application.service`, `common.service` (interface)

**Responsibilities:**
- Business logic and validation
- Transaction management (`@Transactional`)
- Entity manipulation (mutable domain objects)
- Repository orchestration
- DTO ↔ Entity conversion
- Idempotency enforcement
- Reactive pipeline composition

**Key Classes:**

#### `PaymentService` (Interface)
Defines service contracts.

```java
public interface PaymentService {
    Mono<PaymentDTO> processPayment(PaymentProcessRequest request);
    Mono<PaymentDTO> processRefund(UUID orderId);
}
```

---

#### `PaymentServiceImpl` (@Service)
Core business logic implementation.

```java
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private static final Mono<Customer> CUSTOMER_NOT_FOUND = Mono.error(new CustomerNotFoundException());
    private static final Mono<Customer> INSUFFICIENT_BALANCE = Mono.error(new InsufficientBalanceException());
    
    private final CustomerRepository custRepo;
    private final PaymentRepository pymtRepo;
    
    @Override
    @Transactional
    public Mono<PaymentDTO> processPayment(PaymentProcessRequest reqDTO) {
        return DuplicateEventValidator.validate(
            // Phase 1: Idempotency check
            this.pymtRepo.existsByOrderId(reqDTO.orderId()), 
            
            // Phase 2: Customer validation
            this.custRepo.findById(reqDTO.customerId())
        )
        .switchIfEmpty(CUSTOMER_NOT_FOUND)
        
        // Phase 3: Balance validation
        .filter(cust -> cust.getBalance() >= reqDTO.amount())
        .switchIfEmpty(INSUFFICIENT_BALANCE)
        
        // Phase 4: Payment deduction
        .flatMap(cust -> this.deductPayment(cust, reqDTO))
        
        // Phase 5: Logging
        .doOnNext(pymtDTO -> log.info("Payment deducted successfully for orderId: {}", pymtDTO.orderId()));
    }
    
    private Mono<PaymentDTO> deductPayment(Customer customer, PaymentProcessRequest reqDTO) {
        var custPymt = EntityDTOMapper.toCustomerPayment(reqDTO);
        customer.setBalance(customer.getBalance() - reqDTO.amount());
        custPymt.setStatus(PaymentStatus.DEDUCTED);
        
        return this.custRepo.save(customer)
            .then(this.pymtRepo.save(custPymt)) // Sequential execution
            .map(EntityDTOMapper::toPaymentDTO);
    }
    
    @Override
    @Transactional
    public Mono<PaymentDTO> processRefund(UUID orderId) {
        // Phase 1: Find payment (only DEDUCTED can be refunded)
        return this.pymtRepo.findByOrderIdAndStatus(orderId, PaymentStatus.DEDUCTED)
            
            // Phase 2: Zip with customer (reactive join)
            .zipWhen(custPymt -> this.custRepo.findById(custPymt.getCustomerId()))
            
            // Phase 3: Refund payment
            .flatMap(tup -> this.refundPayment(tup.getT1(), tup.getT2()))
            
            // Phase 4: Logging
            .doOnNext(pymtDTO -> log.info("Refunded amount of {} for orderId: {}", pymtDTO.amount(), pymtDTO.orderId()));
    }
    
    private Mono<PaymentDTO> refundPayment(CustomerPayment custPymt, Customer cust){
        cust.setBalance(cust.getBalance() + custPymt.getAmount());
        custPymt.setStatus(PaymentStatus.REFUNDED);
        
        return this.custRepo.save(cust)
            .then(this.pymtRepo.save(custPymt)) // Sequential execution
            .map(EntityDTOMapper::toPaymentDTO);
    }
}
```

**Key Reactive Patterns:**
- **`DuplicateEventValidator.validate()`**: Idempotency via `.transform(emitDuplicateError()).then()`
- **`.zipWhen()`**: Reactive join for coordinated access to multiple entities (avoids nested flatMap)
- **`.then()`**: Sequential execution (customer save → payment save)
- **Static Error Monos**: Performance optimization (avoid repeated object creation)

---

#### `EntityDTOMapper` (Utility)
Static mapper for entity ↔ DTO transformations.

```java
public class EntityDTOMapper {
    // DTO → Entity (for creation)
    public static CustomerPayment toCustomerPayment(PaymentProcessRequest request){
        return CustomerPayment.builder()
            .customerId(request.customerId())
            .orderId(request.orderId())
            .amount(request.amount())
            .build();
    }
    
    // Entity → DTO (for service response)
    public static PaymentDTO toPaymentDTO(CustomerPayment payment){
        return PaymentDTO.builder()
            .paymentId(payment.getPaymentId())
            .orderId(payment.getOrderId())
            .customerId(payment.getCustomerId())
            .amount(payment.getAmount())
            .status(payment.getStatus())
            .build();
    }
}
```

---

**What Service Layer DOES:**
- ✅ Business logic (balance validation, payment calculations)
- ✅ Transaction management (`@Transactional`)
- ✅ Entity manipulation (mutable domain objects)
- ✅ Repository orchestration (save, query)
- ✅ Idempotency enforcement (`DuplicateEventValidator`)
- ✅ Reactive pipeline composition (filter, flatMap, zipWhen, then)
- ✅ Domain exception throwing (`CustomerNotFoundException`, `InsufficientBalanceException`)

**What it DOES NOT do:**
- ❌ Kafka event handling
- ❌ Message acknowledgment
- ❌ Event serialization/deserialization
- ❌ Exception → Event transformation (handled by messaging layer)

---

### Layer 3: Repository Layer (Data Access)
**Package:** `application.repository`

**Responsibilities:**
- Database operations (CRUD)
- Custom query methods
- R2DBC reactive database access

**Key Interfaces:**

```java
public interface CustomerRepository extends ReactiveCrudRepository<Customer, Integer> {
    // Inherited: findById, save, delete, etc.
}

public interface PaymentRepository extends ReactiveCrudRepository<CustomerPayment, UUID> {
    // Custom query for idempotency check
    Mono<Boolean> existsByOrderId(UUID orderId);
    
    // Custom query for refund lookup (only DEDUCTED payments can be refunded)
    Mono<CustomerPayment> findByOrderIdAndStatus(UUID orderId, PaymentStatus status);
}
```

**Key Concepts:**
- **Reactive CRUD**: All operations return `Mono<T>` or `Flux<T>`
- **Custom Queries**: Spring Data R2DBC derives queries from method names
- **Idempotency**: `existsByOrderId()` prevents duplicate payment processing

---

### Layer 4: Entity Layer (Domain Model)
**Package:** `application.entity`

**Responsibilities:**
- Database table mapping
- Mutable domain objects (only within service layer)
- R2DBC annotations (`@Table`, `@Id`)

**Key Classes:**

```java
@Data
@Table("customer")
public class Customer {
    @Id
    private Integer id;
    private String name;
    private Integer balance;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("customer_payment")
public class CustomerPayment {
    @Id
    private UUID paymentId;
    private UUID orderId;
    private Integer customerId;
    private PaymentStatus status;
    private Integer amount;
}
```

**Key Concepts:**
- **Lombok `@Data`**: Generates getters/setters (entities are mutable)
- **Lombok `@Builder`**: Enables fluent entity creation
- **Scope**: Entities **never leave service layer** (prevents external mutation)
- **R2DBC**: Reactive database mapping (non-blocking I/O)

---

### Layer 5: DTO Layer (Data Transfer Objects)
**Package:** `common.dto`

**Responsibilities:**
- Immutable data carriers between layers
- Decoupling messaging layer from service layer
- Type-safe communication contracts
- Boundary immutability (records)

**Key Classes:**

```java
@Builder
public record PaymentProcessRequest(
    Integer customerId, 
    UUID orderId, 
    Integer amount
) {}

@Builder
public record PaymentDTO(
    UUID paymentId, 
    UUID orderId, 
    Integer customerId, 
    Integer amount,
    PaymentStatus status
) {}
```

**Key Concepts:**
- **Java Records**: Immutable by default (final fields, no setters)
- **Lombok `@Builder`**: Enables fluent DTO construction
- **Boundary Objects**: DTOs cross layer boundaries, entities stay in service layer
- **Decoupling**: Messaging layer doesn't know about entities, service layer doesn't know about events

---

## Communication Flow

### End-to-End Message Flow (OrderCreated → PaymentDeducted)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     Kafka Topic: order-events                           │
│                  (Produced by order-service)                            │
└─────────────────────────────────────────────────────────────────────────┘
                            │
                            │ Message<OrderEvent.OrderCreated>
                            │ (key=orderId, payload=OrderCreated)
                            ↓
┌─────────────────────────────────────────────────────────────────────────┐
│              MESSAGING LAYER (Kafka Integration)                        │
├─────────────────────────────────────────────────────────────────────────┤
│  OrderEventProcessorConfig.processor()                                  │
│    ├─ MessageConverter.toRecord()                                       │
│    │   → CustomRecord<OrderEvent> (key, payload, acknowledgment)        │
│    ├─ doOnNext(log.info(...))                                           │
│    ├─ concatMap(OrderEventProcessorImpl.process())                      │
│    │   ├─ Pattern matching switch (OrderCreated/Cancelled/Completed)    │
│    │   └─ handle(OrderEvent.OrderCreated)                               │
│    │       ├─ EventDTOMapper.toPaymentProcessRequest()                  │
│    │       │   → PaymentProcessRequest (DTO)                            │
│    │       ├─ service.processPayment(PaymentProcessRequest)             │
│    │       │   ↓ (delegates to service layer)                           │
│    │       ├─ .map(EventDTOMapper::toPaymentDeductedEvent)              │
│    │       │   → PaymentEvent.PaymentDeducted                           │
│    │       └─ .transform(exceptionHandler())                            │
│    │           → Error recovery (Exception → PaymentFailed)             │
│    ├─ doOnSuccess(acknowledgment.acknowledge())                         │
│    │   → Manual Kafka offset commit                                     │
│    └─ toMessage(PaymentEvent)                                           │
│        → MessageBuilder.setHeader(KafkaHeaders.KEY, orderId)            │
└─────────────────────────────────────────────────────────────────────────┘
                            │
                            │ PaymentProcessRequest (DTO)
                            ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                   SERVICE LAYER (Business Logic)                        │
├─────────────────────────────────────────────────────────────────────────┤
│  PaymentServiceImpl.processPayment(PaymentProcessRequest)               │
│    ├─ Phase 1: DuplicateEventValidator.validate()                       │
│    │   ├─ pymtRepo.existsByOrderId(orderId)                             │
│    │   │   → Mono<Boolean> (true = duplicate, false = new)              │
│    │   └─ If duplicate → Mono.error(EventAlreadyProcessedException)     │
│    ├─ Phase 2: custRepo.findById(customerId)                            │
│    │   └─ .switchIfEmpty(CUSTOMER_NOT_FOUND)                            │
│    ├─ Phase 3: .filter(balance >= amount)                               │
│    │   └─ .switchIfEmpty(INSUFFICIENT_BALANCE)                          │
│    ├─ Phase 4: .flatMap(deductPayment())                                │
│    │   ├─ EntityDTOMapper.toCustomerPayment(request)                    │
│    │   │   → CustomerPayment entity                                     │
│    │   ├─ customer.setBalance(balance - amount)                         │
│    │   ├─ payment.setStatus(DEDUCTED)                                   │
│    │   ├─ custRepo.save(customer)                                       │
│    │   ├─ .then(pymtRepo.save(payment))                                 │
│    │   └─ .map(EntityDTOMapper::toPaymentDTO)                           │
│    │       → PaymentDTO                                                 │
│    └─ Phase 5: doOnNext(log.info(...))                                  │
└─────────────────────────────────────────────────────────────────────────┘
                            │
                            │ Entity operations (Customer, CustomerPayment)
                            ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                 REPOSITORY LAYER (Data Access)                          │
├─────────────────────────────────────────────────────────────────────────┤
│  CustomerRepository (ReactiveCrudRepository)                            │
│    ├─ findById(customerId) → Mono<Customer>                             │
│    └─ save(customer) → Mono<Customer>                                   │
│                                                                          │
│  PaymentRepository (ReactiveCrudRepository)                             │
│    ├─ existsByOrderId(orderId) → Mono<Boolean>                          │
│    ├─ save(payment) → Mono<CustomerPayment>                             │
│    └─ findByOrderIdAndStatus(orderId, status) → Mono<CustomerPayment>   │
└─────────────────────────────────────────────────────────────────────────┘
                            │
                            │ R2DBC SQL queries (non-blocking)
                            ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                    H2 DATABASE (Embedded)                               │
├─────────────────────────────────────────────────────────────────────────┤
│  customer table                                                         │
│    ├─ id (Integer, PK)                                                  │
│    ├─ name (String)                                                     │
│    └─ balance (Integer)                                                 │
│                                                                          │
│  customer_payment table                                                 │
│    ├─ payment_id (UUID, PK, auto-generated)                             │
│    ├─ order_id (UUID, FK to order)                                      │
│    ├─ customer_id (Integer, FK to customer)                             │
│    ├─ status (Enum: DEDUCTED, REFUNDED, FAILED)                         │
│    └─ amount (Integer)                                                  │
└─────────────────────────────────────────────────────────────────────────┘
                            │
                            │ (Return path: Entity → DTO → Event)
                            ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                     Kafka Topic: payment-events                         │
│                  (Consumed by inventory-service, order-service)         │
│                                                                          │
│  Message<PaymentEvent.PaymentDeducted>                                  │
│    ├─ key: orderId (for partitioning)                                   │
│    └─ payload: PaymentDeducted(orderId, paymentId, amount, ...)         │
└─────────────────────────────────────────────────────────────────────────┘
```

**Key Flow Characteristics:**
- **Reactive**: Non-blocking from Kafka → Database → Kafka
- **Sequential**: `concatMap()` preserves order per partition
- **Idempotent**: `DuplicateEventValidator` prevents duplicate processing
- **Error Recovery**: Exceptions → `PaymentFailed` events (no crashes)
- **Manual Ack**: Offset committed only after successful processing
- **Partitioned**: `orderId` as Kafka key ensures all events for same order go to same partition

---

## Testing Strategy

### Integration Testing with Embedded Kafka

**Test Class:** `PaymentServiceTest`

**Key Components:**
- **`@EmbeddedKafka`**: Embedded Kafka broker for integration testing
- **`@SpringBootTest`**: Full Spring context with all beans
- **`@TestPropertySource`**: Configures test-specific Spring Cloud Stream functions
- **`Sinks.Many`**: Controlled message emission and capture
- **`StepVerifier`**: Reactive stream testing with assertions

---

### Test Infrastructure

```java
@TestPropertySource(properties = {
    "spring.cloud.function.definition=processor;orderEvtProducer;paymentEvtConsumer",
    "spring.cloud.stream.bindings.orderEvtProducer-out-0.destination=order-events",
    "spring.cloud.stream.bindings.paymentEvtConsumer-in-0.destination=payment-events"
})
public class PaymentServiceTest extends AbstractIntegrationTest {
    
    // Input sink: Emit OrderEvent messages
    private static final Sinks.Many<OrderEvent> reqSink = 
        Sinks.many().unicast().onBackpressureBuffer();
    
    // Output sink: Capture PaymentEvent messages
    private static final Sinks.Many<PaymentEvent> respSink = 
        Sinks.many().unicast().onBackpressureBuffer();
    
    // Cached response flux for multiple test assertions
    private static final Flux<PaymentEvent> respFlux = respSink.asFlux().cache(0);
    
    @Autowired
    private CustomerRepository custRepo;
    
    @TestConfiguration
    static class TestConfig {
        @Bean
        public Supplier<Flux<OrderEvent>> orderEvtProducer(){
            return reqSink::asFlux; // Converts reqSink to Kafka producer
        }
        
        @Bean
        public Consumer<Flux<PaymentEvent>> paymentEvtConsumer(){
            return f -> f.doOnNext(respSink::tryEmitNext).subscribe();
        }
    }
}
```

**Message Flow in Tests:**
```
reqSink (test)
  → orderEvtProducer (TestConfig)
  → order-events topic (Embedded Kafka)
  → processor (OrderEventProcessorConfig) ← SYSTEM UNDER TEST
  → payment-events topic
  → paymentEvtConsumer (TestConfig)
  → respSink (test)
```

---

### Test Scenarios

#### 1. Deduct and Refund Test (Happy Path + Compensation)
```java
@Test
public void deductAndRefundTest(){
    // Step 1: Deduct payment
    var orderCreatedEvt = TestDataUtil.createOrderCreatedEvent(1, 1, 2, 3);
    expectEvent(orderCreatedEvt, PaymentEvent.PaymentDeducted.class, e -> {
        Assertions.assertNotNull(e.paymentId());
        Assertions.assertEquals(orderCreatedEvt.orderId(), e.orderId());
        Assertions.assertEquals(6, e.amount());
    });
    
    // Step 2: Verify balance deducted
    custRepo.findById(1)
        .as(StepVerifier::create)
        .consumeNextWith(cust -> Assertions.assertEquals(94, cust.getBalance()))
        .verifyComplete();
    
    // Step 3: Check duplicate event (idempotency)
    expectNoEvent(orderCreatedEvt); // Same event, no processing
    
    // Step 4: Cancel order and refund
    var ordCancEvt = TestDataUtil.createOrderCancelledEvent(orderCreatedEvt.orderId());
    expectEvent(ordCancEvt, PaymentEvent.PaymentRefunded.class, e -> {
        Assertions.assertNotNull(e.paymentId());
        Assertions.assertEquals(ordCancEvt.orderId(), e.orderId());
        Assertions.assertEquals(6, e.amount());
    });
    
    // Step 5: Verify balance restored
    custRepo.findById(1)
        .as(StepVerifier::create)
        .consumeNextWith(cust -> Assertions.assertEquals(100, cust.getBalance()))
        .verifyComplete();
}
```

#### 2. Refund Without Deduction (No-Op)
```java
@Test
public void refundWithoutDeductTest(){
    var ordCancEvt = TestDataUtil.createOrderCancelledEvent(UUID.randomUUID());
    expectNoEvent(ordCancEvt); // No prior payment, no refund
}
```

#### 3. Customer Not Found (Error Recovery)
```java
@Test
public void customerNotFoundTest(){
    var orderCreatedEvt = TestDataUtil.createOrderCreatedEvent(19, 1, 2, 3);
    expectEvent(orderCreatedEvt, PaymentEvent.PaymentFailed.class, e -> {
        Assertions.assertEquals(orderCreatedEvt.orderId(), e.orderId());
        Assertions.assertEquals(6, e.amount());
        Assertions.assertEquals("Customer not found", e.message());
    });
}
```

#### 4. Insufficient Balance (Business Rule Violation)
```java
@Test
public void insufficientBalanceTest(){
    var orderCreatedEvt = TestDataUtil.createOrderCreatedEvent(1, 1, 50, 3);
    expectEvent(orderCreatedEvt, PaymentEvent.PaymentFailed.class, e -> {
        Assertions.assertEquals(orderCreatedEvt.orderId(), e.orderId());
        Assertions.assertEquals(150, e.amount());
        Assertions.assertEquals("Customer does not have sufficient balance", e.message());
    });
}
```

---

### Test Utilities

```java
// Generic event expectation with type-safe assertions
private <T> void expectEvent(OrderEvent evt, Class<T> type, Consumer<T> assertion){
    respFlux
        .doFirst(() -> reqSink.tryEmitNext(evt))
        .next()
        .timeout(Duration.ofSeconds(2), Mono.empty())
        .cast(type)
        .as(StepVerifier::create)
        .consumeNextWith(assertion)
        .verifyComplete();
}

// Expect no event (idempotency, no-op scenarios)
private void expectNoEvent(OrderEvent evt){
    respFlux
        .doFirst(() -> reqSink.tryEmitNext(evt))
        .next()
        .timeout(Duration.ofSeconds(2), Mono.empty())
        .as(StepVerifier::create)
        .verifyComplete();
}
```

**Key Testing Patterns:**
- **Controlled Emission**: `reqSink.tryEmitNext()` emits events on-demand
- **Reactive Assertions**: `StepVerifier` validates reactive streams
- **Timeout Handling**: `.timeout(2s, Mono.empty())` prevents hanging tests
- **Type-Safe Casting**: `.cast(PaymentDeducted.class)` for specific event assertions
- **Database Verification**: Direct repository queries validate state changes
- **Idempotency Testing**: Same event emitted twice, processed once

---

## Key Design Principles

### 1. **Separation of Concerns**
- **Messaging Layer**: Kafka integration, event routing, error recovery
- **Service Layer**: Business logic, validation, transaction management
- **Repository Layer**: Database access, query methods
- **Entity Layer**: Mutable domain model (service-scoped)
- **DTO Layer**: Immutable boundary objects

**No cross-cutting concerns:** Kafka logic doesn't leak into service layer, business logic doesn't know about events

---

### 2. **Immutability at Boundaries**
- **DTOs (records)**: Immutable by default (final fields, no setters)
- **Entities**: Mutable but confined to service layer
- **Events**: Immutable (sealed interface with records)

**Pattern:** Mutable entities for internal processing, immutable DTOs/events for external communication

---

### 3. **Dependency Direction**
```
Messaging Layer → Service Layer → Repository Layer → Database
  (Event/DTO)       (DTO/Entity)      (Entity)
```
- Messaging layer depends on Service layer (not vice versa)
- Service layer doesn't know about Kafka or events
- Repository layer doesn't know about DTOs or events
- **Dependency Inversion:** Service layer depends on abstractions (`PaymentService` interface)

---

### 4. **Encapsulation**
- **Entities never leave service layer** (prevents external mutation)
- **Events never enter service layer** (domain logic is event-agnostic)
- **DTOs bridge the layers** (clean contracts)

**Example:**
```
OrderEvent (messaging) → PaymentProcessRequest (DTO) → Customer (entity) → PaymentDTO (DTO) → PaymentEvent (messaging)
```

---

### 5. **Reactive Programming**
- **Non-blocking I/O**: All operations return `Mono<T>` or `Flux<T>`
- **Backpressure**: Reactive streams handle flow control
- **Composition**: Operators like `flatMap`, `zipWhen`, `then` build pipelines
- **Error Handling**: `onErrorResume` for graceful degradation

**Benefits:** Scalability, resource efficiency, responsive systems

---

### 6. **Idempotency**
- **Duplicate Detection**: `DuplicateEventValidator` checks `existsByOrderId()`
- **At-Least-Once Delivery**: Kafka guarantees message delivery, service ensures exactly-once processing
- **Pattern:** Validation publisher → Processing publisher (short-circuits on duplicate)

---

### 7. **Error Recovery (Saga Pattern)**
- **No Exceptions to Kafka**: Exceptions transformed to `PaymentFailed` events
- **Three-Tier Handler**:
  1. Idempotency: `EventAlreadyProcessedException` → `Mono.empty()`
  2. Specific: `CustomerNotFoundException` → `PaymentFailed`
  3. Catch-All: Any exception → `PaymentFailed`
- **Compensating Transactions**: `OrderCancelled` → `processRefund()` restores balance

---

### 8. **Type Safety**
- **Sealed Interfaces**: Exhaustive pattern matching (compiler-enforced)
- **Records**: Immutable DTOs with compile-time guarantees
- **Generic Interfaces**: `EventProcessor<T, R>` for type-safe event processing

**Example:**
```java
switch(event) {
    case OrderEvent.OrderCreated e -> handle(e);
    case OrderEvent.OrderCancelled e -> handle(e);
    case OrderEvent.OrderCompleted e -> handle(e);
    // Compiler error if any case is missing
}
```

---

### 9. **Testability**
- **Service Layer**: Unit testable without Kafka (mock repositories)
- **Messaging Layer**: Integration tests with `@EmbeddedKafka`
- **Reactive Testing**: `StepVerifier` for stream assertions
- **Controlled Emission**: `Sinks.Many` for test data injection
- **Isolation**: `@DirtiesContext` prevents test interference

---

## Benefits of This Architecture

### ✅ **Maintainability**
- **Clear Boundaries**: Each layer has single responsibility
- **Loose Coupling**: Kafka changes don't affect business logic
- **Schema Independence**: Database changes don't affect event handling
- **Readable Code**: Pattern matching, sealed interfaces, records

**Example:** Switching from Kafka to RabbitMQ only requires changes in messaging layer

---

### ✅ **Testability**
- **Service Layer**: Unit testable without Kafka (mock repositories)
- **Messaging Layer**: Integration tests with `@EmbeddedKafka`
- **Reactive Testing**: `StepVerifier` for stream assertions
- **Test Isolation**: `@DirtiesContext` prevents interference
- **Controlled Emission**: `Sinks.Many` for deterministic tests

**Example:** `PaymentServiceTest` validates full stack from Kafka to database

---

### ✅ **Flexibility**
- **Message Broker Agnostic**: Swap Kafka for RabbitMQ without changing service layer
- **Database Agnostic**: Change H2 → PostgreSQL without affecting messaging
- **Multi-Channel**: Add REST endpoints reusing same service layer
- **Event Evolution**: Sealed interfaces enable safe event schema changes

**Example:** Same `PaymentService` can serve Kafka consumers, REST controllers, gRPC handlers

---

### ✅ **Type Safety**
- **Compile-Time Contracts**: DTOs (records) prevent runtime errors
- **Exhaustive Pattern Matching**: Compiler ensures all event types handled
- **No Accidental Mutation**: Entities confined to service layer
- **Generic Interfaces**: `EventProcessor<T, R>` for type-safe processing

**Example:** Adding new `OrderEvent` type causes compilation error until `handle()` method implemented

---

### ✅ **Scalability**
- **Reactive Streams**: Non-blocking I/O for high throughput
- **Kafka Partitioning**: `orderId` as key ensures parallel processing
- **Independent Scaling**: Messaging, service, database layers scale separately
- **Microservice Extraction**: Clear boundaries enable service splitting

**Example:** Deploy multiple instances of payment service, Kafka distributes load via partitions

---

### ✅ **Reliability**
- **Idempotency**: `DuplicateEventValidator` prevents duplicate processing
- **Error Recovery**: Exceptions → `PaymentFailed` events (no crashes)
- **Compensating Transactions**: `processRefund()` for saga rollback
- **Manual Acknowledgment**: Offset committed only after successful processing
- **At-Least-Once Delivery**: Kafka guarantees + idempotency = exactly-once semantics

**Example:** Network failure during processing → message redelivered → idempotency check → skip duplicate

---

### ✅ **Observability**
- **Structured Logging**: `doOnNext()`, `doOnError()` for audit trail
- **Event Correlation**: `orderId` tracks saga across services
- **Reactive Tracing**: Operators provide clear execution flow
- **Database Audit**: `customer_payment` table records all transactions

**Example:** Trace payment flow via logs: "customer payment received" → "payment processed" → "payment refunded"

---

## Implementation Checklist

### Messaging Layer ✅
- [x] Create `OrderEventProcessorConfig` (Spring Cloud Stream bridge)
- [x] Implement `OrderEventProcessorImpl` (implements `OrderEventProcessor<PaymentEvent>`)
- [x] Handle `OrderCreated` → call `service.processPayment()`
- [x] Handle `OrderCancelled` → call `service.processRefund()`
- [x] Handle `OrderCompleted` → return `Mono.empty()`
- [x] Create `EventDTOMapper` (event ↔ DTO transformations)
- [x] Implement three-tier exception handler
- [x] Configure manual Kafka acknowledgment
- [x] Configure Kafka bindings in `application.yaml`

### Service Layer ✅
- [x] Create `PaymentService` interface
- [x] Implement `PaymentServiceImpl` with `@Transactional`
- [x] Implement `processPayment()` with 5-phase reactive pipeline
- [x] Implement `processRefund()` with 4-phase reactive pipeline
- [x] Add balance validation logic (`.filter()`, `.switchIfEmpty()`)
- [x] Add duplicate payment check (`DuplicateEventValidator`)
- [x] Create `EntityDTOMapper` (entity ↔ DTO transformations)
- [x] Implement `deductPayment()` private method
- [x] Implement `refundPayment()` private method

### Repository Layer ✅
- [x] Create `CustomerRepository extends ReactiveCrudRepository<Customer, Integer>`
- [x] Create `PaymentRepository extends ReactiveCrudRepository<CustomerPayment, UUID>`
- [x] Add custom query: `existsByOrderId(UUID orderId)` (idempotency)
- [x] Add custom query: `findByOrderIdAndStatus(UUID orderId, PaymentStatus status)` (refund)

### Entity Layer ✅
- [x] Create `Customer` entity with `@Table("customer")`
- [x] Create `CustomerPayment` entity with `@Table("customer_payment")`
- [x] Add Lombok `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`

### DTO Layer ✅
- [x] Create `PaymentProcessRequest` record with `@Builder`
- [x] Create `PaymentDTO` record with `@Builder`

### Data Initialization ✅
- [x] Create `data.sql` with schema and sample data
- [x] Pre-populate `customer` table (anya, becky, bondo with balance=100)
- [x] Create `CustomerPaymentApplication` entry point

### Testing ✅
- [x] Create `AbstractIntegrationTest` with `@EmbeddedKafka`
- [x] Create `TestDataUtil` for test data generation
- [x] Create `PaymentServiceTest` with integration tests
- [x] Test scenario: `deductAndRefundTest()` (happy path + compensation)
- [x] Test scenario: `refundWithoutDeductTest()` (no-op)
- [x] Test scenario: `customerNotFoundTest()` (error recovery)
- [x] Test scenario: `insufficientBalanceTest()` (business rule violation)
- [x] Implement `expectEvent()` utility (type-safe assertions)
- [x] Implement `expectNoEvent()` utility (idempotency testing)

### Common Module (choreo-common) ✅
- [x] Create sealed `OrderEvent` interface with inner records
- [x] Create sealed `PaymentEvent` interface with inner records
- [x] Create `OrderEventProcessor<R>` interface with pattern matching
- [x] Create `DuplicateEventValidator` utility
- [x] Create `EventAlreadyProcessedException`
- [x] Create `CustomRecord<T>` and `MessageConverter`
- [x] Create domain-specific exceptions (`CustomerNotFoundException`, `InsufficientBalanceException`)

### Documentation ✅
- [x] Create `ARCHITECTURE_NOTES.md` with comprehensive documentation
- [x] Add inline comments explaining reactive flows
- [x] Create PlantUML diagrams (class, sequence)
- [x] Document test strategies and patterns

---

## Summary

This architecture demonstrates **enterprise-grade event-driven microservices** implementing the **Saga Choreography Pattern** with production-ready patterns:

### Architecture Layers
- **Messaging Layer:** Kafka integration, event routing, error recovery
- **Service Layer:** Business logic, validation, transaction management
- **Repository Layer:** Reactive database access (R2DBC)
- **DTO Layer:** Immutable contracts between layers
- **Entity Layer:** Mutable domain model (service-scoped)

### Key Patterns Implemented
1. **Saga Choreography:** Distributed transaction management via events (no central orchestrator)
2. **Reactive Programming:** Non-blocking I/O with Project Reactor (`Mono`, `Flux`)
3. **Idempotency:** `DuplicateEventValidator` prevents duplicate processing
4. **Error Recovery:** Exceptions → `PaymentFailed` events (graceful degradation)
5. **Compensating Transactions:** `processRefund()` rolls back distributed operations
6. **Type Safety:** Sealed interfaces, pattern matching, records
7. **Manual Acknowledgment:** Kafka offset committed only after successful processing
8. **Partitioning:** `orderId` as Kafka key ensures event ordering per entity

### Key Insights
- **Events and Entities never meet directly**: DTOs act as translation layer
- **Immutability at boundaries, mutability within**: Records for DTOs/events, entities for internal processing
- **Reactive composition over imperative code**: Operators like `flatMap`, `zipWhen`, `then` build pipelines
- **Error signals are data**: Exceptions transformed to domain events for saga coordination
- **Test-driven reactive systems**: `StepVerifier`, `Sinks.Many`, `@EmbeddedKafka` for integration testing

### Production-Ready Features
- ✅ Idempotency (at-least-once → exactly-once semantics)
- ✅ Error recovery (no crashes, all errors become events)
- ✅ Compensating transactions (saga rollback)
- ✅ Manual acknowledgment (reliable message processing)
- ✅ Reactive streams (scalability, backpressure)
- ✅ Type safety (sealed interfaces, pattern matching)
- ✅ Comprehensive testing (unit, integration, reactive)
- ✅ Observability (structured logging, event correlation)

### Next Steps
- Implement `inventory-service` (similar pattern: `InventoryEventProcessor<InventoryEvent>`)
- Implement `shipping-service` (similar pattern: `ShippingEventProcessor<ShippingEvent>`)
- Implement `order-service` (saga coordinator: aggregates all events, manages order state)
- Add distributed tracing (Spring Cloud Sleuth + Zipkin)
- Add metrics (Micrometer + Prometheus)
- Deploy to Kubernetes with Kafka cluster

---

**This implementation serves as a reference architecture for building production-grade, event-driven microservices with saga pattern, reactive programming, and comprehensive error handling.**

