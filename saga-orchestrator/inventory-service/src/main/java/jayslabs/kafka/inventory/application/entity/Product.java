package jayslabs.kafka.inventory.application.entity;

import org.springframework.data.annotation.Id;

import lombok.Data;

@Data
public class Product {

    @Id
    private Integer id;
    private String description;
    private Integer availableQuantity;
}
