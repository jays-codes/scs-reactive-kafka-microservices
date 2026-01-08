package jayslabs.kafka.order;

import jayslabs.kafka.order.common.dto.OrderCreateRequest;

public class TestDataUtil {


    public static OrderCreateRequest toOrderCreateRequest(
        int custId, int prodId, int unitPrice, int qty){
            return OrderCreateRequest.builder()
                .unitPrice(unitPrice)
                .quantity(qty)
                .customerId(custId)
                .productId(prodId)
                .build();
    }
}
