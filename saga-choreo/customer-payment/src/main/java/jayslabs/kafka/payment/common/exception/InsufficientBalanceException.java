package jayslabs.kafka.payment.common.exception;

public class InsufficientBalanceException extends RuntimeException {
    private static final String MESSAGE = "Customer does not have sufficient balance";

    public InsufficientBalanceException() {
        super(MESSAGE);
    }

}
