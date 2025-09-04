package jayslabs.kafka.section5.config;

/**
 * Utility class for delivery channel binding names.
 * Uses Spring Cloud Stream binding references instead of direct topics.
 * This is a pure utility class with static methods - no Spring management needed.
 */
public final class DeliveryChannelProperties {
    
    // StreamBridge binding names (not topic names)
    private static final String DIGITAL_BINDING = "digital-delivery-out";
    private static final String PHYSICAL_BINDING = "physical-delivery-out";

    // Private constructor to prevent instantiation
    private DeliveryChannelProperties() {
        throw new UnsupportedOperationException("Utility class");
    }

    // Static getters for binding names
    public static String getDigital() {
        return DIGITAL_BINDING;
    }
    
    public static String getPhysical() {
        return PHYSICAL_BINDING;
    }
    
    /**
     * Get binding name by order type.
     * Simple, direct mapping to StreamBridge binding names.
     */
    public static String getBindingForOrderType(jayslabs.kafka.section5.dto.OrderType orderType) {
        return switch (orderType) {
            case DIGITAL -> DIGITAL_BINDING;
            case PHYSICAL -> PHYSICAL_BINDING;
        };
    }
}
