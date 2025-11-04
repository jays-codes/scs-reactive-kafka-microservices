package jayslabs.kafka.common.events.shipping;

import java.time.Instant;
import java.util.UUID;

import jayslabs.kafka.common.events.DomainEvent;
import jayslabs.kafka.common.events.OrderSaga;
import lombok.Builder;

public sealed interface ShippingEvent extends DomainEvent, OrderSaga {

    @Builder
    record ShippingScheduled(
        UUID orderId,
        UUID shipmentId,
        Instant expectedDeliveryDate,
        Instant createdAt
    ) implements ShippingEvent{}
}
