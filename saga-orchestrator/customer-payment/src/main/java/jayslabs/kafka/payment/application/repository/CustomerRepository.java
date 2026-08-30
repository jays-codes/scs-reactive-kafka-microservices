package jayslabs.kafka.payment.application.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import jayslabs.kafka.payment.application.entity.Customer;

@Repository
public interface CustomerRepository extends ReactiveCrudRepository<Customer, Integer> {

}
