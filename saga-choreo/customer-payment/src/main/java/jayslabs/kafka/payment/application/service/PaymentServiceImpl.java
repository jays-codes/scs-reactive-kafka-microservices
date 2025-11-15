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
public class PaymentServiceImpl implements PaymentService{

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private static final Mono<Customer> CUSTOMER_NOT_FOUND = Mono.error(new CustomerNotFoundException());
    private static final Mono<Customer> INSUFFICIENT_BALANCE = Mono.error(new InsufficientBalanceException());

    private final CustomerRepository custRepo;
    private final PaymentRepository pymtRepo;

    @Override
    @Transactional
    public Mono<PaymentDTO> processPayment(PaymentProcessRequestDTO reqDTO) {
        return DuplicateEventValidator.validate(
            //[validation publisher : Mono<Boolean>] db call to check if orderId already exists. 
            //true = duplicate event, false = new event
            this.pymtRepo.existsByOrderId(reqDTO.orderId()), 

            //[processing publisher : Mono<Customer>] customer validation and payment deduction
            //returns Mono<Customer> if customer found, otherwise empty 
            this.custRepo.findById(reqDTO.customerId())
            )
            .switchIfEmpty(CUSTOMER_NOT_FOUND) //switched to error mono if customer not found

            //verify if Mono<Customer> has sufficient balance
            //returns Mono<Customer> if customer has sufficient balance, otherwise empty
            .filter(cust -> cust.getBalance() >= reqDTO.amount())
            .switchIfEmpty(INSUFFICIENT_BALANCE) //switched to error mono

            //apply payment deduction
            .flatMap(cust -> this.deductPayment(cust, reqDTO))
            .doOnNext(pymtDTO -> log.info("Payment deducted successfully for orderId: {}", pymtDTO.orderId())
        );
    }

    @Override
    public Mono<PaymentDTO> refundPayment(UUID orderId) {
        return null;
    }

    private Mono<PaymentDTO> deductPayment(Customer customer, PaymentProcessRequestDTO reqDTO) {
        var custPymt = EntityDTOMapper.toCustomerPayment(reqDTO); //[BP] for creating a pymt transaction
        customer.setBalance(customer.getBalance() - reqDTO.amount());
        custPymt.setStatus(PaymentStatus.DEDUCTED);
        return this.custRepo.save(customer)
        .then(this.pymtRepo.save(custPymt))
        .map(EntityDTOMapper::toPaymentDTO);
    }

}

/*
INPUT: PaymentProcessRequestDTO(customerId=1, orderId=abc-123, amount=50)
↓
┌─────────────────────────────────────────────────────────────┐
│ PHASE 1: IDEMPOTENCY CHECK                                  │
├─────────────────────────────────────────────────────────────┤
│ pymtRepo.existsByOrderId(abc-123)                           │
│ → Query: SELECT EXISTS(SELECT 1 FROM customer_payment       │
│          WHERE order_id = 'abc-123')                        │
│ → Result: false (not processed before)                      │
│ → DuplicateEventValidator: PASS ✓                           │
└─────────────────────────────────────────────────────────────┘
↓
┌─────────────────────────────────────────────────────────────┐
│ PHASE 2: CUSTOMER VALIDATION                                │
├─────────────────────────────────────────────────────────────┤
│ custRepo.findById(1)                                        │
│ → Query: SELECT * FROM customer WHERE id = 1                │
│ → Result: Customer(id=1, name="Sam", balance=100)           │
│ → switchIfEmpty: SKIPPED (customer found) ✓                 │
└─────────────────────────────────────────────────────────────┘
↓
┌─────────────────────────────────────────────────────────────┐
│ PHASE 3: BALANCE VALIDATION                                 │
├─────────────────────────────────────────────────────────────┤
│ filter(cust -> 100 >= 50)                                   │
│ → Predicate: true                                           │
│ → Customer passes through ✓                                 │
│ → switchIfEmpty: SKIPPED (not empty) ✓                      │
└─────────────────────────────────────────────────────────────┘
↓
┌─────────────────────────────────────────────────────────────┐
│ PHASE 4: PAYMENT DEDUCTION                                  │
├─────────────────────────────────────────────────────────────┤
│ flatMap(cust -> deductPayment(cust, request))               │
│                                                             │
│ Inside deductPayment():                                     │
│ 1. Create payment entity (orderId, customerId, amount)      │
│ 2. Update customer balance: 100 - 50 = 50 (in-memory)       │
│ 3. Set payment status: DEDUCTED                             │
│ 4. custRepo.save(customer)                                  │
│    → UPDATE customer SET balance=50 WHERE id=1              │
│ 5. .then(pymtRepo.save(payment))                            │
│    → INSERT INTO customer_payment VALUES(...)               │
│    → Auto-generated paymentId: xyz-789                      │
│ 6. .map(EntityDTOMapper::toPaymentDTO)                      │
│    → Convert entity to DTO                                  │
└─────────────────────────────────────────────────────────────┘
↓
┌─────────────────────────────────────────────────────────────┐
│ PHASE 5: LOGGING & RETURN                                   │
├─────────────────────────────────────────────────────────────┤
│ doOnNext(log.info("Payment deducted..."))                   │
│ → Log: "Payment deducted successfully for orderId: abc-123" │
└─────────────────────────────────────────────────────────────┘
↓
OUTPUT: Mono<PaymentDTO>(
    paymentId=xyz-789,
    orderId=abc-123,
    customerId=1,
    amount=50,
    status=DEDUCTED
)

*/