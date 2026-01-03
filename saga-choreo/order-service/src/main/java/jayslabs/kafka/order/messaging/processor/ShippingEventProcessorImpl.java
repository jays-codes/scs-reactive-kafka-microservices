package jayslabs.kafka.order.messaging.processor;

import org.springframework.stereotype.Service;

import jayslabs.kafka.common.events.order.OrderEvent;
import jayslabs.kafka.common.events.shipping.ShippingEvent;
import jayslabs.kafka.common.processor.ShippingEventProcessor;
import jayslabs.kafka.order.common.service.shipping.ShippingComponentStatusListener;
import jayslabs.kafka.order.messaging.mapper.ShippingEventMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ShippingEventProcessorImpl implements ShippingEventProcessor<OrderEvent> {

    private final ShippingComponentStatusListener statusListener;

    @Override
    public Mono<OrderEvent> handle(ShippingEvent.ShippingScheduled event) {
        var dto = ShippingEventMapper.toOrderShipmentDTO(event);
        return this.statusListener.onSuccess(dto)
        .then(Mono.empty());
    }
}
