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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StripeService {

    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);

    private final PaymentRepository paymentRepository;
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
        Stripe.apiKey = secretKey;

        if (serviceRequest.getPlan() == null) {
            logger.info("Plan is required and must be one of: BASIC, PRO, TEAM.");
            return StripeResponse.builder()
                    .status("FAILED")
                    .message("Plan is required and must be one of: BASIC, PRO, TEAM.")
                    .build();
        }

        // Create a Payment Entity with PENDING status
        Payment payment = new Payment();
        payment.setUserId(Long.valueOf(jwt.getClaims().get("userId").toString()));
        payment.setAmountInCents(serviceRequest.getAmount());
        payment.setPlanPurchased(serviceRequest.getPlan());
        payment.setCurrency("USD");
        payment.setStatus("PENDING_SESSION_CREATION");
        payment.setTransactionDate(LocalDateTime.now());

        // Build Stripe Session Parameters
        SessionCreateParams.LineItem.PriceData.ProductData productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(String.valueOf(serviceRequest.getPlan()))
                        .build();

        SessionCreateParams.LineItem.PriceData priceData =
                SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency("USD")
                        .setUnitAmount(serviceRequest.getAmount())
                        .setProductData(productData)
                        .build();

        SessionCreateParams.LineItem lineItem =
                SessionCreateParams
                        .LineItem.builder()
                        .setQuantity(getQuantity(serviceRequest.getPlan()))
                        .setPriceData(priceData)
                        .build();

        // Save Payment record before calling Stripe
        try {
            payment = paymentRepository.save(payment);
        } catch (Exception e) {
            logger.error("Failed to save initial pending payment record.", e);
            return StripeResponse.builder()
                    .status("FAILED")
                    .message("Internal database error: " + e.getMessage())
                    .build();
        }

        // Create Stripe session with metadata
        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl("http://localhost:5173/paymentSuccess?session_id={CHECKOUT_SESSION_ID}")
                        .setCancelUrl("http://localhost:5173/paymentFailure")
                        .addLineItem(lineItem)
                        .putMetadata("local_payment_id", String.valueOf(payment.getId()))
                        .putMetadata("user_id", jwt.getClaims().get("userId").toString())
                        .putMetadata("username", jwt.getClaims().get("name").toString())
                        .putMetadata("email", jwt.getSubject())
                        .putMetadata("plan", serviceRequest.getPlan().toString())
                        .build();

        Session session = null;
        try {
            session = Session.create(params);

            // Update the entity with the Stripe Session ID
            payment.setStripeSessionId(session.getId());
            payment.setStatus("SENT_TO_STRIPE");
            paymentRepository.save(payment);

        } catch (StripeException e) {
            logger.error("Failed to create Stripe session.", e);
            payment.setStatus("FAILED_SESSION_CREATION");
            paymentRepository.save(payment);

            return StripeResponse
                    .builder()
                    .status("FAILED")
                    .message("Payment session could not be created: " + e.getMessage())
                    .build();
        }

        // ✅ REMOVED: handleSuccessfulPayment(session.getId());
        // ✅ REMOVED: queueService.publishPlanUpgradeEmailRequest(storageUpgradeNotification);

        // These will now be called ONLY from the webhook after payment is confirmed

        return StripeResponse
                .builder()
                .status("SUCCESS")
                .message("Payment session created")
                .sessionId(session.getId())
                .sessionUrl(session.getUrl())
                .build();
    }

    /**
     * Handles the successful completion of a Stripe Checkout Session.
     * This method should ONLY be called by the Webhook Controller when Stripe
     * sends a 'checkout.session.completed' event.
     *
     * @param stripeSessionId The ID of the successful Stripe Session.
     */
    public void handleSuccessfulPayment(String stripeSessionId) {
        // 1. Find the payment by session ID
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

        // 4. Retrieve user info from Stripe session metadata for email notification
        try {
            Stripe.apiKey = secretKey;
            Session session = Session.retrieve(stripeSessionId);

            String email = session.getMetadata().get("email");
            String username = session.getMetadata().get("username");
            String plan = payment.getPlanPurchased().toString();
            int storageGB = Math.toIntExact(getQuantity(payment.getPlanPurchased()));

            StorageUpgradeNotification notification = new StorageUpgradeNotification(
                    email,
                    username,
                    plan,
                    storageGB
            );
            queueService.publishPlanUpgradeEmailRequest(notification);

        } catch (StripeException e) {
            logger.error("Failed to retrieve session metadata for email notification", e);
            // Continue execution - plan upgrade was successful even if email fails
        }

        logger.info("Payment for session {} marked SUCCESS and plan upgrade event published for User ID {}",
                stripeSessionId, payment.getUserId());
    }
}