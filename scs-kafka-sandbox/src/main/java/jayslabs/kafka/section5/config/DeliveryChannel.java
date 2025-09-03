package jayslabs.kafka.section5.config;

import jayslabs.kafka.section5.dto.OrderType;

/**
 * Enum-based channel configuration with type safety.
 * Maps order types to their corresponding delivery channels.
 */
public enum DeliveryChannel {
    
    DIGITAL_DELIVERY("digital-delivery-topic", OrderType.DIGITAL),
    PHYSICAL_DELIVERY("physical-delivery-topic", OrderType.PHYSICAL);
    
    private final String topicName;
    private final OrderType orderType;
    
    DeliveryChannel(String topicName, OrderType orderType) {
        this.topicName = topicName;
        this.orderType = orderType;
    }
    
    public String getTopicName() {
        return topicName;
    }
    
    public OrderType getOrderType() {
        return orderType;
    }
    
    /**
     * Find delivery channel by order type.
     */
    public static DeliveryChannel findByOrderType(OrderType orderType) {
        for (DeliveryChannel channel : values()) {
            if (channel.orderType == orderType) {
                return channel;
            }
        }
        throw new IllegalArgumentException("No delivery channel found for order type: " + orderType);
    }
}
