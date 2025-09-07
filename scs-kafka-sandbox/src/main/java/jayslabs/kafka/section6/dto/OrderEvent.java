package jayslabs.kafka.section6.dto;

public record OrderEvent(
    int customerId,
    int productId,
    OrderType orderType
) {

}
