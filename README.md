# scs-reactive-kafka-microservices
Jay's project/practice repo for Event-driven Microservices using Reactive Kafka and Spring Cloud Stream

#### proj scs-kafka-sandbox (jayaslabs.kafka; SpringBoot 3.5.4, jdk 21; Clud Stream, Spring for Apache Kafka, Lombok, spring-cloud-stream-binder-kafka-reactive)
- src/test/java: create AbstractIntegrationTest (AIT) base test class (uses @EmbeddedKafka, @SpringbootTest, EmbeddedKafkaBroker - @Autowired); created KafkaConsumerTest (extends AIT, @ExtendWith OutputCaptureExtension) with @Test method testKafkaConsumer(CapturedOutput) - uses: Mono, .delay(), .then(), .fromSupplier(), Duration, output::getOut, Duration, .as(), StepVerifier, consumeNextWith(); Inner class @TestConfiguration TestConfig with testProducer():Supplier<Flux<String>>
- prep workspace for integration test: removed dep:scs-test-binder, added SCSAppTest using @EmbeddedKafka
- added code for setting binding properties via @Bean via SenderOptionsCustomizer (deprecated)
- created KafkaProducer with producer():Supplier<Flux<String>>; modified app.yaml to add to scf.definition, scs.bindings (producer-out-0)
- modified app.yaml to set properties based on kafka.binding (function-0, consumer-in-0)
- added code for setting binding properties via @Bean via ReceiverOptionsCustomizer (deprecated)
- modified app.yaml to define binder specific properties: spring.cloud.stream.kafka.binder.<configuration/producer-properties/consumer-properties>, set "group.instance.id" var
- modified app.yaml to setup for active profiles, + application-section2.yaml, + application.yaml, modified sping app to use scanBasePackages appending active profile var ${sec}
- modified KafkaConsumer to add another function bean - function():Function<Flux<String>,Mono<Void>>; modified application.yaml: added binding for function(), and set spring.cloud.function.definition to use function
- pkg: kafka.section2: created KafkaConsumer (@Configuration) with consumer():Consumer<Flux<String>> (@Bean); defined bindings in application.yaml: spring.cloud.stream.bindings
- initial project commit; updated pom reference for spring-cloud-stream-binder-kafka-reactive; readme update

#### proj folder: kafka-setup
- added docker-compose.yaml to setup docker (image: vinsdocker/kafka), volumes r
eferences server.properties; added /data/ to gitignore

#### repo: scs-reactive-kafka-microservices