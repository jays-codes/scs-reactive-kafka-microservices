package jayslabs.kafka.common.processor;

import jayslabs.kafka.common.events.DomainEvent;
import reactor.core.publisher.Mono;

public interface EventProcessor<T extends DomainEvent, R extends DomainEvent> {

    Mono<R> process(T event);

}
