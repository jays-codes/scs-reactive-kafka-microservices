package jayslabs.kafka.order.messaging.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jayslabs.kafka.common.events.order.OrderEvent;
import jayslabs.kafka.order.common.service.OrderEventListener;
import jayslabs.kafka.order.messaging.processor.OrderEventListenerImpl;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Sinks;

@Configuration
@RequiredArgsConstructor
public class OrderEventListenerConfig {

    @Bean
    public OrderEventListener orderEventListener(){
        var sink = Sinks.many().unicast().<OrderEvent>onBackpressureBuffer();
        var flux = sink.asFlux();
        return new OrderEventListenerImpl(sink, flux);
    }
}
