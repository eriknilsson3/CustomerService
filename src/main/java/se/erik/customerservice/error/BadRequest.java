package se.erik.customerservice.error;


public class BadRequest extends RuntimeException {
    public BadRequest(String message) {
        super(message);
    }
}