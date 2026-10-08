package com.bookstore.dto;

import com.bookstore.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDto(
        Long id,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt,
        List<OrderItemDto> items
) {
    public record OrderItemDto(
            Long bookId,
            String title,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal

    ) {
    }
}