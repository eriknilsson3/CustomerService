package se.erik.customerservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.erik.customerservice.model.Customer;

import java.util.Optional;

public interface CustomerRepo extends JpaRepository<Customer, Long> {
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    Optional<Customer> findByEmail(String email);
}
