package jayslabs.kafka.payment.application.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jayslabs.kafka.common.events.payment.PaymentStatus;
import jayslabs.kafka.common.util.DuplicateEventValidator;
import jayslabs.kafka.payment.application.entity.Customer;
import jayslabs.kafka.payment.application.mapper.EntityDTOMapper;
import jayslabs.kafka.payment.application.repository.CustomerRepository;
import jayslabs.kafka.payment.application.repository.PaymentRepository;
import jayslabs.kafka.payment.common.dto.PaymentDTO;
import jayslabs.kafka.payment.common.dto.PaymentProcessRequestDTO;
import jayslabs.kafka.payment.common.exception.CustomerNotFoundException;
import jayslabs.kafka.payment.common.exception.InsufficientBalanceException;
import jayslabs.kafka.payment.common.service.PaymentService;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class PayymentServiceImpl implements PaymentService{

    private static final Logger log = LoggerFactory.getLogger(PayymentServiceImpl.class);

    private static final Mono<Customer> CUSTOMER_NOT_FOUND = Mono.error(new CustomerNotFoundException());
    private static final Mono<Customer> INSUFFICIENT_BALANCE = Mono.error(new InsufficientBalanceException());

    private final CustomerRepository custRepo;
    private final PaymentRepository pymtRepo;

    @Override
    @Transactional
    public Mono<PaymentDTO> processPayment(PaymentProcessRequestDTO request) {
        return DuplicateEventValidator.validate(
            //db call to check if orderId already exists
            this.pymtRepo.existsByOrderId(request.orderId()), 
            this.custRepo.findById(request.customerId())
            .switchIfEmpty(CUSTOMER_NOT_FOUND)
            .filter(cust -> cust.getBalance() >= request.amount())
            .switchIfEmpty(INSUFFICIENT_BALANCE)
            .flatMap(cust -> this.deductPayment(cust, request))
            .doOnNext(pymtDTO -> log.info("Payment deducted successfully for orderId: {}", pymtDTO.orderId()))
        );
    }

    @Override
    public Mono<PaymentDTO> refundPayment(UUID orderId) {
        return null;
    }

    private Mono<PaymentDTO> deductPayment(Customer customer, PaymentProcessRequestDTO request) {
        var payment = EntityDTOMapper.toCustomerPayment(request);
        customer.setBalance(customer.getBalance() - request.amount());
        payment.setStatus(PaymentStatus.DEDUCTED);
        return this.custRepo.save(customer)
        .then(this.pymtRepo.save(payment))
        .map(EntityDTOMapper::toPaymentDTO);
    }

}
