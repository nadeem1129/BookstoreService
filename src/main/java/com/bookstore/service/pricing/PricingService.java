package com.bookstore.service.pricing;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PricingService {

    private final DiscountStrategy discountStrategy;

    public PricingService(DiscountStrategy discountStrategy) {
        this.discountStrategy = discountStrategy;
    }

    public BigDecimal calculateTotal(BigDecimal subtotal) {
        BigDecimal discount = discountStrategy.calculateDiscount(subtotal);
        return subtotal.subtract(discount);
    }
}