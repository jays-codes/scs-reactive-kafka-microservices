package jayslabs.kafka.inventory.messaging.mapper;

import java.time.Instant;
import java.util.function.Function;

import jayslabs.kafka.common.events.inventory.InventoryEvent;
import jayslabs.kafka.common.events.order.OrderEvent;
import jayslabs.kafka.inventory.common.dto.InventoryDTO;
import jayslabs.kafka.inventory.common.dto.InventoryProcessRequest;
import reactor.core.publisher.Mono;

public class EventDTOMapper {

    public static InventoryProcessRequest toInventoryProcessRequest(OrderEvent.OrderCreated evt){
        return InventoryProcessRequest.builder()
        .orderId(evt.orderId())
        .productId(evt.productId())
        .quantity(evt.quantity())
        .build();
    }

    public static InventoryEvent toInventoryDeductedEvent(InventoryDTO invdto){
        return InventoryEvent.InventoryDeducted.builder()
        .orderId(invdto.orderId())
        .inventoryId(invdto.inventoryId())
        .productId(invdto.productId())
        .quantity(invdto.quantity())
        .createdAt(Instant.now())
        .build();
    }

    public static InventoryEvent toInventoryRestoredEvent(InventoryDTO invdto){
        return InventoryEvent.InventoryRestored.builder()
        .orderId(invdto.orderId())
        .inventoryId(invdto.inventoryId())
        .productId(invdto.productId())
        .quantity(invdto.quantity())
        .createdAt(Instant.now())
        .build();
    }

    public static Function<Throwable, Mono<InventoryEvent>> toInventoryFailedEvent(OrderEvent.OrderCreated evt){
        return ex -> Mono.fromSupplier(() -> InventoryEvent.InventoryFailed.builder()
        .orderId(evt.orderId())
        .productId(evt.productId())
        .quantity(evt.quantity())
        .message(ex.getMessage())
        .createdAt(Instant.now())
        .build());
    }
}
