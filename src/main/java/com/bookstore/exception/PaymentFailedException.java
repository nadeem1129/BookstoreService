package com.bookstore.exception;

public class PaymentFailedException extends RuntimeException {
    public PaymentFailedException(Long orderId) {
        super("Payment was declined for order " + orderId);
    }
}
