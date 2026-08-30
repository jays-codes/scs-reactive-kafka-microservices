package jayslabs.kafka.order.common.service;

import reactor.core.publisher.Mono;

public interface OrderComponentStatusListener<T> {

    Mono<Void> onSuccess(T event);

    Mono<Void> onFailure(T event);

    Mono<Void> onRollback(T event);
}

