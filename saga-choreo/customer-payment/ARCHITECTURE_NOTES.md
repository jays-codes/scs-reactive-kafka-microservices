# Customer Payment Service - Architecture & Design Notes

## Database Schema

### Table 1: `customer`
| Column | Type | Description |
|--------|------|-------------|
| `customer_id` | UUID/Integer | Primary key |
| `name` | String | Customer name |
| `balance` | Decimal/Integer | Available balance (e.g., $100) |

**Purpose:** Stores customer account information and current balance

**Data Population:**
- Pre-populated on application startup (no REST endpoints for creation)
- Uses data initialization strategy (e.g., `data.sql`, `@PostConstruct`, or `ApplicationRunner`)

### Table 2: `customer_payment`
| Column | Type | Description |
|--------|------|-------------|
| `id` (payment_id) | UUID | Primary key |
| `order_id` | UUID | Foreign key to order |
| `customer_id` | UUID | Foreign key to customer |
| `status` | Enum | Payment status (DEDUCTED, REFUNDED, FAILED) |
| `amount` | Decimal/Integer | Payment amount |

**Purpose:** Audit trail of all payment transactions (deductions and refunds)

**Initial State:** Empty table (populated as events are processed)

---

## Business Logic Flow

### Scenario 1: Order Created - Payment Deduction

**Input:** `OrderCreated` event
```
{
  orderId: 1,
  customerId: 1 (Sam),
  productId: 100,
  amount: $10
}
```

**Processing Steps:**
1. Receive `OrderCreated` event from Kafka
2. Extract `customerId = 1` and `amount = $10`
3. Query `customer` table: Sam has `balance = $100`
4. Validate: `$100 >= $10` ✓ (sufficient balance)
5. **Update `customer` table:** `balance = $100 - $10 = $90`
6. **Insert into `customer_payment` table:**
   ```
   payment_id: <new UUID>
   order_id: 1
   customer_id: 1
   status: DEDUCTED
   amount: $10
   ```
7. Emit `PaymentDeducted` event to Kafka

**Result:**
- Customer balance: $100 → $90
- Payment record created with status `DEDUCTED`

---

### Scenario 2: Second Order - Another Deduction

**Input:** `OrderCreated` event
```
{
  orderId: 2,
  customerId: 1 (Sam),
  productId: 200,
  amount: $5
}
```

**Processing Steps:**
1. Query `customer` table: Sam has `balance = $90`
2. Validate: `$90 >= $5` ✓
3. **Update `customer` table:** `balance = $90 - $5 = $85`
4. **Insert into `customer_payment` table:**
   ```
   payment_id: <new UUID>
   order_id: 2
   customer_id: 1
   status: DEDUCTED
   amount: $5
   ```
5. Emit `PaymentDeducted` event

**Result:**
- Customer balance: $90 → $85
- Second payment record created

---

### Scenario 3: Order Cancelled - Payment Refund (Compensating Transaction)

**Input:** `OrderCancelled` event
```
{
  orderId: 2,
  message: "Out of stock"
}
```

**Processing Steps:**
1. Receive `OrderCancelled` event
2. Query `customer_payment` table: `WHERE order_id = 2`
3. Find existing payment record:
   ```
   payment_id: 82
   order_id: 2
   customer_id: 1
   status: DEDUCTED
   amount: $5
   ```
4. Validate: Payment exists and status is `DEDUCTED` ✓
5. **Update `customer` table:** `balance = $85 + $5 = $90` (refund)
6. **Insert new refund record in `customer_payment` table:**
   ```
   payment_id: <new UUID>
   order_id: 2
   customer_id: 1
   status: REFUNDED
   amount: $5
   ```
   OR **Update existing record:** `status = DEDUCTED → REFUNDED`
7. Emit `PaymentRefunded` event to Kafka

**Result:**
- Customer balance: $85 → $90 (restored)
- Payment status changed to `REFUNDED`
- Compensating transaction completed

