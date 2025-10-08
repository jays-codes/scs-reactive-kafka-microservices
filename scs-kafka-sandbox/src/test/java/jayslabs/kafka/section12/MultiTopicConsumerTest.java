package jayslabs.kafka.section12;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;

import jayslabs.kafka.AbstractIntegrationTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 Objective: to test the multi topic consumer
    - Test consumer in section12 package
    - use producers defined below in TestConfig class, and configured as per TestPropertySource
 */

@TestPropertySource(properties = {
    "sec=section12",
    "spring.cloud.function.definition=consumer;producer1;producer2",
    "spring.cloud.stream.bindings.producer1-out-0.destination=input-topic1",
    "spring.cloud.stream.bindings.producer2-out-0.destination=input-topic2"
})
public class MultiTopicConsumerTest extends AbstractIntegrationTest{

    private static final Sinks.Many<String> sink1 = Sinks.many().multicast().onBackpressureBuffer();
    private static final Sinks.Many<String> sink2 = Sinks.many().multicast().onBackpressureBuffer();


    @Test
    public void testMultiTopicConsumer() throws InterruptedException {
        sink1.tryEmitNext("message 1");
        sink2.tryEmitNext("message 2");
        sink1.tryEmitNext("message 3");

        Thread.sleep(2_000);
    }

    @TestConfiguration
    static class TestConfig{

        @Bean
        public Supplier<Flux<String>> producer1(){
            return sink1::asFlux;
        }

        @Bean
        public Supplier<Flux<String>> producer2(){
            return sink2::asFlux;
        }



    }
}
