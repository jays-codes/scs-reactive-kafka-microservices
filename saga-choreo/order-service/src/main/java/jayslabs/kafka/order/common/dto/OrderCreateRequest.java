package jayslabs.kafka.order.common.dto;

import lombok.Builder;

//Modelled as received from the controller layer
@Builder
public record OrderCreateRequest(
    Integer customerId,
    Integer productId,
    Integer quantity,
    Integer unitPrice
) {

}
