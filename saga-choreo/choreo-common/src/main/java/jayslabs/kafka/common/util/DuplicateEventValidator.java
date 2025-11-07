package jayslabs.kafka.common.util;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jayslabs.kafka.common.exception.EventAlreadyProcessedException;
import reactor.core.publisher.Mono;

public class DuplicateEventValidator {

    private static final Logger log = LoggerFactory.getLogger(DuplicateEventValidator.class);

    public static Function<Mono<Boolean>, Mono<Void>> emitDuplicateError(){
        return mono -> mono
            .flatMap(b -> b ? Mono.error(new EventAlreadyProcessedException()) : Mono.empty())
            .doOnError(EventAlreadyProcessedException.class, ex -> log.warn("Duplicate event"))
            .then();
    }

    public static <T> Mono<T> validate(Mono<Boolean> evtValidationPub, Mono<T> evtProcessingPub){
        return evtValidationPub
            .transform(emitDuplicateError())
            .then(evtProcessingPub);
    }
}
