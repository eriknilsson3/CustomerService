package se.erik.customerservice.security;


import jakarta.validation.Valid;
import se.erik.customerservice.dto.CreateCustomerRequest;
import se.erik.customerservice.dto.CustomerResponse;
import se.erik.customerservice.dto.LoginRequest;
import se.erik.customerservice.dto.LoginResponse;
import se.erik.customerservice.error.BadRequest;
import se.erik.customerservice.model.Customer;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import se.erik.customerservice.repository.CustomerRepo;
import se.erik.customerservice.service.CustomerService;

@RestController
@RequestMapping("/auth")
public class LoginController {

    private final CustomerRepo customerRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwTService jwTService;
    private final CustomerService customerService;

    public LoginController(CustomerRepo customerRepo, PasswordEncoder passwordEncoder, JwTService jwTService, CustomerService customerService) {
        this.customerRepo = customerRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwTService = jwTService;
        this.customerService = customerService;
    }

    @Valid
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        Customer customer = customerRepo
                .findByEmail(request.email()).orElseThrow(() -> new BadRequest("Invalid email or passowrd"));

        boolean passwordMatches = passwordEncoder.matches(request.password(), customer.getPasswordHash());

        if (!passwordMatches) {
            throw new BadRequest("Invalid email or password");
        }
        String token = jwTService.generateToken(customer.getId(),customer.getEmail());

        return new LoginResponse(token);
    }

    @Valid
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse registerCustomer(@RequestBody CreateCustomerRequest request) {
        return customerService.createCustomer(request);
    }
}
