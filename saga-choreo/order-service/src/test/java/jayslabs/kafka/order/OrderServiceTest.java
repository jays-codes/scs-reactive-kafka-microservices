package jayslabs.kafka.order;

import org.junit.jupiter.api.Test;

import jayslabs.kafka.common.events.inventory.InventoryEvent;
import jayslabs.kafka.common.events.payment.PaymentEvent;

public class OrderServiceTest extends AbstractIntegrationTest{

    @Test
    public void orderCompleteWorkflowTest() throws InterruptedException{

        // simulate OrderCreateRequest sent from OrderController
        var req = TestDataUtil.toOrderCreateRequest(1, 1, 2, 3);
        
        // validate order in pending state
        var ordId = initiateOrder(req);

        // check for OrderCreated event
        verifyOrderCreatedEvent(ordId, 6);

        // simulate PaymentDeducted event sent from PaymentService
        emitEvent(PaymentEvent.PaymentDeducted.builder().orderId(ordId).build());

        //Thread.sleep(1_000);

        // simulate InventoryDeducted event sent from InventoryService
        emitEvent(InventoryEvent.InventoryDeducted.builder().orderId(ordId).build());

        // check for OrderCompleted event
        verifyOrderCompletedEvent(ordId);
    }
}
