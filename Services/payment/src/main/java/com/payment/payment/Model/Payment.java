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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "stripe_session_id", unique = true, nullable = true)
    private String stripeSessionId;

    @Column(name = "amount_in_cents", nullable = false)
    private Long amountInCents;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_purchased", nullable = false)
    private Plan planPurchased;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate = LocalDateTime.now();

    @Column(name = "metadata")
    private String metadata;
}
