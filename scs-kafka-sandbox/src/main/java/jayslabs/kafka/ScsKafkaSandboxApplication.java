package jayslabs.kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "jayslabs.kafka.${spring.profiles.active}")
public class ScsKafkaSandboxApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScsKafkaSandboxApplication.class, args);
	}

}
