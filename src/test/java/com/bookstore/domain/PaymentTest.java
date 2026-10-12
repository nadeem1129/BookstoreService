package com.bookstore.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentTest {

    @Test
    void paymentStartsPendingAndCanSucceed() {
        Payment payment = payment();

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        payment.markSucceeded();
        assertEquals(PaymentStatus.SUCCEEDED, payment.getStatus());
    }

    @Test
    void pendingPaymentCanFailAndBeRetried() {
        Payment payment = payment();

        payment.markFailed();
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        payment.markPending();
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
    }

    @Test
    void successfulPaymentCannotBeRetriedOrFailed() {
        Payment payment = payment();
        payment.markSucceeded();

        assertThrows(IllegalStateException.class, payment::markPending);
        assertThrows(IllegalStateException.class, payment::markFailed);
    }

    @Test
    void onlyPendingPaymentCanTransitionToSucceeded() {
        Payment payment = payment();
        payment.markFailed();

        assertThrows(IllegalStateException.class, payment::markSucceeded);
    }

    private Payment payment() {
        return Payment.builder()
                .order(Order.builder().user(User.builder().id(1L).build()).build())
                .amount(new BigDecimal("19.99"))
                .build();
    }
}
