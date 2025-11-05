package com.payment.payment.Controller;

import com.payment.payment.Service.StripeService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller to handle asynchronous events (webhooks) sent from Stripe.
 * This endpoint is intentionally UNAUTHENTICATED as Stripe servers don't send JWTs.
 */
@RestController
@RequestMapping("/stripe/webhook")
@RequiredArgsConstructor
public class StripeWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeService stripeService;

    // Add this property to your application.properties:
    // stripe.webhook.secret=whsec_your_webhook_secret_from_stripe
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        // 1. Verify the signature for security
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            logger.error("Invalid Stripe webhook signature: {}", e.getMessage());
            return new ResponseEntity<>("Invalid signature", HttpStatus.BAD_REQUEST);
        }

        // 2. Handle the specific event type
        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer()
                    .getObject()
                    .orElse(null);

            if (session != null) {
                logger.info("Received checkout.session.completed event for session ID: {}", session.getId());

                // 3. CALL THE SERVICE METHOD TO UPGRADE THE PLAN
                // This is where the actual upgrade happens after payment confirmation
                stripeService.handleSuccessfulPayment(session);
            } else {
                logger.error("checkout.session.completed event data object was null.");
            }
        }

        // Optionally handle other events like 'payment_intent.succeeded', etc.
        // Add more event handlers here if needed

        // Stripe expects a 200 OK response to confirm receipt of the event
        return new ResponseEntity<>("Received", HttpStatus.OK);
    }
}