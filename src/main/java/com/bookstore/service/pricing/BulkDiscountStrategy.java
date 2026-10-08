package com.bookstore.service.pricing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class BulkDiscountStrategy implements DiscountStrategy {

    private final BigDecimal threshold;
    private final BigDecimal rate;

    public BulkDiscountStrategy(
            @Value("${app.pricing.bulk-discount.threshold:100.00}") BigDecimal threshold,
            @Value("${app.pricing.bulk-discount.rate:0.05}") BigDecimal rate) {
        this.threshold = threshold;
        this.rate = rate;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        if (subtotal == null || subtotal.compareTo(threshold) < 0) {
            return BigDecimal.ZERO;
        }
        return subtotal.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}