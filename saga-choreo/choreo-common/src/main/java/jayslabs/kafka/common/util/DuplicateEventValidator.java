package jayslabs.kafka.common.util;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jayslabs.kafka.common.exception.EventAlreadyProcessedException;
import reactor.core.publisher.Mono;

public class DuplicateEventValidator {

    private static final Logger log = LoggerFactory.getLogger(DuplicateEventValidator.class);

    //Jay's notes in comments

    /**
        logic:
        Take the validation mono.
        Run it through the duplicate-check transformer.
        If that finishes cleanly, subscribe to the actual processing mono.
        If it errors (duplicate), short‑circuit with that error.
    */
    public static <T> Mono<T> validate(Mono<Boolean> evtValidationPub, Mono<T> evtProcessingPub){
        return evtValidationPub
            .transform(emitDuplicateError())
            //.then(evtProcessingPub) only executes if  is false (non-duplicate). 
            //else, error is propagated upwards as Mono<Void> with error signal. 
            //evtProcessingPub NEVER SUBSCRIBED TO.
            .then(evtProcessingPub);
    }

    public static Function<Mono<Boolean>, Mono<Void>> emitDuplicateError(){
        //mono below is the original Mono<Boolean> passed from transform() "evtValidationPub"
        return mono -> mono
            .flatMap(b -> b ? Mono.error(new EventAlreadyProcessedException()) : Mono.empty())
            .doOnError(EventAlreadyProcessedException.class, ex -> log.warn("Duplicate event"))
            
            //if not duplicate, Converts Mono.empty() to Mono<Void> that 
            //completes successfully, else, error is propagated upwards as Mono<Void>
            //with error signal.
            .then();
    }
}

/*
 Reactive flow ----
Scenario  A: Non-duplicate event

1. evtValidationPub emits: false
    ↓
2. .flatMap(b -> b ? Mono.error(...) : Mono.empty())
    ↓
   b = false, so returns Mono.empty()
    ↓
3. .doOnError(...) 
    ↓
   No error, so SKIPPED
    ↓
4. .then()
    ↓
   Converts Mono.empty() to Mono<Void> that completes successfully
    ↓
5. .then(evtProcessingPub) receives completion signal
    ↓
   NOW subscribes to evtProcessingPub
    ↓
6. repository.deductInventory() EXECUTES
    ↓
7. Result flows to final subscriber

-----------

Scenario  B: Duplicate event
1. evtValidationPub emits: true
    ↓
2. .flatMap(b -> b ? Mono.error(...) : Mono.empty())
    ↓
   b = true, so returns Mono.error(EventAlreadyProcessedException)
    ↓
3. .doOnError(EventAlreadyProcessedException.class, ex -> log.warn(...))
    ↓
   Error detected! Execute side effect: log.warn("Duplicate event")
    ↓
4. .then()
    ↓
   Propagates the error as Mono<Void> with error signal
    ↓
5. .then(evtProcessingPub) receives error signal
    ↓
   ERROR SHORT-CIRCUITS! evtProcessingPub NEVER SUBSCRIBES
    ↓
6. repository.deductInventory() NEVER EXECUTES
    ↓
7. Error flows to final subscriber
 */
