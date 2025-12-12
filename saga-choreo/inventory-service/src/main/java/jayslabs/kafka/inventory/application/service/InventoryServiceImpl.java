package jayslabs.kafka.inventory.application.service;

import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jayslabs.kafka.common.util.DuplicateEventValidator;
import jayslabs.kafka.inventory.common.service.InventoryService;
import lombok.RequiredArgsConstructor;
import jayslabs.kafka.common.events.inventory.InventoryStatus;

import jayslabs.kafka.inventory.common.exception.OutOfStockException;

import reactor.core.publisher.Mono;
import jayslabs.kafka.inventory.application.entity.Product;
import jayslabs.kafka.inventory.application.repository.ProductRepository;
import jayslabs.kafka.inventory.application.repository.InventoryRepository;

import jayslabs.kafka.inventory.common.dto.InventoryDTO;
import jayslabs.kafka.inventory.common.dto.InventoryProcessRequest;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService{  

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private static final Mono<Product> OUT_OF_STOCK = Mono.error(new OutOfStockException());

    private static final ProductRepository prodRepo;
    private static final InventoryRepository invRepo;

    public Mono<InventoryDTO> processInventory(InventoryProcessRequest reqDTO){
        return DuplicateEventValidator.validate(
            this.invRepo.existsByOrderId(reqDTO.orderId()),
            this.prodRepo.findById(reqDTO.productId())
        )
        .filter(prod -> prod.getAvailableQuantity() >= reqDTO.quantity())
        .switchIfEmpty(OUT_OF_STOCK)
        .flatMap(prod -> this.deductInventory(prod, reqDTO))
        .doOnNext(invDTO -> log.info("Inventory deducted successfully for orderId: {}", invDTO.orderId())
        );
    }

    private Mono<InventoryDTO> deductInventory(Product prod, InventoryProcessRequest reqDTO){
        
        
        prod.setAvailableQuantity(prod.getAvailableQuantity() - reqDTO.quantity());
        return this.prodRepo.save(prod)
            .then(this.invRepo.saveInventoryRecord(reqDTO, InventoryStatus.DEDUCTED))
            .map(invRecord -> InventoryDTO.builder()
                .inventoryId(invRecord.getId())
                .orderId(reqDTO.orderId())
                .productId(reqDTO.productId())
                .quantity(reqDTO.quantity())
                .status(InventoryStatus.DEDUCTED)
                .build()
            );
    }
}
