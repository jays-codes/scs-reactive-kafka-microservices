package jayslabs.kafka.common.processor;

import jayslabs.kafka.common.events.DomainEvent;
import jayslabs.kafka.common.events.payment.PaymentEvent;
import reactor.core.publisher.Mono;

public interface PaymentEventProcessor<R extends DomainEvent>
extends EventProcessor<PaymentEvent, R> {

    @Override
    default Mono<R> process(PaymentEvent event){
        return switch(event){
            case PaymentEvent.PaymentDeducted e-> this.handle(e);
            case PaymentEvent.PaymentRefunded e-> this.handle(e);
            case PaymentEvent.PaymentFailed e-> this.handle(e);
        };
    }

    Mono<R> handle (PaymentEvent.PaymentDeducted e);

    Mono<R> handle (PaymentEvent.PaymentRefunded e);

    Mono<R> handle (PaymentEvent.PaymentFailed e);

}
