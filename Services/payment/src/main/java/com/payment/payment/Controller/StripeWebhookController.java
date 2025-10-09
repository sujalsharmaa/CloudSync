//package com.payment.payment.Controller;
//
//import com.payment.payment.Service.StripeService;
//import com.stripe.Stripe;
//import com.stripe.exception.SignatureVerificationException;
//import com.stripe.model.Event;
//import com.stripe.model.checkout.Session;
//import com.stripe.net.Webhook;
//import lombok.RequiredArgsConstructor;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
///**
// * Controller to handle asynchronous events (webhooks) sent from Stripe.
// */
//@RestController
//@RequestMapping("/stripe/webhook")
//@RequiredArgsConstructor
//public class StripeWebhookController {
//
//    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);
//
//    private final StripeService stripeService;
//
//    // You would need to add this property to your application.properties/yaml
//    @Value("${stripe.webhook.secret}")
//    private String webhookSecret;
//
//    @PostMapping
//    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
//                                                      @RequestHeader("Stripe-Signature") String sigHeader) {
//
//        // Ensure Stripe API Key is set for potential API calls within the service
//        // Although the service itself likely sets it, it's good practice here.
//        // NOTE: The key is pulled from the Service, but we'll rely on the Stripe.apiKey
//        // set within StripeService for consistency, or inject it here as well.
//
//        Event event;
//
//        // 1. Verify the signature for security
//        try {
//            event = Webhook.constructEvent(
//                    payload, sigHeader, webhookSecret
//            );
//        } catch (SignatureVerificationException e) {
//            // Invalid signature
//            logger.error("Invalid Stripe webhook signature: {}", e.getMessage());
//            return new ResponseEntity<>("Invalid signature", HttpStatus.BAD_REQUEST);
//        }
//
//        // 2. Handle the specific event type
//        if ("checkout.session.completed".equals(event.getType())) {
//            Session session = (Session) event.getDataObjectDeserializer().getObject()
//                    .orElse(null);
//
//            if (session != null) {
//                logger.info("Received checkout.session.completed event for session ID: {}", session.getId());
//
//                // --- 3. CALL THE UNUSED SERVICE METHOD ---
//                // This is where handleSuccessfulPayment gets called, making it 'used'.
//                stripeService.handleSuccessfulPayment(session.getId());
//            } else {
//                logger.error("checkout.session.completed event data object was null.");
//            }
//        }
//
//        // Optionally handle other events like 'payment_intent.succeeded', etc.
//
//        // Stripe expects a 200 OK response to confirm receipt of the event
//        return new ResponseEntity<>("Received", HttpStatus.OK);
//    }
//}