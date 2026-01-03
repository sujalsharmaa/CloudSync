package com.payment.payment.Service;

import com.payment.payment.Dto.Plan;
import com.payment.payment.Dto.PlanUpgradeDto;
import com.payment.payment.Dto.ServiceRequest;
import com.payment.payment.Dto.StorageUpgradeNotification;
import com.payment.payment.Dto.StripeResponse;
import com.payment.payment.Exception.BusinessException;
import com.payment.payment.Model.Payment;
import com.payment.payment.Repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StripeService {

    @Value("${redirect_url_success}")
    private String redirectURL;

    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);
    private final PaymentRepository paymentRepository;
    private final QueueService queueService;

    @Value("${stripe.secretKey}")
    private String secretKey;

    public long getQuantity(Plan plan) {
        int quantity = 0;
        switch (plan) {
            case PRO -> quantity = 1000;
            case BASIC -> quantity = 100;
            case TEAM -> quantity = 5000;
            default -> quantity = 1;
        }
        return quantity;
    }

    @CircuitBreaker(name = "stripeService", fallbackMethod = "checkoutFallback")
    public StripeResponse checkoutProducts(ServiceRequest serviceRequest, Jwt jwt) {
        Stripe.apiKey = secretKey;

        if (serviceRequest.getPlan() == null) {
            logger.error("Plan is required.");
            // Throw BusinessException to be handled by GlobalExceptionHandler
            throw new BusinessException("Plan is required and must be one of: BASIC, PRO, TEAM.");
        }

        Payment payment = new Payment();
        payment.setUserId(Long.valueOf(jwt.getClaims().get("userId").toString()));
        payment.setAmountInCents(serviceRequest.getAmount());
        payment.setPlanPurchased(serviceRequest.getPlan());
        payment.setCurrency("USD");
        payment.setStatus("PENDING_SESSION_CREATION");
        payment.setTransactionDate(LocalDateTime.now());

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
                SessionCreateParams.LineItem.builder()
                        .setQuantity(getQuantity(serviceRequest.getPlan()))
                        .setPriceData(priceData)
                        .build();

        try {
            payment = paymentRepository.save(payment);
        } catch (Exception e) {
            logger.error("Failed to save initial pending payment record.", e);
            // Throw generic BusinessException or a custom DatabaseException
            throw new BusinessException("Internal error: Failed to initiate payment record.");
        }

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(redirectURL + "paymentSuccess?session_id={CHECKOUT_SESSION_ID}")
                        .setCancelUrl(redirectURL + "paymentFailure")
                        .addLineItem(lineItem)
                        .putMetadata("local_payment_id", String.valueOf(payment.getId()))
                        .putMetadata("user_id", jwt.getClaims().get("userId").toString())
                        .putMetadata("username", jwt.getClaims().get("name").toString())
                        .putMetadata("email", jwt.getSubject())
                        .putMetadata("plan", serviceRequest.getPlan().toString())
                        .build();

        try {
            Session session = Session.create(params);
            payment.setStripeSessionId(session.getId());
            payment.setStatus("SENT_TO_STRIPE");
            paymentRepository.save(payment);

            return StripeResponse.builder()
                    .status("SUCCESS")
                    .message("Payment session created")
                    .sessionId(session.getId())
                    .sessionUrl(session.getUrl())
                    .build();

        } catch (StripeException e) {
            logger.error("Failed to create Stripe session.", e);
            payment.setStatus("FAILED_SESSION_CREATION");
            paymentRepository.save(payment);

            // Throw BusinessException to return clean error JSON
            throw new BusinessException("Payment session could not be created: " + e.getMessage());
        }
    }

    public StripeResponse checkoutFallback(ServiceRequest serviceRequest, Jwt jwt, Throwable t) {
        logger.error("Circuit Breaker: Stripe service is currently unavailable or failing. Reason: {}", t.getMessage());
        throw new BusinessException("Payment service is temporarily unavailable. Please try again later.");
    }

    // handleSuccessfulPayment remains the same...
    public void handleSuccessfulPayment(Session session) {
        String stripeSessionId = session.getId();
        Optional<Payment> paymentOpt = paymentRepository.findByStripeSessionId(stripeSessionId);

        if (paymentOpt.isEmpty()) {
            logger.error("Attempted to handle successful payment for unknown session ID: {}", stripeSessionId);
            // Since this is a webhook/internal call, logging is usually sufficient,
            // but you could throw ResourceNotFoundException if you wanted to NACK the webhook.
            return;
        }

        Payment payment = paymentOpt.get();
        if ("SUCCESS".equals(payment.getStatus())) {
            return;
        }

        payment.setStatus("SUCCESS");
        payment.setTransactionDate(LocalDateTime.now());
        paymentRepository.save(payment);

        PlanUpgradeDto upgradeDto = new PlanUpgradeDto(payment.getUserId(), payment.getPlanPurchased());
        queueService.publishPlanUpgradeInfo(upgradeDto);

        try {
            Stripe.apiKey = secretKey;
            String email = session.getMetadata().get("email");
            String username = session.getMetadata().get("username");
            String plan = payment.getPlanPurchased().toString();
            int storageGB = Math.toIntExact(getQuantity(payment.getPlanPurchased()));

            StorageUpgradeNotification notification = new StorageUpgradeNotification(email, username, plan, storageGB);
            queueService.publishPlanUpgradeEmailRequest(notification);
        } catch (Exception e) {
            logger.error("Failed to retrieve session metadata for email notification", e);
        }
    }
}