package com.bookstore.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record CartDto(
        Long id,
        List<CartItemDto> items,
        BigDecimal subtotal
) {
    @Builder
    public record CartItemDto(
            Long bookId,
            String title,
            String author,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal
    ){}
}
