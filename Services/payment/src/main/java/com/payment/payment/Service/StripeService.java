package com.payment.payment.Service;

import com.payment.payment.Dto.*;
import com.payment.payment.Model.Payment;
import com.payment.payment.Repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional; // Import for Optional

@Service
@RequiredArgsConstructor // Automatically injects final fields
public class StripeService {

    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);

    // Inject the new PaymentRepository (from previous step)
    private final PaymentRepository paymentRepository;

    // Inject the QueueService for Kafka publishing
    private final QueueService queueService;

    @Value("${stripe.secretKey}")
    private String secretKey;

    public long getQuantity(Plan plan){
        int Quantity = 0;
        switch (plan){
            case PRO -> Quantity=1000;
            case BASIC -> Quantity=100;
            case TEAM -> Quantity=5000;
            default -> Quantity=1;
        }
        return Quantity;
    }

    public StripeResponse checkoutProducts(ServiceRequest serviceRequest, Jwt jwt) {
        // Set your secret key. Remember to switch to your live secret key in production!
        Stripe.apiKey = secretKey;
        if (serviceRequest.getPlan() == null) {
            logger.info("Plan is required and must be one of: BASIC, PRO, TEAM.");
            return StripeResponse.builder()
                    .status("FAILED")
                    .message("Plan is required and must be one of: BASIC, PRO, TEAM.")
                    .build();
        }

        // --- 1. Create a Payment Entity with PENDING status for initial tracking ---
        // NOTE: Replace this hardcoded ID (1L) with the actual authenticated user's ID.


        Payment payment = new Payment();
        payment.setUserId(Long.valueOf(jwt.getClaims().get("userId").toString()));
        payment.setAmountInCents(serviceRequest.getAmount());
        // FIX: Ensure planPurchased is set to prevent DataIntegrityViolationException
        payment.setPlanPurchased(serviceRequest.getPlan()); // <-- FIX APPLIED HERE
        payment.setCurrency("USD");
        payment.setStatus("PENDING_SESSION_CREATION");
        payment.setTransactionDate(LocalDateTime.now());

        // --- 2. Build Stripe Session Parameters (Uses a helper variable for quantity) ---

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
                        .setQuantity(getQuantity(serviceRequest.getPlan()))
                        .setPriceData(priceData)
                        .build();

        // --- 3. Save Payment record before calling Stripe to ensure we have a local ID ---
        try {
            payment = paymentRepository.save(payment);
        } catch (Exception e) {
            logger.error("Failed to save initial pending payment record.", e);
            return StripeResponse.builder().status("FAILED").message("Internal database error: " + e.getMessage()).build();
        }

        // Create new session with the line items
        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        // Append CHECKOUT_SESSION_ID to success URL for post-payment processing
                        .setSuccessUrl("http://localhost:5173/paymentSuccess?session_id={CHECKOUT_SESSION_ID}")
                        .setCancelUrl("http://localhost:5173/paymentFailure")
                        .addLineItem(lineItem)
                        // Add metadata to link Stripe session back to our Payment record
                        .putMetadata("local_payment_id", String.valueOf(payment.getId()))
                        .putMetadata("user_id", jwt.getClaims().get("userId").toString())
                        .build();

        // --- 4. Create Stripe session and update Payment record ---
        Session session = null;
        try {
            session = Session.create(params);

            // Update the entity with the Stripe Session ID and change status to SENT_TO_STRIPE
            payment.setStripeSessionId(session.getId());
            payment.setStatus("SENT_TO_STRIPE");
            paymentRepository.save(payment);

        } catch (StripeException e) {
            logger.error("Failed to create Stripe session.", e);
            // If Stripe creation fails, mark the local payment record as FAILED and save
            payment.setStatus("FAILED_SESSION_CREATION");
            paymentRepository.save(payment);

            return StripeResponse
                    .builder()
                    .status("FAILED")
                    .message("Payment session could not be created: " + e.getMessage())
                    .build();
        }
        handleSuccessfulPayment(session.getId());

        StorageUpgradeNotification storageUpgradeNotification = new StorageUpgradeNotification(
                jwt.getSubject(),jwt.getClaims().get("name").toString(),
                serviceRequest.getPlan().toString(), Math.toIntExact(getQuantity(Plan.valueOf(serviceRequest.getPlan().toString()))),java.time.Year.now().toString()
        );
        queueService.publishPlanUpgradeEmailRequest(storageUpgradeNotification);

        return StripeResponse
                .builder()
                .status("SUCCESS")
                .message("Payment session created ")
                .sessionId(session.getId())
                .sessionUrl(session.getUrl())
                .build();
    }

    /**
     * Handles the successful completion of a Stripe Checkout Session, updating the local
     * payment status and publishing a Kafka event to upgrade the user's plan.
     * This method is typically called by a Webhook Controller reacting to stripe's
     * 'checkout.session.completed' event.
     *
     * @param stripeSessionId The ID of the successful Stripe Session.
     */
    public void handleSuccessfulPayment(String stripeSessionId) {
        // 1. Find the payment by session ID
        // NOTE: This assumes PaymentRepository has findByStripeSessionId(String sessionId) defined.
        Optional<Payment> paymentOpt = paymentRepository.findByStripeSessionId(stripeSessionId);

        if (paymentOpt.isEmpty()) {
            logger.error("Attempted to handle successful payment for unknown session ID: {}", stripeSessionId);
            return;
        }

        Payment payment = paymentOpt.get();

        if ("SUCCESS".equals(payment.getStatus())) {
            logger.warn("Payment for session ID {} already processed as SUCCESS. Skipping.", stripeSessionId);
            return;
        }

        // 2. Update Payment Status to SUCCESS
        payment.setStatus("SUCCESS");
        payment.setTransactionDate(LocalDateTime.now());
        paymentRepository.save(payment);

        // 3. Publish Plan Upgrade Event to Kafka
        PlanUpgradeDto upgradeDto = new PlanUpgradeDto(
                payment.getUserId(),
                payment.getPlanPurchased()
        );
        queueService.publishPlanUpgradeInfo(upgradeDto);

        logger.info("Payment for session {} marked SUCCESS and plan upgrade event published for User ID {}", stripeSessionId, payment.getUserId());
    }
}
