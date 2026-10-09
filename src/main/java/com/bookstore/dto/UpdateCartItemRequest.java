package com.bookstore.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record UpdateCartItemRequest(
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at lease 1")
        Integer quantity
) {
}
