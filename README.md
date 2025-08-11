# scs-reactive-kafka-microservices
Jay's project/practice repo for Event-driven Microservices using Reactive Kafka and Spring Cloud Stream

#### proj scs-kafka-sandbox (jayaslabs.kafka; SpringBoot 3.5.4, jdk 21; Clud Stream, Spring for Apache Kafka, Lombok, spring-cloud-stream-binder-kafka-reactive)
- modified KafkaConsumer to add another function bean - function():Function<Flux<String>,Mono<Void>>; modified application.yaml: added binding for function(), and set spring.cloud.function.definition to use function
- pkg: kafka.section2: created KafkaConsumer (@Configuration) with consumer():Consumer<Flux<String>> (@Bean); defined bindings in application.yaml: spring.cloud.stream.bindings
- initial project commit; updated pom reference for spring-cloud-stream-binder-kafka-reactive; readme update

#### proj folder: kafka-setup
- added docker-compose.yaml to setup docker (image: vinsdocker/kafka), volumes references server.properties; added /data/ to gitignore

#### repo: scs-reactive-kafka-microservices