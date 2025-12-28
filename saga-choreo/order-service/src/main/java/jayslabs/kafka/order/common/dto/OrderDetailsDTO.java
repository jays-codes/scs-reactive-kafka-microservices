package jayslabs.kafka.order.common.dto;

import lombok.Builder;

@Builder
public record OrderDetailsDTO(
    PurchaseOrderDTO order,
    OrderPaymentDTO payment,
    OrderInventoryDTO inventory
) {

}
