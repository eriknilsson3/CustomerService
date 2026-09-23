package se.erik.customerservice.service;

import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import se.erik.customerservice.dto.ChangePasswordRequest;
import se.erik.customerservice.dto.CreateCustomerRequest;
import se.erik.customerservice.dto.CustomerResponse;
import se.erik.customerservice.dto.UpdateCustomerRequest;
import se.erik.customerservice.error.BadRequest;
import se.erik.customerservice.error.ConflictException;
import se.erik.customerservice.error.NotFoundException;
import se.erik.customerservice.model.Customer;
import se.erik.customerservice.repository.CustomerRepo;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepo customerRepo;
    private final BookingClient bookingClient;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(CustomerRepo customerRepo, BookingClient bookingClient, PasswordEncoder passwordEncoder) {
        this.customerRepo = customerRepo;
        this.bookingClient = bookingClient;
        this.passwordEncoder = passwordEncoder;
    }

    public List<CustomerResponse> getAllCustomers() {
        return customerRepo.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public CustomerResponse getCustomerById(Long id) {
        Customer customer = customerRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer with id " + id + " not found"));

        return toDTO(customer);
    }

    ublic CustomerResponse createCustomer(CreateCustomerRequest request) {

        if (customerRepo.existsByEmail(request.email())) {
            throw new BadRequest("Customer with email " + request.email() + " already exists");
        }

        Customer customer = new Customer();
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setEmail(request.email());
        customer.setPhoneNumber(request.phoneNumber());
        customer.setPasswordHash(passwordEncoder.encode(request.password()));

        try {
            Customer saved = customerRepo.save(customer);
            return toDTO(saved);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequest("Customer with email " + request.email() + " already exists");
        }
    }

    public void deleteCustomer(Long id, String authorizationHeader) {

        Customer customer = customerRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer with id " + id + " not found"));

        boolean hasBookings = bookingClient.hasActiveBookings(id, authorizationHeader);

        if (hasBookings) {
            throw new ConflictException("Customer widh id " +id+ " has active booking, cannot delete");
        }
        customerRepo.delete(customer);
    }

    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request) {

        Customer customer = customerRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer with id " + id + " not found"));

        if (request.firstName() != null) customer.setFirstName(request.firstName());
        if (request.lastName() != null) customer.setLastName(request.lastName());
        if (request.phoneNumber() != null) customer.setPhoneNumber(request.phoneNumber());

        if (request.email() != null) {
            if (!request.email().equals(customer.getEmail()) &&
                    customerRepo.existsByEmail(request.email())) {
                throw new BadRequest("Customer with email " + request.email() + " already exists");
            }
            customer.setEmail(request.email());
        }

        if (request.password() != null) {
            customer.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        try {
            Customer saved = customerRepo.save(customer);
            return toDTO(saved);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequest("Could not update customer");
        }
    }

    private CustomerResponse toDTO(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getPhoneNumber()
        );
    }

    public void changePassword(Long id, ChangePasswordRequest request) {
        Customer customer = customerRepo.findById(id).orElseThrow(()
                -> new NotFoundException("Customer with id " + id + " not found"));

        if (!passwordEncoder.matches(
                request.currentPassword(),
                customer.getPasswordHash()
        )) {
            throw new BadRequest("Current password doesn't match");
        }

        customer.setPasswordHash(passwordEncoder.encode(request.newPassword()));

        customerRepo.save(customer);
    }
}
