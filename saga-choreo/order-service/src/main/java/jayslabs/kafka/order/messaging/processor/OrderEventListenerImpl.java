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
// from a @Bean factory method in OrderEventListenerConfig.
@RequiredArgsConstructor
public class OrderEventListenerImpl implements OrderEventListener, EventPublisher<OrderEvent> {

    private final static Logger log = LoggerFactory.getLogger(OrderEventListenerImpl.class);

    private final Sinks.Many<OrderEvent> sink; //Where you drop events
    private final Flux<OrderEvent> flux; //Stream that reads from sink
    
    //Constructor Injection: OrderEventListenerConfig.orderEventListener() 
    // injects sink and flux from the bean factory method



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

/*
What happens at startup

Application Start
    ↓
// Setting up the bean
OrderEventListenerConfig.orderEventListener()
    ↓
1. Create Sinks.Many<OrderEvent> 
   (Think of this as a "bucket" where you can drop events)
    ↓
2. Create Flux<OrderEvent> from sink
   (Think of this as a "stream" that reads from the bucket)
    ↓
3. Create OrderEventListenerImpl(sink, flux)
    ↓
4. Spring registers this as the OrderEventListener bean
    ↓

// Actual usage
5. When OrderServiceImpl.placeOrder() is called, it emits an OrderEvent.OrderCreated
    ↓
6. OrderEventListenerImpl.emitOrderCreated() is called
    ↓
7. OrderEvent.OrderCreated is emitted to the sink
    ↓
8. Flux<OrderEvent> is updated to include the new event
    ↓
9. Spring Cloud Stream consumes the event from the flux and publishes it to the order-events topic
*/