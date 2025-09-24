package jayslabs.kafka.section9.processor;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import jayslabs.kafka.common.MessageConverter;
import jayslabs.kafka.section9.dto.DigitalDelivery;
import jayslabs.kafka.section9.dto.OrderEvent;
import jayslabs.kafka.section9.dto.OrderType;
import jayslabs.kafka.section9.dto.PhysicalDelivery;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
/*
digital order goes to digital-delivery-topic
physical order goes to physical-delivery-topic AND digital-delivery-topic
*/
@Configuration
public class FanOutProcessor {

    private static final Logger log = LoggerFactory.getLogger(FanOutProcessor.class);

    private final Sinks.Many<OrderEvent> sink = Sinks.many().multicast().onBackpressureBuffer();

    @Bean
    public Function<
            Flux<Message<OrderEvent>>,
            Tuple2<
                Flux<DigitalDelivery>,
                Flux<PhysicalDelivery>
            >
        > processor(){
        return flux -> {
            flux
            .map(MessageConverter::toRecord)
            .doOnNext(r -> this.sink.tryEmitNext(r.message()))
            .doOnNext(r -> r.acknowledgement().acknowledge())
            .subscribe();

            return Tuples.of(
                sink.asFlux().transform(this.toDigitalDelivery()),
                sink.asFlux()
                    .filter(oe -> OrderType.PHYSICAL.equals(oe.orderType()))
                    .transform(this.toPhysicalDelivery())
            );
        };
    }


    private Function<Flux<OrderEvent>, Flux<DigitalDelivery>> toDigitalDelivery(){
        return flux -> flux.map(e -> new DigitalDelivery(e.productId(), "%s@gmail.com".formatted(e.customerId())));
    }

    private Function<Flux<OrderEvent>, Flux<PhysicalDelivery>> toPhysicalDelivery(){
        return flux -> flux.map(e -> new PhysicalDelivery(
            e.productId(), 
            "%s St".formatted(e.customerId()), 
            "%s city".formatted(e.customerId()), 
            "Canada"));
    }


}
