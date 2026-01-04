package com.payment.payment.Controller;

import com.payment.payment.Service.StripeService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionRetrieveParams;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stripe/webhook")
@RequiredArgsConstructor
public class StripeWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);
    private final StripeService stripeService;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            logger.error("Invalid Stripe webhook signature: {}", e.getMessage());
            return new ResponseEntity<>("Invalid signature", HttpStatus.BAD_REQUEST);
        }



        if ("checkout.session.completed".equals(event.getType())) {
            EventDataObjectDeserializer dataDeserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = dataDeserializer.getObject().orElse(null);

            if (stripeObject instanceof Session) {

                String sessionId = ((Session) stripeObject).getId();

                try {
                    SessionRetrieveParams params = SessionRetrieveParams.builder()
                            .addExpand("line_items")
                            .build();

                    Session session = Session.retrieve(sessionId, params, null);

                    logger.info("Payment succeeded for Session ID: {}", session.getId());

                    stripeService.handleSuccessfulPayment(session);

                } catch (StripeException e) {
                    logger.error("Stripe API error during session retrieval: {}", e.getMessage());
                    return new ResponseEntity<>("Stripe API Error", HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } else {
                logger.error("Deserialization failed or object is not a Session.");
            }
        }

        return new ResponseEntity<>("Received", HttpStatus.OK);
    }
}