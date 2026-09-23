package se.erik.customerservice.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import se.erik.customerservice.dto.ChangePasswordRequest;
import se.erik.customerservice.dto.CreateCustomerRequest;
import se.erik.customerservice.dto.CustomerResponse;
import se.erik.customerservice.dto.UpdateCustomerRequest;
import se.erik.customerservice.service.CustomerService;

import java.util.List;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public List<CustomerResponse> getAllCustomers() {
        return customerService.getAllCustomers();
    }

    @GetMapping("/{id}")
    public CustomerResponse getCustomer(@PathVariable Long id) {
        return customerService.getCustomerById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        return customerService.createCustomer(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCustomer(
            @PathVariable Long id, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        customerService.deleteCustomer(id, authorizationHeader);
    }

    @PutMapping("/{id}")
    public CustomerResponse updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        return customerService.updateCustomer(id, request);
    }

    @PutMapping("/{id}/change-password")
    public void changePassword(@PathVariable Long id, @Valid @RequestBody ChangePasswordRequest request) {
        customerService.changePassword(id, request);
    }
}

