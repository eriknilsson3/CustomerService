package se.erik.customerservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import se.erik.customerservice.dto.CreateCustomerRequest;
import se.erik.customerservice.dto.CustomerResponse;
import se.erik.customerservice.dto.UpdateCustomerRequest;
import se.erik.customerservice.error.BadRequest;
import se.erik.customerservice.error.ConflictException;
import se.erik.customerservice.error.NotFoundException;
import se.erik.customerservice.error.ServiceUnavailableException;
import se.erik.customerservice.model.Customer;
import se.erik.customerservice.repository.CustomerRepo;
import se.erik.customerservice.service.BookingClient;
import se.erik.customerservice.service.CustomerService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepo customerRepo;

    @Mock
    private BookingClient bookingClient;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void createCustomer_shouldCreateCustomerSuccessfully() {

        CreateCustomerRequest request =
                new CreateCustomerRequest(
                        "Alice",
                        "Andersson",
                        "alice@example.com",
                        "0701234567",
                        "password123"
                );

        when(customerRepo.existsByEmail(request.email()))
                .thenReturn(false);

        when(passwordEncoder.encode(request.password()))
                .thenReturn("encoded-password");

        when(customerRepo.save(any(Customer.class)))
                .thenAnswer(invocation -> {

                    Customer customer =
                            invocation.getArgument(0);

                    customer.setId(1L);

                    return customer;
                });

        CustomerResponse result =
                customerService.createCustomer(request);

        assertEquals(1L, result.id());
        assertEquals("Alice", result.firstName());
        assertEquals("Andersson", result.lastName());
        assertEquals("alice@example.com", result.email());
        assertEquals("0701234567", result.phoneNumber());

        verify(passwordEncoder)
                .encode("password123");

        verify(customerRepo)
                .save(any(Customer.class));
    }

    @Test
    void createCustomer_shouldRejectDuplicateEmail() {

        CreateCustomerRequest request =
                new CreateCustomerRequest(
                        "Alice",
                        "Andersson",
                        "alice@example.com",
                        "0701234567",
                        "password123"
                );

        when(customerRepo.existsByEmail(request.email()))
                .thenReturn(true);

        assertThrows(
                BadRequest.class,
                () -> customerService.createCustomer(request)
        );

        verify(customerRepo, never())
                .save(any(Customer.class));
    }

    @Test
    void getCustomerById_shouldReturnCustomer() {

        Customer customer = new Customer(
                "Alice",
                "Andersson",
                "alice@example.com",
                "encoded-password",
                "0701234567"
        );

        customer.setId(1L);

        when(customerRepo.findById(1L))
                .thenReturn(Optional.of(customer));

        CustomerResponse result =
                customerService.getCustomerById(1L);

        assertEquals(1L, result.id());
        assertEquals("Alice", result.firstName());
        assertEquals("Andersson", result.lastName());
        assertEquals(
                "alice@example.com",
                result.email()
        );
    }

    @Test
    void getCustomerById_shouldThrowWhenCustomerDoesNotExist() {

        when(customerRepo.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> customerService.getCustomerById(999L)
        );
    }

    @Test
    void updateCustomer_shouldUpdateCustomerSuccessfully() {

        Customer customer = new Customer(
                "OldFirstName",
                "OldLastName",
                "alice@example.com",
                "encoded-password",
                "0700000000"
        );

        customer.setId(1L);

        when(customerRepo.findById(1L))
                .thenReturn(Optional.of(customer));

        when(customerRepo.save(any(Customer.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        UpdateCustomerRequest request =
                new UpdateCustomerRequest(
                        "NewFirstName",
                        "NewLastName",
                        null,
                        null,
                        "0709999999"
                );

        CustomerResponse result =
                customerService.updateCustomer(
                        1L,
                        request
                );

        assertEquals(
                "NewFirstName",
                result.firstName()
        );

        assertEquals(
                "NewLastName",
                result.lastName()
        );

        assertEquals(
                "0709999999",
                result.phoneNumber()
        );

        verify(customerRepo)
                .save(customer);
    }

    @Test
    void deleteCustomer_shouldRejectCustomerWithActiveBooking() {

        Customer customer = new Customer(
                "Alice",
                "Andersson",
                "alice@example.com",
                "encoded-password",
                "0701234567"
        );

        customer.setId(1L);

        when(customerRepo.findById(1L))
                .thenReturn(Optional.of(customer));

        when(
                bookingClient.hasActiveBookings(
                        eq(1L),
                        anyString()
                )
        ).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> customerService.deleteCustomer(
                        1L,
                        "Bearer test-token"
                )
        );

        verify(customerRepo, never())
                .delete(any(Customer.class));
    }

    @Test
    void deleteCustomer_shouldDeleteCustomerWithoutActiveBooking() {

        Customer customer = new Customer(
                "Alice",
                "Andersson",
                "alice@example.com",
                "encoded-password",
                "0701234567"
        );

        customer.setId(1L);

        when(customerRepo.findById(1L))
                .thenReturn(Optional.of(customer));

        when(
                bookingClient.hasActiveBookings(
                        eq(1L),
                        anyString()
                )
        ).thenReturn(false);

        customerService.deleteCustomer(
                1L,
                "Bearer test-token"
        );

        verify(customerRepo)
                .delete(customer);
    }

    @Test
    void deleteCustomer_shouldPropagateBookingServiceUnavailable() {

        Customer customer = new Customer(
                "Alice",
                "Andersson",
                "alice@example.com",
                "encoded-password",
                "0701234567"
        );

        customer.setId(1L);

        when(customerRepo.findById(1L))
                .thenReturn(Optional.of(customer));

        when(
                bookingClient.hasActiveBookings(
                        eq(1L),
                        anyString()
                )
        ).thenThrow(
                new ServiceUnavailableException(
                        "Booking service is unavailable"
                )
        );

        assertThrows(
                ServiceUnavailableException.class,
                () -> customerService.deleteCustomer(
                        1L,
                        "Bearer test-token"
                )
        );

        verify(customerRepo, never())
                .delete(any(Customer.class));
    }
}