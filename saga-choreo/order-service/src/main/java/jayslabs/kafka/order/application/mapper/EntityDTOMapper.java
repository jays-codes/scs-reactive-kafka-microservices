package jayslabs.kafka.order.application.mapper;

import jayslabs.kafka.order.application.entity.OrderInventory;
import jayslabs.kafka.order.application.entity.OrderPayment;
import jayslabs.kafka.order.application.entity.PurchaseOrder;
import jayslabs.kafka.order.common.dto.OrderInventoryDTO;
import jayslabs.kafka.order.common.dto.OrderPaymentDTO;
import jayslabs.kafka.order.common.dto.OrderCreateRequest;
import jayslabs.kafka.order.common.dto.PurchaseOrderDTO;
import jayslabs.kafka.common.events.order.OrderStatus;

public class EntityDTOMapper {

    // typically invoked when processing a request to create a new order, with OrderCreateRequest as input coming 
    // from the controller layer
    public static PurchaseOrder toPurchaseOrder(OrderCreateRequest request) {
        return PurchaseOrder.builder()
            .customerId(request.customerId())
            .productId(request.productId())
            .quantity(request.quantity())
            .unitPrice(request.unitPrice())
            .amount(request.quantity() * request.unitPrice())
            .status(OrderStatus.PENDING)
            .build();
    }

    // invoked when returning the order details to the client, with PurchaseOrder as input coming 
    // from the repository layer
    public static PurchaseOrderDTO toPurchaseOrderDTO(PurchaseOrder entity) {
        return PurchaseOrderDTO.builder()
            .orderId(entity.getOrderId())
            .customerId(entity.getCustomerId())
            .productId(entity.getProductId())
            .quantity(entity.getQuantity())
            .unitPrice(entity.getUnitPrice())
            .amount(entity.getAmount())
            .status(entity.getStatus())
            .deliveryDate(entity.getDeliveryDate())
            .build();
    }

    // OrderPayment transformations
    public static OrderPayment toOrderPayment(OrderPaymentDTO dto) {
        return OrderPayment.builder()
            .orderId(dto.orderId())
            .paymentId(dto.paymentId())
            .status(dto.status())
            .message(dto.message())
            .build();
    }

    public static OrderPaymentDTO toOrderPaymentDTO(OrderPayment entity) {
        return OrderPaymentDTO.builder()
            .paymentId(entity.getPaymentId())
            .orderId(entity.getOrderId())
            .status(entity.getStatus())
            .message(entity.getMessage())
            .build();
    }

    // OrderInventory transformations
    public static OrderInventory toOrderInventory(OrderInventoryDTO dto) {
        return OrderInventory.builder()
            .orderId(dto.orderId())
            .inventoryId(dto.inventoryId())
            .status(dto.status())
            .message(dto.message())
            .build();
    }

    public static OrderInventoryDTO toOrderInventoryDTO(OrderInventory entity) {
        return OrderInventoryDTO.builder()
            .inventoryId(entity.getInventoryId())
            .orderId(entity.getOrderId())
            .status(entity.getStatus())
            .message(entity.getMessage())
            .build();
    }
}
