package jayslabs.kafka.order.application.entity;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;

import jayslabs.kafka.common.events.order.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrder {

    @Id
    private UUID orderId;
    private Integer customerId;
    private Integer productId;
    private Integer quantity;
    private Integer unitPrice;
    private Integer amount;
    private OrderStatus status;
    private Instant deliveryDate;
}
