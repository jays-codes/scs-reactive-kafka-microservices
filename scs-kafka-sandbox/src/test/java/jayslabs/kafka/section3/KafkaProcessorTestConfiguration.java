// package jayslabs.kafka.section3;

// import java.util.concurrent.ConcurrentLinkedQueue;
// import java.util.function.Consumer;
// import java.util.function.Supplier;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.boot.test.context.TestConfiguration;
// import org.springframework.context.annotation.Bean;

// import reactor.core.publisher.Flux;

// /**
//  * Test configuration for KafkaProducerTest using ConcurrentLinkedQueue.
//  * Provides clean separation of concerns with thread-safe message capture.
//  */
// @TestConfiguration
// public class KafkaProcessorTestConfiguration {

//     private static final Logger log = LoggerFactory.getLogger(KafkaProcessorTestConfiguration.class);
    
//     // Thread-safe message collector - accessible from test class
//     public static final ConcurrentLinkedQueue<String> CAPTURED_MESSAGES = new ConcurrentLinkedQueue<>();

//     @Bean
//     public Consumer<Flux<String>> testConsumer() {
//         return flux -> flux
//             .doOnNext(message -> {
//                 log.info("Test consumer received: {}", message);
//                 CAPTURED_MESSAGES.offer(message); // Thread-safe message capture
//             })
//             .doOnError(error -> log.error("Error in test consumer", error))
//             .subscribe();
//     }

//     @Bean
//     public Supplier<Flux<String>> testProducer(){
//         return () -> Flux.just("Hello World 123")
//             .doOnNext(msg -> log.info("Test producer sending: {}", msg));
//     }
// }
