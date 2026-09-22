package se.erik.customerservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import se.erik.customerservice.controller.CustomerController;
import se.erik.customerservice.dto.CustomerResponse;
import se.erik.customerservice.security.JwTService;
import se.erik.customerservice.security.LoginController;
import se.erik.customerservice.service.CustomerService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
public class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private JwTService jwtService;

    @MockitoBean
    private LoginController loginController;


    @Test
    void createCustomer_shouldReturnCreatedCustomer()
            throws Exception {

        CustomerResponse response =
                new CustomerResponse(
                        1L,
                        "Marcus",
                        "Viklund",
                        "marcus.viklund@gmail.com",
                        "123456789"
                );

        when(customerService.createCustomer(any()))
                .thenReturn(response);

        String requestBody = """
            {
                "firstName": "Marcus",
                "lastName": "Viklund",
                "email": "marcus.viklund@gmail.com",
                "phoneNumber": "123456789",
                "password": "qwerty123"
            }
            """;

        mockMvc.perform(
                        post("/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(
                        jsonPath("$.firstName")
                                .value("Marcus")
                )
                .andExpect(
                        jsonPath("$.lastName")
                                .value("Viklund")
                )
                .andExpect(
                        jsonPath("$.email")
                                .value(
                                        "marcus.viklund@gmail.com"
                                )
                )
                .andExpect(
                        jsonPath("$.phoneNumber")
                                .value("123456789")
                );
    }

    @Test
    void updateCustomer_shouldReturnUpdatedCustomer()
            throws Exception {

        Long customerId = 1L;

        CustomerResponse response =
                new CustomerResponse(
                        customerId,
                        "Dagobert",
                        "Thorsten",
                        "marcus.viklund@gmail.com",
                        "987654321"
                );

        when(
                customerService.updateCustomer(
                        eq(customerId),
                        any()
                )
        ).thenReturn(response);

        String requestBody = """
            {
                "firstName": "Dagobert",
                "lastName": "Thorsten",
                "phoneNumber": "987654321"
            }
            """;

        mockMvc.perform(
                        put("/customers/" + customerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(
                        jsonPath("$.firstName")
                                .value("Dagobert")
                )
                .andExpect(
                        jsonPath("$.lastName")
                                .value("Thorsten")
                )
                .andExpect(
                        jsonPath("$.phoneNumber")
                                .value("987654321")
                );
    }

    @Test
    void deleteCustomer_shouldReturnNoContent()
            throws Exception {

        Long customerId = 1L;

        doNothing()
                .when(customerService)
                .deleteCustomer(
                        eq(customerId),
                        anyString()
                );

        mockMvc.perform(
                        delete("/customers/" + customerId)
                                .header(
                                        "Authorization",
                                        "Bearer test-token"
                                )
                )
                .andExpect(status().isNoContent());

        verify(customerService)
                .deleteCustomer(
                        eq(customerId),
                        anyString()
                );
    }
}