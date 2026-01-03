package jayslabs.kafka.order.messaging.processor;

import org.springframework.stereotype.Service;

import jayslabs.kafka.common.events.order.OrderEvent;
import jayslabs.kafka.common.events.payment.PaymentEvent;
import jayslabs.kafka.common.processor.PaymentEventProcessor;
import jayslabs.kafka.order.common.service.OrderFulfillmentService;
import jayslabs.kafka.order.common.service.payment.PaymentComponentStatusListener;
import jayslabs.kafka.order.messaging.mapper.OrderEventMapper;
import jayslabs.kafka.order.messaging.mapper.PaymentEventMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class PaymentEventProcessorImpl implements PaymentEventProcessor<OrderEvent> {

    private final OrderFulfillmentService fulfillmentService;
    private final PaymentComponentStatusListener statusListener;

    @Override
    public Mono<OrderEvent> handle(PaymentEvent.PaymentDeducted event) {
        var dto = PaymentEventMapper.toOrderPaymentDTO(event);
        return this.statusListener.onSuccess(dto)
        .then(this.fulfillmentService.completeOrder(event.orderId()))
        .map(OrderEventMapper::toOrderCompletedEvent);
    }

    @Override
    public Mono<OrderEvent> handle(PaymentEvent.PaymentFailed event) {
        var dto = PaymentEventMapper.toOrderPaymentDTO(event);
        return this.statusListener.onFailure(dto)
        .then(this.fulfillmentService.cancelOrder(event.orderId()))
        .map(OrderEventMapper::toOrderCancelledEvent);
    }

    @Override
    public Mono<OrderEvent> handle(PaymentEvent.PaymentRefunded event) {
        var dto = PaymentEventMapper.toOrderPaymentDTO(event);
        return this.statusListener.onRollback(dto)
        .then(Mono.empty());
    }
}
