package jayslabs.kafka.section11;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;

import jayslabs.kafka.AbstractIntegrationTest;
import jayslabs.kafka.section11.dto.ContactMethod;
import jayslabs.kafka.section11.dto.Email;
import jayslabs.kafka.section11.dto.Phone;
import reactor.core.publisher.Flux;

@TestPropertySource(properties = {
    "sec=section11",
    "spring.cloud.function.definition=consumer;cmProducer",
    "spring.cloud.stream.bindings.cmProducer-out-0.destination=input-topic",
    //"spring.cloud.stream.bindings.cmProducer-out-0.producer.useNativeEncoding=true",
    "spring.cloud.stream.kafka.bindings.cmProducer-out-0.producer.configuration.value.serializer=org.springframework.kafka.support.serializer.JsonSerializer"
    //"spring.profiles.active=section8",  // ✅ Explicitly set active profile
    // "spring.cloud.function.definition=consumer",
    // "spring.cloud.stream.bindings.consumer-in-0.destination=input-topic"
})
public class EncodingDecodingTest extends AbstractIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(EncodingDecodingTest.class);

    
    // @Test
    // public void testEncodingDecoding() throws InterruptedException {

    //     var sender = this.<Integer,Integer>createSender(
    //         so -> 
    //             so
    //             .withKeySerializer(new IntegerSerializer())
    //             .withValueSerializer(new IntegerSerializer())
    //     );

    //     //emit values
    //     Flux.range(1, 3)
    //       .map(i -> this.toSenderRecord("input-topic", i, i))
    //       .as(sender::send)
    //       .doOnNext(sr -> log.info("result: {}", sr.correlationMetadata()))
    //       .subscribe();

    //     Thread.sleep(5_000);
    // }

    @Test
    public void testEncodingDecodingForAbstractTypes() throws InterruptedException {



        Thread.sleep(1_000);
    }

    @TestConfiguration
    static class TestConfig{

        @Bean
        public Supplier<Flux<ContactMethod>> cmProducer(){
            
            return () -> Flux.just(
                new Email("test@test.com"), 
                new Phone(14167));
        }



    }
}
