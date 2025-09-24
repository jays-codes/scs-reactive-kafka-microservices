package jayslabs.kafka.section9.dto;

public record OrderEvent(
    int customerId,
    int productId,
    OrderType orderType
) {

}
