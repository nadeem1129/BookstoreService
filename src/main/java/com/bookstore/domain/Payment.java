package com.bookstore.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor
public class Payment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Builder
    public Payment(Order order, BigDecimal amount) {
        this.order = order;
        this.amount = amount;
    }

    public void markPending() {
        if (status == PaymentStatus.SUCCEEDED) {
            throw new IllegalStateException("A successful payment cannot be retried");
        }
        status = PaymentStatus.PENDING;
    }

    public void markSucceeded() {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can succeed");
        }
        status = PaymentStatus.SUCCEEDED;
    }

    public void markFailed() {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can fail");
        }
        status = PaymentStatus.FAILED;
    }
}
