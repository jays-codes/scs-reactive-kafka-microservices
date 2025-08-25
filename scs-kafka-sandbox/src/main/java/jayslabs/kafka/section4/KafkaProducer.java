package jayslabs.kafka.section4;

import java.time.Duration;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import reactor.core.publisher.Flux;

@Configuration
public class KafkaProducer {
    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);

//    @Bean
//    public SenderOptionsCustomizer customizer(){
//        return (s, so) -> so.producerProperty(ProducerConfig.ACKS_CONFIG, "all")
//                .producerProperty(ProducerConfig.BATCH_SIZE_CONFIG, "20001");
//    }

    @Bean
    public Supplier<Flux<Message<String>>> producer() {
        return () -> Flux.interval(Duration.ofSeconds(1))
            .take(5)
            .map(this::toMessage)
            .doOnNext(i -> log.info("Produced message: {}", i));
    }

    private Message<String> toMessage(long i){
        return MessageBuilder.withPayload("message-" + i)
            .setHeader(KafkaHeaders.KEY, "key-" + i)
            .setHeader("my-dummy-key", "dummy-key-" + i)
            .build();
    }
}
