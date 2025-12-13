package jayslabs.kafka.common.processor;

import jayslabs.kafka.common.events.DomainEvent;
import jayslabs.kafka.common.events.order.OrderEvent;
import reactor.core.publisher.Mono;

/*
  Uses Template Method Pattern to provide default process() method; 
  Uses Strategy Pattern to handle different OrderEvent types
  Uses Pattern Matching to route different OrderEvent types to corresponding handle() methods
 */
public interface OrderEventProcessor<R extends DomainEvent> 
    extends EventProcessor<OrderEvent, R>{

    /*
    Sealed interface exhaustiveness: Java compiler ensures all OrderEvent types are handled
    Type-safe routing: Each case extracts the concrete event type
    Strategy pattern: Routes to appropriate handle() method overload
    No instanceof checks: Pattern matching provides clean, type-safe dispatch
    */
    @Override
    default Mono<R> process(OrderEvent event){
        return switch(event){
            case OrderEvent.OrderCreated e -> this.handle(e);
            case OrderEvent.OrderCancelled e -> this.handle(e);
            case OrderEvent.OrderCompleted e -> this.handle(e);
        };
    }

    Mono<R> handle (OrderEvent.OrderCreated e);

    Mono<R> handle (OrderEvent.OrderCancelled e);
    
    default Mono<R> handle (OrderEvent.OrderCompleted e){
        return Mono.empty();
    }
}
