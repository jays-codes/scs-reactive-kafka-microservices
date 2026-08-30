package jayslabs.kafka.payment.application.entity;


import org.springframework.data.annotation.Id;

import lombok.Data;

@Data
public class Customer {
    
    @Id
    private Integer id;
    private String name;
    private Integer balance;
}
