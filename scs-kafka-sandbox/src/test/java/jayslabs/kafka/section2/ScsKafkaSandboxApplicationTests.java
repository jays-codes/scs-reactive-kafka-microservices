package jayslabs.kafka.section2;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;

@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@SpringBootTest
class ScsKafkaSandboxApplicationTests {

	@Autowired
	private EmbeddedKafkaBroker broker;

	@Test
	void contextLoads() {
	}

}
