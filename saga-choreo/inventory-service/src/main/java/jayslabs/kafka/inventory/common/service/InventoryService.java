package jayslabs.kafka.inventory.common.service;

import java.util.UUID;

import jayslabs.kafka.inventory.common.dto.InventoryDTO;
import jayslabs.kafka.inventory.common.dto.InventoryProcessRequest;
import reactor.core.publisher.Mono;

public interface InventoryService {
    Mono<InventoryDTO> processInventory(InventoryProcessRequest request);

    Mono<InventoryDTO> processRestore(UUID orderId);

}
