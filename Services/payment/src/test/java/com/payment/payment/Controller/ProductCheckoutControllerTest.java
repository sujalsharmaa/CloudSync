package com.payment.payment.Controller;

import com.payment.payment.Dto.Plan;
import com.payment.payment.Dto.ServiceRequest;
import com.payment.payment.Dto.StripeResponse;
import com.payment.payment.Service.StripeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCheckoutControllerTest {

    @Mock
    private StripeService stripeService;

    @InjectMocks
    private ProductCheckoutController checkoutController;

    @Test
    void checkoutProducts_ShouldReturnStripeUrl_WhenRequestIsValid() {

        ServiceRequest request = new ServiceRequest();
        request.setPlan(Plan.valueOf("PRO"));
        request.setAmount(1000L); // $10.00
        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getClaims()).thenReturn(Map.of("userId", "user-123"));
        StripeResponse mockStripeResponse = StripeResponse.builder()
                .status("SUCCESS")
                .sessionId("sess_12345")
                .sessionUrl("https://checkout.stripe.com/pay/sess_12345")
                .build();

        when(stripeService.checkoutProducts(request, mockJwt)).thenReturn(mockStripeResponse);


        ResponseEntity<StripeResponse> response = checkoutController.checkoutProducts(request, mockJwt);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SUCCESS", response.getBody().getStatus());
        assertEquals("https://checkout.stripe.com/pay/sess_12345", response.getBody().getSessionUrl());
    }
}