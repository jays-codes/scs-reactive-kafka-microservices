package jayslabs.kafka.section8.dto;

public record OrderEvent(
    int customerId,
    int productId,
    OrderType orderType
) {

}
