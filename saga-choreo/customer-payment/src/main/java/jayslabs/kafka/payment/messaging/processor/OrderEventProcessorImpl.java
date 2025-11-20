package jayslabs.kafka.payment.messaging.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jayslabs.kafka.common.events.order.OrderEvent;
import jayslabs.kafka.common.events.payment.PaymentEvent;
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

    @Override
    public Mono<PaymentEvent> handle(OrderEvent.OrderCreated event) {
        return this.service.processPayment(EventDTOMapper.toPaymentProcessRequest(event))
        .map(EventDTOMapper::toPaymentDeductedEvent)
        .doOnNext(evt -> log.info("payment processed for orderId: {}", evt.orderId()));
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



}