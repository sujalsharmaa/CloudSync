package com.payment.payment.Model;

import com.payment.payment.Dto.Plan;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "payments")
@NoArgsConstructor
public class Payment {

    /**
     * Unique identifier for the payment transaction.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identifier for the user who made the payment.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * The ID provided by Stripe for the successful checkout session (cs_test_...).
     * This column is now set to nullable=true to allow saving the record before
     * the Stripe session is successfully created and the ID is known.
     */
    @Column(name = "stripe_session_id", unique = true, nullable = true)
    private String stripeSessionId;

    /**
     * The total amount paid in the smallest currency unit (e.g., cents for USD).
     * This matches the Long type used by Stripe's UnitAmount.
     */
    @Column(name = "amount_in_cents", nullable = false)
    private Long amountInCents;

    /**
     * The currency code (e.g., "USD").
     */
    @Column(name = "currency", length = 3, nullable = false)
    private String currency = "USD";

    /**
     * The subscription plan purchased by the user during this transaction.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "plan_purchased", nullable = false)
    private Plan planPurchased;

    /**
     * The current status of the payment (e.g., 'SUCCESS', 'PENDING', 'FAILED').
     */
    @Column(name = "status", nullable = false)
    private String status;

    /**
     * Timestamp when the payment transaction was recorded/completed.
     */
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate = LocalDateTime.now();

    /**
     * Optional: Any additional metadata from the payment processor.
     */
    @Column(name = "metadata")
    private String metadata;
}
