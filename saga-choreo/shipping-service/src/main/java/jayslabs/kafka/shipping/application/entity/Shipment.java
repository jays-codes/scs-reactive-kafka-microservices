package jayslabs.kafka.shipping.application.entity;

import org.springframework.data.annotation.Id;

import java.time.Instant;
import java.util.UUID;

import jayslabs.kafka.common.events.shipping.ShippingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {

    @Id
    private UUID id;
    private UUID orderId;
    private Integer productId;
    private Integer customerId;
    private Integer quantity;
    private ShippingStatus status;
    private Instant deliveryDate;
}
