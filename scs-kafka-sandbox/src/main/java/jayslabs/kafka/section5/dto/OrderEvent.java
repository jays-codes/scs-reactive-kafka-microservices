package jayslabs.kafka.section5.dto;

public record OrderEvent(
    int customerId,
    int productId,
    OrderType orderType
) {

}
