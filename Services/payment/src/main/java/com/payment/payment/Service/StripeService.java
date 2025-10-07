package com.payment.payment.Service;

import com.payment.payment.Dto.ServiceRequest;
import com.payment.payment.Dto.StripeResponse;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class StripeService {

    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);

    @Value("${stripe.secretKey}")
    private String secretKey;

    public long getQuantity(ServiceRequest serviceRequest){
        int Quantity = 0;
        switch (serviceRequest.getPlan()){
            case PRO -> Quantity=1000;
            case BASIC -> Quantity=100;
            case TEAM -> Quantity=5000;
            case DEFAULT -> Quantity=1;
            case null, default -> Quantity=1;
        }
        return Quantity;
    }

    public StripeResponse checkoutProducts(ServiceRequest serviceRequest) {
        // Set your secret key. Remember to switch to your live secret key in production!
        Stripe.apiKey = secretKey;

        // Create a PaymentIntent with the order amount and currency
        SessionCreateParams.LineItem.PriceData.ProductData productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(String.valueOf(serviceRequest.getPlan()))
                        .build();

        // Create new line item with the above product data and associated price
        SessionCreateParams.LineItem.PriceData priceData =
                SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency("USD")
                        .setUnitAmount(serviceRequest.getAmount())
                        .setProductData(productData)
                        .build();

        // Create new line item with the above price data
        SessionCreateParams.LineItem lineItem =
                SessionCreateParams
                        .LineItem.builder()
                        .setQuantity(getQuantity(serviceRequest))
                        .setPriceData(priceData)
                        .build();

        // Create new session with the line items
        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl("http://localhost:5173/paymentSuccess")
                        .setCancelUrl("http://localhost:5173/paymentFailure")
                        .addLineItem(lineItem)
                        .build();

        // Create new session
        Session session = null;
        try {
            session = Session.create(params);
        } catch (StripeException e) {
            logger.error("Failed to create Stripe session.", e);
        }

        if (session == null) {
            return StripeResponse
                    .builder()
                    .status("FAILED")
                    .message("Payment session could not be created")
                    .build();
        }

        return StripeResponse
                .builder()
                .status("SUCCESS")
                .message("Payment session created ")
                .sessionId(session.getId())
                .sessionUrl(session.getUrl())
                .build();
    }
}
