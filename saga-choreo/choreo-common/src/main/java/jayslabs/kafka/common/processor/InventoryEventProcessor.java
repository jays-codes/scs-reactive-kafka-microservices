package jayslabs.kafka.common.processor;

import jayslabs.kafka.common.events.DomainEvent;
import jayslabs.kafka.common.events.inventory.InventoryEvent;
import reactor.core.publisher.Mono;

public interface InventoryEventProcessor<R extends DomainEvent>
extends EventProcessor<InventoryEvent, R> {

    @Override
    default Mono<R> process(InventoryEvent event){
        return switch(event){
            case InventoryEvent.InventoryDeducted e -> this.handle(e);
            case InventoryEvent.InventoryRestored e -> this.handle(e);
            case InventoryEvent.InventoryFailed e -> this.handle(e);
        };
    }

    Mono<R> handle (InventoryEvent.InventoryDeducted e);

    Mono<R> handle (InventoryEvent.InventoryRestored e);

    Mono<R> handle (InventoryEvent.InventoryFailed e);

}
