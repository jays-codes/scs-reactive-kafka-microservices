package jayslabs.kafka.common;

import reactor.kafka.receiver.ReceiverOffset;

public record CustomRecord<T>(
    String key, 
    T message, 
    ReceiverOffset acknowledgement) {

}
