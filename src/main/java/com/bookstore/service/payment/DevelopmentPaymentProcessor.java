package com.bookstore.service.payment;

import com.bookstore.domain.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
@Slf4j
public class DevelopmentPaymentProcessor implements PaymentProcessor {

    @Override
    public boolean processPayment(Order order) {
        log.warn("PAYMENT_STUB_APPROVED orderId={} amount={} - no real payment was collected",
                order.getId(), order.getTotalAmount());
        return true;
    }
}
