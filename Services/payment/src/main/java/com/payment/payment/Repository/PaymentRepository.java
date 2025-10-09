package com.payment.payment.Repository;

import com.payment.payment.Model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional; // Required for returning Optional

/**
 * Repository interface for managing Payment entities.
 * Extends JpaRepository to provide standard CRUD operations.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Finds a Payment entity by the unique Stripe Session ID.
     * This method is crucial for webhook handling to link the Stripe event
     * back to the local database record.
     *
     * @param stripeSessionId The ID provided by Stripe for the checkout session.
     * @return An Optional containing the Payment entity if found.
     */
    Optional<Payment> findByStripeSessionId(String stripeSessionId);
}
