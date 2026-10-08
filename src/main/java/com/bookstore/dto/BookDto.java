package com.bookstore.dto;

import java.math.BigDecimal;

public record BookDto(
        Long id,
        String title,
        String author,
        String isbn,
        String description,
        BigDecimal price,
        int stock
) {
}
