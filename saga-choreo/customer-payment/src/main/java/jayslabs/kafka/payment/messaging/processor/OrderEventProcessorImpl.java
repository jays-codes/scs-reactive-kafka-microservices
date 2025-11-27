package jayslabs.kafka.payment.messaging.processor;

import java.util.function.UnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jayslabs.kafka.common.events.order.OrderEvent;
import jayslabs.kafka.common.events.payment.PaymentEvent;
import jayslabs.kafka.common.exception.EventAlreadyProcessedException;
import jayslabs.kafka.common.processor.OrderEventProcessor;
import jayslabs.kafka.payment.common.service.PaymentService;
import jayslabs.kafka.payment.messaging.mapper.EventDTOMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/** 
    Messaging layer responsibilities: consume OrderEvents from Kafka, 
    delegate business logic to PaymentService, transform responses to PaymentEvents 
    for downstream services, handle Kafka-specific concerns (acknowledgment, 
    error handling), maintain reactive flow integrity; 
    
    TODO: OrderCancelled → processRefund() compensation flow, 
    OrderCompleted → no-op terminal event; 
    
    Pattern demonstrates enterprise event-driven messaging layer: clean separation 
    between Kafka concerns and business logic, type-safe sealed interface event routing, 
    reactive pipeline composition, immutable DTO/event transformations, saga choreography 
    event correlation via orderId; Enables distributed transaction management where 
    payment events trigger inventory/shipping services without central orchestrator
 */

@Service
@RequiredArgsConstructor
public class OrderEventProcessorImpl implements OrderEventProcessor<PaymentEvent> {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProcessorImpl.class);

    private final PaymentService service;

    /**
     * Payment processing pipeline:
     
    OrderEvent.OrderCreated
        ↓ (event received from Kafka -> handle() method is called)
        ↓
    EventDTOMapper.toPaymentProcessRequest(event)
        ↓ (converts event to PaymentProcessRequest DTO as is required by service layer)
        ↓
    service.processPayment(PaymentProcessRequest)
        ↓ (performs requisite repository operations and returns Mono<PaymentDTO>)
        ↓
    .map(EventDTOMapper::toPaymentDeductedEvent)
        ↓ (assuming successful payment, sends the appropriate event using mapper to convert 
        ↓ PaymentDTO to PaymentEvent.PaymentDeducted)
        ↓
    .doOnNext(evt -> log.info("payment processed {}", evt))
        ↓ (side-effect logging)
    .transform(exceptionHandler(event))
        ↓ (if payment fails, and error signal is received, applies exception handling 
        ↓ transformation to convert the error to PaymentEvent.PaymentFailed)
        ↓
    Mono<PaymentEvent> (emits PaymentDeducted OR PaymentFailed)
     */
    @Override
    public Mono<PaymentEvent> handle(OrderEvent.OrderCreated event) {
        return this.service.processPayment(EventDTOMapper.toPaymentProcessRequest(event))
        .map(EventDTOMapper::toPaymentDeductedEvent)
        .doOnNext(evt -> log.info("payment processed {}", evt))
        .transform(exceptionHandler(event));
    }

    @Override
    public Mono<PaymentEvent> handle(OrderEvent.OrderCancelled event) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Mono<PaymentEvent> handle(OrderEvent.OrderCompleted event) {
        // TODO Auto-generated method stub
        return null;
    }

    private UnaryOperator<Mono<PaymentEvent>> exceptionHandler(OrderEvent.OrderCreated evt){
        return mono -> mono.onErrorResume(EventAlreadyProcessedException.class, e-> Mono.empty())
        //.onErrorResume(CustomerNotFoundException.class, EventDTOMapper.toPaymentFailedEvent(evt))
        //.onErrorResume(InsufficientBalanceException.class, EventDTOMapper.toPaymentFailedEvent(evt))
        
        //Catch-all safety net - handles unexpected exceptions
        .onErrorResume(EventDTOMapper.toPaymentFailedEvent(evt));
    }
}

/*
Full reactive flow including exception handling:

Scenario: Customer not found
↓
service.processPayment() executes
↓
custRepo.findById(customerId) returns Mono.empty()
↓
.switchIfEmpty(CUSTOMER_NOT_FOUND) emits error
↓
Mono.error(CustomerNotFoundException("Customer not found"))
↓
.map(EventDTOMapper::toPaymentDeductedEvent) SKIPPED (error short-circuits)
↓
.doOnNext(...) SKIPPED
↓
.transform(exceptionHandler(event)) executes
↓
.onErrorResume(EventAlreadyProcessedException.class, ...) 
  → No match (not EventAlreadyProcessedException)
↓
.onErrorResume(EventDTOMapper.toPaymentFailedEvent(evt))
  → MATCHES! (catches CustomerNotFoundException)
↓
Executes: EventDTOMapper.toPaymentFailedEvent(evt).apply(ex)
↓
Creates: PaymentEvent.PaymentFailed(
    orderId=abc-123,
    customerId=999,
    amount=50,
    message="Customer not found",
    createdAt=2025-11-24T10:00:00Z
)
↓
Returns: Mono<PaymentEvent> (emits PaymentFailed)
↓
Pipeline completes successfully (error recovered)
↓
PaymentFailed event published to Kafka
↓
Downstream services react (e.g., Order Service marks order as FAILED)

*/