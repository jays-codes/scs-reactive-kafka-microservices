package jayslabs.kafka.section11;

import org.apache.kafka.common.serialization.IntegerSerializer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestPropertySource;

import jayslabs.kafka.AbstractIntegrationTest;
import reactor.core.publisher.Flux;

@TestPropertySource(properties = {
    "sec=section11"
    //"spring.profiles.active=section8",  // ✅ Explicitly set active profile
    // "spring.cloud.function.definition=consumer",
    // "spring.cloud.stream.bindings.consumer-in-0.destination=input-topic"
})
public class EncodingDecodingTest extends AbstractIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(EncodingDecodingTest.class);

    @Test
    public void testEncodingDecoding() throws InterruptedException {

        var sender = this.<Integer,Integer>createSender(
            so -> 
                so
                .withKeySerializer(new IntegerSerializer())
                .withValueSerializer(new IntegerSerializer())
        );

        //emit values
        Flux.range(1, 3)
          .map(i -> this.toSenderRecord("input-topic", i, i))
          .as(sender::send)
          .doOnNext(sr -> log.info("result: {}", sr.correlationMetadata()))
          .subscribe();

        Thread.sleep(5_000);
    }
}
