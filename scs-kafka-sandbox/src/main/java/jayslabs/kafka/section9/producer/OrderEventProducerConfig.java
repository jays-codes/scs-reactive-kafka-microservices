package jayslabs.kafka.section9.producer;

import java.time.Duration;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import jayslabs.kafka.section9.dto.OrderEvent;
import jayslabs.kafka.section9.dto.OrderType;
import reactor.core.publisher.Flux;

@Configuration
public class OrderEventProducerConfig {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducerConfig.class);

    @Bean
    public Supplier<Flux<Message<OrderEvent>>> orderEventProducer(){

        return () -> Flux.range(1, 10)
            .delayElements(Duration.ofSeconds(1))
            .map(this::toMessage)
            .doOnNext(m -> log.info("Produced message: {}", m));
        
    }

    private Message<OrderEvent> toMessage(int i){
        var type = i % 2 == 0 ? OrderType.DIGITAL : OrderType.PHYSICAL;
        return MessageBuilder.withPayload(new OrderEvent(i, i, type))
            .setHeader(KafkaHeaders.KEY, "order-key-" + i)
            .build();
    }
}
