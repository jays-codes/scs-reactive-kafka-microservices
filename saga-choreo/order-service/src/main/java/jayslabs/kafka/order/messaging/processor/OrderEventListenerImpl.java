package jayslabs.kafka.order.messaging.processor;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jayslabs.kafka.common.events.order.OrderEvent;
import jayslabs.kafka.common.publisher.EventPublisher;
import jayslabs.kafka.order.common.dto.PurchaseOrderDTO;
import jayslabs.kafka.order.common.service.OrderEventListener;
import jayslabs.kafka.order.messaging.mapper.OrderEventMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

//@Service
// NOTE: @Service is intentionally omitted because Sinks.Many<OrderEvent> 
// cannot be auto-injected by Spring. The sink is injected via constructor 
// from a @Bean factory method in configuration.
@RequiredArgsConstructor
public class OrderEventListenerImpl implements OrderEventListener, EventPublisher<OrderEvent> {

    private final static Logger log = LoggerFactory.getLogger(OrderEventListenerImpl.class);

    private final Sinks.Many<OrderEvent> sink;
    private final Flux<OrderEvent> flux;

    @Override
    public void emitOrderCreated(PurchaseOrderDTO dto) {
        var event = OrderEventMapper.toOrderCreatedEvent(dto);
        this.sink.emitNext(
            event,
            Sinks.EmitFailureHandler.busyLooping(Duration.ofSeconds(1))
        );

    }

    @Override
    public Flux<OrderEvent> publish() {
        return this.flux;
    }
}

/*
1. ORDER CREATED
   └─ OrderService.createOrder(orderRequest)
   
2. EMIT ORDER EVENT
   └─ orderEventListener.emitOrderCreated(dto)
      └─ sink.emitNext(OrderEvent.OrderCreated)
      
3. FLUX RECEIVES EVENT
   └─ All subscribers of publish() receive the event
   
4. SPRING CLOUD STREAM BINDINGS
   └─ Publishes to Kafka topics:
      ├─ order-events (input from this service)
      ├─ payment-events (output for payment service)
      ├─ inventory-events (output for inventory service)
      └─ shipping-events (output for shipping service)
      
5. OTHER SERVICES CONSUME
   └─ Payment Service: Listens to payment-events
   └─ Inventory Service: Listens to inventory-events
   └─ Shipping Service: Listens to shipping-events
*/