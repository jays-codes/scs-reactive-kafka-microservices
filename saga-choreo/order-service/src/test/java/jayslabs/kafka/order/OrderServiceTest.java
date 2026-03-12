package jayslabs.kafka.order;

import java.time.Instant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jayslabs.kafka.common.events.inventory.InventoryEvent;
import jayslabs.kafka.common.events.inventory.InventoryStatus;
import jayslabs.kafka.common.events.order.OrderStatus;
import jayslabs.kafka.common.events.payment.PaymentEvent;
import jayslabs.kafka.common.events.payment.PaymentStatus;
import jayslabs.kafka.common.events.shipping.ShippingEvent;

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

        // emit shipping scheduled event
        emitEvent(ShippingEvent.ShippingScheduled.builder().orderId(ordId)
        .expectedDeliveryDate(Instant.now())
        .build());

        // verify order details via REST endpoint
        // we might have to wait for sometime for streambridge to send and app to process
        Thread.sleep(1_500);

        verifyOrderDetails(ordId, odto -> {
            Assertions.assertNotNull(odto.order().deliveryDate());
            Assertions.assertEquals(OrderStatus.COMPLETED, odto.order().status());
            Assertions.assertEquals(PaymentStatus.DEDUCTED, odto.payment().status());
            Assertions.assertEquals(InventoryStatus.DEDUCTED, odto.inventory().status());
        });
    }

    @Test
    public void orderCancelWorkflowTest() throws InterruptedException{

        // simulate OrderCreateRequest sent from OrderController
        var req = TestDataUtil.toOrderCreateRequest(1, 1, 2, 3);
        
        // validate order in pending state
        var ordId = initiateOrder(req);

        // check for OrderCreated event
        verifyOrderCreatedEvent(ordId, 6);

        // simulate PaymentDeducted event sent from PaymentService
        emitEvent(PaymentEvent.PaymentDeducted.builder().orderId(ordId).build());

        // simulate InventoryFailed event sent from InventoryService
        emitEvent(InventoryEvent.InventoryFailed.builder().orderId(ordId).build());

        // check for OrderCancelled event
        verifyOrderCancelledEvent(ordId);

        // emit shipping scheduled event
        emitEvent(ShippingEvent.ShippingScheduled.builder().orderId(ordId)
        .expectedDeliveryDate(Instant.now())
        .build());

        // verify order details via REST endpoint
        // we might have to wait for sometime for streambridge to send and app to process
        Thread.sleep(1_500);

        //this.expectNoEvent();


        // order details should not have delivery date
        verifyOrderDetails(ordId, odto -> {
            Assertions.assertNull(odto.order().deliveryDate());
            Assertions.assertEquals(OrderStatus.CANCELLED, odto.order().status());
            Assertions.assertEquals(PaymentStatus.DEDUCTED, odto.payment().status());
            Assertions.assertEquals(InventoryStatus.DECLINED, odto.inventory().status());
        });
    }

    @Test
    public void compensatingTransactionTest() throws InterruptedException{

        // simulate OrderCreateRequest sent from OrderController
        var req = TestDataUtil.toOrderCreateRequest(1, 1, 2, 3);
        
        // validate order in pending state
        var ordId = initiateOrder(req);

        // check for OrderCreated event
        verifyOrderCreatedEvent(ordId, 6);

        // simulate Payment Declined event sent from PaymentService
        emitEvent(PaymentEvent.PaymentFailed.builder().orderId(ordId).build());

        // simulate InventoryDeducted event sent from InventoryService
        emitEvent(InventoryEvent.InventoryDeducted.builder().orderId(ordId).build());

        // check for OrderCancelled event
        verifyOrderCancelledEvent(ordId);

        // emit inventory restored event
        emitEvent(InventoryEvent.InventoryRestored.builder().orderId(ordId).build());

        // verify order details via REST endpoint
        // we might have to wait for sometime for streambridge to send and app to process
        Thread.sleep(1_500);

        //this.expectNoEvent();


        // order details should not have delivery date
        verifyOrderDetails(ordId, odto -> {
            Assertions.assertNull(odto.order().deliveryDate());
            Assertions.assertEquals(OrderStatus.CANCELLED, odto.order().status());
            Assertions.assertEquals(PaymentStatus.FAILED, odto.payment().status());
            Assertions.assertEquals(InventoryStatus.RESTORED, odto.inventory().status());
        });
    }
}