---

## Layered Architecture (Separation of Concerns)

### Layer 1: Messaging Layer (Kafka Integration)
**Package:** `messaging` or `consumer`

**Responsibilities:**
- Consume events from Kafka topics
- Handle Kafka-specific concerns (acknowledgment, offset management, error handling)
- Implement `OrderEventProcessor<PaymentEvent>` interface
- Map domain events to DTOs
- Delegate business logic to Service Layer
- Publish outbound events via `EventPublisher`

**Key Classes:**
- `PaymentEventListener` - Spring Cloud Function consumer
- `PaymentEventProcessorImpl` - Implements `OrderEventProcessor<PaymentEvent>`

**What it DOES:**
```java
@Component
public class PaymentEventProcessorImpl implements OrderEventProcessor<PaymentEvent> {
    
    @Autowired
    private PaymentService paymentService;
    
    @Override
    public Mono<PaymentEvent> handle(OrderEvent.OrderCreated event) {
        // Map event to DTO
        PaymentRequestDto dto = toDto(event);
        
        // Delegate to service layer
        return paymentService.deductPayment(dto)
            .map(this::toPaymentDeductedEvent);
    }
    
    @Override
    public Mono<PaymentEvent> handle(OrderEvent.OrderCancelled event) {
        return paymentService.refundPayment(event.orderId())
            .map(this::toPaymentRefundedEvent);
    }
}
```

**What it DOES NOT do:**
- ❌ Direct database access
- ❌ Business logic (balance validation, calculations)
- ❌ Entity manipulation

---

### Layer 2: Service Layer (Business Logic)
**Package:** `service`

**Responsibilities:**
- Business logic and validation
- Transaction management
- Entity manipulation
- Repository orchestration
- DTO ↔ Entity conversion

**Key Classes:**
- `PaymentService` - Core business logic

**What it DOES:**
```java
@Service
public class PaymentService {
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    public Mono<PaymentResponseDto> deductPayment(PaymentRequestDto request) {
        return customerRepository.findById(request.customerId())
            // Validate sufficient balance
            .filter(customer -> customer.getBalance() >= request.amount())
            .switchIfEmpty(Mono.error(new InsufficientBalanceException()))
            // Deduct balance
            .flatMap(customer -> {
                customer.setBalance(customer.getBalance() - request.amount());
                return customerRepository.save(customer);
            })
            // Create payment record
            .flatMap(customer -> {
                Payment payment = Payment.builder()
                    .orderId(request.orderId())
                    .customerId(customer.getId())
                    .amount(request.amount())
                    .status(PaymentStatus.DEDUCTED)
                    .build();
                return paymentRepository.save(payment);
            })
            // Convert entity to DTO
            .map(this::toDto);
    }
    
    public Mono<PaymentResponseDto> refundPayment(UUID orderId) {
        return paymentRepository.findByOrderId(orderId)
            .filter(payment -> payment.getStatus() == PaymentStatus.DEDUCTED)
            .switchIfEmpty(Mono.error(new PaymentNotFoundException()))
            .flatMap(payment -> 
                customerRepository.findById(payment.getCustomerId())
                    .flatMap(customer -> {
                        // Refund balance
                        customer.setBalance(customer.getBalance() + payment.getAmount());
                        return customerRepository.save(customer)
                            .thenReturn(payment);
                    })
            )
            .flatMap(payment -> {
                payment.setStatus(PaymentStatus.REFUNDED);
                return paymentRepository.save(payment);
            })
            .map(this::toDto);
    }
}
```

**What it DOES NOT do:**
- ❌ Kafka event handling
- ❌ Message acknowledgment
- ❌ Event serialization/deserialization

---

### Layer 3: Repository Layer (Data Access)
**Package:** `repository`

**Responsibilities:**
- Database operations (CRUD)
- Query methods
- R2DBC reactive database access

