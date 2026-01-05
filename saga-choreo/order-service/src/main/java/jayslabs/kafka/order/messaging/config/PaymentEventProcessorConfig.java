package jayslabs.kafka.order.messaging.config;

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
import jayslabs.kafka.common.processor.PaymentEventProcessor;
import jayslabs.kafka.common.util.MessageConverter;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;


@Configuration
@RequiredArgsConstructor
public class PaymentEventProcessorConfig {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventProcessorConfig.class);
    private final PaymentEventProcessor<OrderEvent> evtProcessor;

    @Bean
    public Function<Flux<Message<PaymentEvent>>, Flux<Message<OrderEvent>>> processor(){
        return flux -> flux.map(MessageConverter::toRecord)
        .doOnNext(cr -> log.info("received in order-service: {}", cr.message()))
        .concatMap(cr -> this.evtProcessor.process(cr.message())
        .doOnSuccess(evt -> cr.acknowledgement().acknowledge())
        ).map(this::toMessage);
    }

    private Message<OrderEvent> toMessage(OrderEvent evt){
        return MessageBuilder.withPayload(evt)
        .setHeader(KafkaHeaders.KEY, evt.orderId().toString())
        .build();
    }
}
