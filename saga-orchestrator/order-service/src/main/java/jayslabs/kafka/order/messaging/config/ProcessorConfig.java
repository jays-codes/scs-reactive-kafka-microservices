package jayslabs.kafka.order.messaging.config;

import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import jayslabs.kafka.common.events.inventory.InventoryEvent;
import jayslabs.kafka.common.events.order.OrderEvent;
import jayslabs.kafka.common.events.payment.PaymentEvent;
import jayslabs.kafka.common.events.shipping.ShippingEvent;
import jayslabs.kafka.common.processor.EventProcessor;
import jayslabs.kafka.common.publisher.EventPublisher;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

/*
Defines Spring Cloud Stream functions (Kafka integration)
*/
@Configuration
@RequiredArgsConstructor
public class ProcessorConfig extends AbstractOrderEventRouterConfig {

    private final EventProcessor<InventoryEvent, OrderEvent> inventoryEventProcessor;
    private final EventProcessor<PaymentEvent, OrderEvent> paymentEventProcessor;
    private final EventProcessor<ShippingEvent, OrderEvent> shippingEventProcessor;
    private final EventPublisher<OrderEvent> eventPublisher;

    @Bean
    public Function<Flux<Message<InventoryEvent>>, Flux<Message<OrderEvent>>> inventoryProcessor(){
        return this.processor(this.inventoryEventProcessor);
    }

    @Bean
    public Function<Flux<Message<PaymentEvent>>, Flux<Message<OrderEvent>>> paymentProcessor(){
        return this.processor(this.paymentEventProcessor);
    }

    @Bean
    public Function<Flux<Message<ShippingEvent>>, Flux<Message<OrderEvent>>> shippingProcessor(){
        return this.processor(this.shippingEventProcessor);
    }

    @Bean
    public Supplier<Flux<Message<OrderEvent>>> orderEventProducer(){
        
        //impl of publish() method in OrderEventListenerImpl
        return () -> this.eventPublisher.publish()
            .map(this::toMessage);
    }
}
