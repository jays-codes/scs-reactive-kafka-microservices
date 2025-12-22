package jayslabs.kafka.shipping.application.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jayslabs.kafka.common.events.shipping.ShippingStatus;
import jayslabs.kafka.common.util.DuplicateEventValidator;
import jayslabs.kafka.shipping.application.entity.Shipment;
import jayslabs.kafka.shipping.application.mapper.EntityDTOMapper;
import jayslabs.kafka.shipping.application.repository.ShipmentRepository;
import jayslabs.kafka.shipping.common.dto.CreateShippingRequest;
import jayslabs.kafka.shipping.common.dto.ShipmentDTO;
import jayslabs.kafka.shipping.common.service.ShippingService;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ShippingServiceImpl implements ShippingService {

    private static final Logger log = LoggerFactory.getLogger(ShippingServiceImpl.class);

    private final ShipmentRepository shipmentRepo;

    @Override
    @Transactional
    public Mono<Void> createShipmentRecord(CreateShippingRequest reqDTO) {
        return DuplicateEventValidator.validate(
            this.shipmentRepo.existsByOrderId(reqDTO.orderId()),
            this.createShipmentRec(reqDTO)
        )
        .doOnNext(shipment -> log.info("Shipment record created for orderId: {}", shipment.getOrderId()))
        .then();
    }

    private Mono<Void> createShipmentRec(CreateShippingRequest reqDTO) {
        var shipment = EntityDTOMapper.toShipment(reqDTO);
        shipment.setStatus(ShippingStatus.PENDING);
        return this.shipmentRepo.save(shipment)
        .then();
    }

    @Override
    @Transactional
    public Mono<ShipmentDTO> scheduleShipment(UUID orderId) {
        return this.shipmentRepo.findByOrderIdAndStatus(orderId, ShippingStatus.PENDING)
        .flatMap(this::scheduleShipment)
        .doOnNext(shipmentDTO -> log.info("Shipment scheduled for orderId: {}", shipmentDTO.orderId()));
    }

    private Mono<ShipmentDTO> scheduleShipment(Shipment shipment) {
        shipment.setStatus(ShippingStatus.SCHEDULED);
        shipment.setDeliveryDate(Instant.now().plus(7, ChronoUnit.DAYS));
        return this.shipmentRepo.save(shipment)
        .map(EntityDTOMapper::toShipmentDTO);
    }

    @Override
    @Transactional
    public Mono<Void> cancelShipment(UUID orderId) {
        return this.shipmentRepo.deleteByOrderId(orderId)
        .doOnNext(v -> log.info("Shipment cancelled for orderId: {}", orderId))
        .then(Mono.empty());
    }
}