**Key Interfaces:**
```java
public interface CustomerRepository extends ReactiveCrudRepository<Customer, UUID> {
    // Inherited: findById, save, delete, etc.
}

public interface PaymentRepository extends ReactiveCrudRepository<Payment, UUID> {
    Mono<Payment> findByOrderId(UUID orderId);
}
```

---

### Layer 4: Entity Layer (Domain Model)
**Package:** `entity`

**Responsibilities:**
- Database table mapping
- Mutable domain objects (only within service layer)

**Key Classes:**
```java
@Table("customer")
public class Customer {
    @Id
    private UUID id;
    private String name;
    private Integer balance;
    // getters/setters
}

@Table("customer_payment")
public class Payment {
    @Id
    private UUID id;
    private UUID orderId;
    private UUID customerId;
    private PaymentStatus status;
    private Integer amount;
    // getters/setters
}
```

**Scope:** Entities are **mutable** and should **never leave the service layer**

---

### Layer 5: DTO Layer (Data Transfer Objects)
**Package:** `dto`

**Responsibilities:**
- Immutable data carriers between layers
- Decoupling messaging layer from service layer
- Type-safe communication contracts

**Key Classes:**
```java
public record PaymentRequestDto(
    UUID orderId,
    UUID customerId,
    Integer productId,
    Integer amount
) {}

public record PaymentResponseDto(
    UUID paymentId,
    UUID orderId,
    UUID customerId,
    PaymentStatus status,
    Integer amount
) {}
```

---

## Communication Flow

```
┌─────────────────────────────────────────────────────────────┐
│                     Kafka Topic                             │
│                  (order-events-topic)                       │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ OrderCreated event
                            ↓
┌─────────────────────────────────────────────────────────────┐
│              MESSAGING LAYER (Kafka Layer)                  │
│  - PaymentEventListener (Spring Cloud Function)             │
│  - PaymentEventProcessorImpl (OrderEventProcessor impl)     │
│                                                              │
│  OrderEvent → PaymentRequestDto                             │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ PaymentRequestDto
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   SERVICE LAYER                             │
│  - PaymentService                                           │
│  - Business logic, validation                               │
│  - Transaction management                                   │
│                                                              │
│  PaymentRequestDto → Entity → PaymentResponseDto            │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ Entity operations
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                 REPOSITORY LAYER                            │
│  - CustomerRepository (R2DBC)                               │
│  - PaymentRepository (R2DBC)                                │
│                                                              │
│  Entity ↔ Database                                          │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ SQL queries
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    H2 DATABASE                              │
│  - customer table                                           │
│  - customer_payment table                                   │
└─────────────────────────────────────────────────────────────┘
```

---

## Data Flow: Event to Database

### Inbound Flow (OrderCreated → PaymentDeducted)

```
1. Kafka Message (OrderCreated)
   ↓
2. Messaging Layer receives OrderEvent
   ↓
3. Map: OrderEvent → PaymentRequestDto
   {
     orderId: UUID,
     customerId: UUID,
     amount: Integer
   }
   ↓
4. Service Layer receives PaymentRequestDto
   ↓
5. Service queries Repository for Customer entity
   ↓
6. Service validates and updates Customer entity
   ↓
7. Service creates Payment entity
   ↓
8. Repository saves entities to database
   ↓
9. Service returns PaymentResponseDto
   {
     paymentId: UUID,
     orderId: UUID,
     status: DEDUCTED,
     amount: Integer
   }
   ↓
10. Messaging Layer maps: PaymentResponseDto → PaymentEvent.PaymentDeducted
    ↓
11. EventPublisher emits PaymentDeducted to Kafka
```

---

## Key Design Principles

