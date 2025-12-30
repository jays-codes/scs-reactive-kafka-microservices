package jayslabs.kafka.order.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import jayslabs.kafka.order.application.entity.OrderInventory;
import jayslabs.kafka.order.application.mapper.EntityDTOMapper;
import jayslabs.kafka.order.application.repository.OrderInventoryRepository;
import jayslabs.kafka.order.common.dto.OrderInventoryDTO;
import jayslabs.kafka.order.common.service.inventory.InventoryComponentFetcher;
import jayslabs.kafka.order.common.service.inventory.InventoryComponentStatusListener;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class InventoryComponentServiceImpl implements InventoryComponentFetcher, InventoryComponentStatusListener{

    private final OrderInventoryRepository invRepo;

    private static final OrderInventoryDTO DEFAULT_DTO = OrderInventoryDTO.builder().build();

    @Override
    public Mono<OrderInventoryDTO> getComponent(UUID orderId) {
        return this.invRepo.findByOrderId(orderId)
        .map(EntityDTOMapper::toOrderInventoryDTO)
        .defaultIfEmpty(DEFAULT_DTO);
    }

    @Override
    public Mono<Void> onSuccess(OrderInventoryDTO event) {
        return this.invRepo.findByOrderId(event.orderId())
        .switchIfEmpty(Mono.defer(() -> this.add(event, true)))
        .then();
    }

    @Override
    public Mono<Void> onFailure(OrderInventoryDTO event) {
        return this.invRepo.findByOrderId(event.orderId())
        .switchIfEmpty(Mono.defer(() -> this.add(event, false)))
        .then();
    }
    
    @Override
    public Mono<Void> onRollback(OrderInventoryDTO event) {
        return this.invRepo.findByOrderId(event.orderId())
        .doOnNext(entity -> entity.setStatus(event.status()))
        .flatMap(this.invRepo::save)
        .then();
    }

    private Mono<OrderInventory> add(OrderInventoryDTO dto, boolean isSuccess) {
        var entity = EntityDTOMapper.toOrderInventory(dto);
        entity.setSuccess(isSuccess);
        return this.invRepo.save(entity);
    }


}
