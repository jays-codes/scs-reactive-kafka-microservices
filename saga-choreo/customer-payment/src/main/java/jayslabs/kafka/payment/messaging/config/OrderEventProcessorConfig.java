package jayslabs.kafka.payment.messaging.config;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import jayslabs.kafka.common.events.order.OrderEvent;
import jayslabs.kafka.common.events.payment.PaymentEvent;
import jayslabs.kafka.common.processor.OrderEventProcessor;
import jayslabs.kafka.common.util.MessageConverter;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@Configuration
@RequiredArgsConstructor
public class OrderEventProcessorConfig {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProcessorConfig.class); 

    /*
    Interface: OrderEventProcessor<PaymentEvent>
    ↓ (Spring injects concrete implementation)
    Implementation: OrderEventProcessorImpl
    ↓ (which delegates to)
    Service: PaymentService
    */
    private final OrderEventProcessor<PaymentEvent> evtProcessor;

    @Bean 
    public Function<Flux<Message<OrderEvent>>, Flux<Message<PaymentEvent>>> processor(){
        return flux -> flux.map(MessageConverter::toRecord) //Flux<CustomRecord<OrderEvent>>
            .doOnNext(cr -> log.info("customer payment received {}", cr.message()))
            .concatMap(cr -> this.evtProcessor.process(cr.message())
               //best place to do retry logic, send to DLT if error is thrown
               //.retry(2)
               // -or- .onErrorResume() //refer to section7.CharFinder in proj scs-kafka-sandbox 
               .doOnSuccess(evt -> cr.acknowledgement().acknowledge())
            ).map(this::toMessage);
    }

    private Message<PaymentEvent> toMessage(PaymentEvent evt){
        return MessageBuilder.withPayload(evt)
            .setHeader(KafkaHeaders.KEY, evt.orderId().toString())
            .build();
    }
}

/**
Data Flow:

Input:  Message<OrderEvent> (Spring framework type)
    ↓
Convert: CustomRecord<OrderEvent> (domain type)
    ↓
Process: PaymentDTO (service layer type)
    ↓
Convert: PaymentEvent (domain event type)
    ↓
Output: Message<PaymentEvent> (Spring framework type)

 */