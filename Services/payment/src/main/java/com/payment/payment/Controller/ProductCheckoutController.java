package com.payment.payment.Controller;

import com.payment.payment.Dto.ServiceRequest;
import com.payment.payment.Dto.StripeResponse;
import com.payment.payment.Service.StripeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/service/v1")
@RequiredArgsConstructor
public class ProductCheckoutController {

    private final StripeService stripeService;

    @PostMapping("/checkout")
    public ResponseEntity<StripeResponse> checkoutProducts(@RequestBody ServiceRequest serviceRequest, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaims().get("userId").toString();
        StripeResponse stripeResponse = stripeService.checkoutProducts(serviceRequest,userId);
        System.out.println(serviceRequest);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(stripeResponse);
    }
}