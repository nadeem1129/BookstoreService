package com.bookstore.service.payment;

import com.bookstore.domain.Order;

public interface PaymentProcessor {
    boolean processPayment(Order order);
}
