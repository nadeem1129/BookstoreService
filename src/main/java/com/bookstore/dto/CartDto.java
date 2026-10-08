package com.bookstore.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartDto(
        Long id,
        List<CartItemDto> items,
        BigDecimal subtotal
) {
    public record CartItemDto(
            Long bookId,
            String title,
            String author,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal
    ){}
}
