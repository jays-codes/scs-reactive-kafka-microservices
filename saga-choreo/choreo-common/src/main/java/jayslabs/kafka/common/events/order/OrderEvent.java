package jayslabs.kafka.common.events.order;

import jayslabs.kafka.common.events.DomainEvent;
import jayslabs.kafka.common.events.OrderSaga;

public interface OrderEvent extends DomainEvent, OrderSaga {

}
