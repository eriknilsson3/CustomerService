package se.erik.customerservice.dto;

public record UpdateCustomerRequest(
        String firstName, String lastName, String email, String password, String phoneNumber) {
}
