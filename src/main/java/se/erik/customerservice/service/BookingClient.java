package se.erik.customerservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import se.erik.customerservice.error.ServiceUnavailableException;

@Service
public class BookingClient {

    private final RestClient restClient;

    public BookingClient(
            RestClient.Builder builder,
            @Value("${booking.service.base-url}") String bookingServiceUrl
    ) {
        this.restClient = builder
                .baseUrl(bookingServiceUrl)
                .build();
    }

    public boolean hasActiveBookings(Long customerId, String authorizationHeader) {

        try {
            Boolean result = restClient.get()
                    .uri("/bookings/customer/{customerId}/active", customerId)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(Boolean.class);

            return Boolean.TRUE.equals(result);

        } catch (RestClientException e) {
            throw new ServiceUnavailableException(
                    "Booking service is unavailable"
            );
        }
    }
}