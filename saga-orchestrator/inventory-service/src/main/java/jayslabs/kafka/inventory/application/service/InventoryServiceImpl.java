package jayslabs.kafka.inventory.application.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jayslabs.kafka.common.events.inventory.InventoryStatus;
import jayslabs.kafka.common.util.DuplicateEventValidator;
import jayslabs.kafka.inventory.application.entity.OrderInventory;
import jayslabs.kafka.inventory.application.entity.Product;
import jayslabs.kafka.inventory.application.mapper.EntityDTOMapper;
import jayslabs.kafka.inventory.application.repository.InventoryRepository;
import jayslabs.kafka.inventory.application.repository.ProductRepository;
import jayslabs.kafka.inventory.common.dto.InventoryDTO;
import jayslabs.kafka.inventory.common.dto.InventoryProcessRequest;
import jayslabs.kafka.inventory.common.exception.OutOfStockException;
import jayslabs.kafka.inventory.common.service.InventoryService;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;


@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService{  

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private static final Mono<Product> OUT_OF_STOCK = Mono.error(new OutOfStockException());

    private final ProductRepository prodRepo;
    private final InventoryRepository invRepo;

    @Override
    @Transactional
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

    @Override
    @Transactional
    public Mono<InventoryDTO> processRestore(UUID orderId){
        return this.invRepo.findByOrderIdAndStatus(orderId, InventoryStatus.DEDUCTED)
        .zipWhen(ordinv -> this.prodRepo.findById(ordinv.getProductId()))
        .flatMap(tup -> this.restoreInventory(tup.getT1(), tup.getT2()))
        .doOnNext(invDTO -> log.info("Inventory restored for quantity:{}, of productId: {}, for orderId: {}", 
        invDTO.quantity(), invDTO.productId(), invDTO.orderId())
        );
    }

    private Mono<InventoryDTO> deductInventory(Product prod, InventoryProcessRequest reqDTO){
        
        var ordinv = EntityDTOMapper.toOrderInventory(reqDTO);
        prod.setAvailableQuantity(prod.getAvailableQuantity() - reqDTO.quantity());
        ordinv.setStatus(InventoryStatus.DEDUCTED);

        return  this.prodRepo.save(prod)
        .then(this.invRepo.save(ordinv))
        .map(EntityDTOMapper::toInventoryDTO);
    }

    private Mono<InventoryDTO> restoreInventory(OrderInventory ordinv, Product prod){
        prod.setAvailableQuantity(prod.getAvailableQuantity() + ordinv.getQuantity());
        ordinv.setStatus(InventoryStatus.RESTORED);
        return this.prodRepo.save(prod)
        .then(this.invRepo.save(ordinv))
        .map(EntityDTOMapper::toInventoryDTO);
    }
}
