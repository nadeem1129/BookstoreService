package com.bookstore.service;

import com.bookstore.domain.Cart;
import com.bookstore.dto.CartDto;

public interface CartService {

    CartDto getCart(Long userId);

    CartDto addToCart(Long userId, Long bookId, int quantity);

    CartDto updateItemQuantity(Long userId, Long bookId, int quantity);

    CartDto removeItem(Long userId, Long bookId);

    Cart getOrCreateCartEntity(Long userId);

    void clearCart(Long userId);
}
