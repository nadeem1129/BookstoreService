package com.bookstore.service.pricing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BulkDiscountStrategyTest {

    private final BulkDiscountStrategy strategy = new BulkDiscountStrategy(
            new BigDecimal("100.00"),
            new BigDecimal("0.05"));

    @Test
    void returnsNoDiscountBelowThreshold() {
        assertEquals(BigDecimal.ZERO, strategy.calculateDiscount(new BigDecimal("99.99")));
    }

    @Test
    void appliesDiscountAtThreshold() {
        assertEquals(new BigDecimal("5.00"), strategy.calculateDiscount(new BigDecimal("100.00")));
    }

    @Test
    void roundsDiscountToTwoPlacesHalfUp() {
        BulkDiscountStrategy lowThresholdStrategy = new BulkDiscountStrategy(
                BigDecimal.ZERO,
                new BigDecimal("0.05"));

        assertEquals(new BigDecimal("0.06"),
                lowThresholdStrategy.calculateDiscount(new BigDecimal("1.10")));
    }

    @Test
    void returnsNoDiscountForNullSubtotal() {
        assertEquals(BigDecimal.ZERO, strategy.calculateDiscount(null));
    }
}
