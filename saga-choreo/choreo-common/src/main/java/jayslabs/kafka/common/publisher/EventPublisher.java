package jayslabs.kafka.common.publisher;

import jayslabs.kafka.common.events.DomainEvent;
import reactor.core.publisher.Flux;

public interface EventPublisher<T extends DomainEvent> {

    Flux<T> publish();
}
