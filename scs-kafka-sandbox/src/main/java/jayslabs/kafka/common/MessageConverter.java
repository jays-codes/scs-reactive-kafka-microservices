package jayslabs.kafka.common;

import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import reactor.kafka.receiver.ReceiverOffset;

public class MessageConverter {

    public static <T> CustomRecord<T> toRecord(Message<T> message){
        var payload = message.getPayload();
        var key = message.getHeaders().get(KafkaHeaders.RECEIVED_KEY, String.class);
        var ack = message.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT, ReceiverOffset.class);

        return new CustomRecord<>(key, payload, ack);
    }
}