### 1. **Separation of Concerns**
- Each layer has a single, well-defined responsibility
- No cross-cutting concerns (Kafka logic doesn't leak into service layer)

### 2. **Immutability at Boundaries**
- DTOs (records) are immutable
- Entities are mutable but confined to service layer
- Events are immutable (sealed interface with records)

### 3. **Dependency Direction**
```
Messaging Layer → Service Layer → Repository Layer → Database
     (DTO)            (Entity)         (Entity)
```
- Messaging layer depends on Service layer (not vice versa)
- Service layer doesn't know about Kafka
- Repository layer doesn't know about DTOs or events

### 4. **Encapsulation**
- **Entities never leave service layer** (prevents external mutation)
- **Events never enter service layer** (domain logic is event-agnostic)
- **DTOs bridge the layers** (clean contracts)

### 5. **Testability**
- Service layer can be tested without Kafka
- Business logic isolated from infrastructure
- Mock repositories for unit tests
- Integration tests at messaging layer

---

## Benefits of This Architecture

### ✅ **Maintainability**
- Clear boundaries make code easier to understand
- Changes to Kafka configuration don't affect business logic
- Changes to database schema don't affect event handling

### ✅ **Testability**
- Service layer can be unit tested independently
- Mock DTOs instead of complex Kafka messages
- No need for embedded Kafka in service layer tests

### ✅ **Flexibility**
- Can swap Kafka for RabbitMQ without changing service layer
- Can change database (H2 → PostgreSQL) without affecting messaging
- Can add REST endpoints reusing same service layer

### ✅ **Type Safety**
- DTOs provide compile-time contracts
- No accidental entity mutation outside service layer
- Pattern matching ensures all event types handled

### ✅ **Scalability**
- Layers can be optimized independently
- Service layer can be reused by multiple consumers (Kafka, REST, gRPC)
- Clear separation enables microservice extraction

---

## Implementation Checklist

### Messaging Layer
- [ ] Create `PaymentEventListener` (Spring Cloud Function consumer)
- [ ] Implement `OrderEventProcessor<PaymentEvent>`
- [ ] Handle `OrderCreated` → call `paymentService.deductPayment()`
- [ ] Handle `OrderCancelled` → call `paymentService.refundPayment()`
- [ ] Map events to DTOs
- [ ] Map DTOs to outbound events
- [ ] Configure Kafka bindings in `application.yaml`

### Service Layer
- [ ] Create `PaymentService`
- [ ] Implement `deductPayment(PaymentRequestDto)` method
- [ ] Implement `refundPayment(UUID orderId)` method
- [ ] Add balance validation logic
- [ ] Add duplicate payment check (idempotency)
- [ ] Transaction management with `@Transactional` (if needed)

### Repository Layer
- [ ] Create `CustomerRepository extends ReactiveCrudRepository`
- [ ] Create `PaymentRepository extends ReactiveCrudRepository`
- [ ] Add custom query: `findByOrderId(UUID orderId)`

### Entity Layer
- [ ] Create `Customer` entity with `@Table("customer")`
- [ ] Create `Payment` entity with `@Table("customer_payment")`
- [ ] Add Lombok `@Data` or getters/setters

### DTO Layer
- [ ] Create `PaymentRequestDto` record
- [ ] Create `PaymentResponseDto` record

### Data Initialization
- [ ] Create `data.sql` or `DataInitializer` class
- [ ] Pre-populate `customer` table with sample data

### Testing
- [ ] Unit tests for `PaymentService`
- [ ] Integration tests for messaging layer
- [ ] Test scenarios: deduction, refund, insufficient balance, duplicate payment

---

## Summary

This architecture demonstrates **enterprise-grade separation of concerns** for event-driven microservices:

- **Messaging Layer:** Kafka ↔ DTO translation
- **Service Layer:** Business logic with Entity manipulation
- **Repository Layer:** Database access
- **DTO Layer:** Immutable contracts between layers
- **Entity Layer:** Mutable domain model (service-scoped)

The key insight: **Events and Entities never meet directly**. DTOs act as the translation layer, keeping domain logic clean and testable.

