package jayslabs.kafka.order.common.service;

import jayslabs.kafka.order.common.dto.PurchaseOrderDTO;

public interface OrderEventListener {

    void emitOrderCreated(PurchaseOrderDTO dto);
}
