package com.bookstore.service.impl;

import com.bookstore.domain.Cart;
import com.bookstore.domain.Order;
import com.bookstore.domain.User;
import com.bookstore.exception.EmptyCartException;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.service.CartService;
import com.bookstore.service.order.OrderFactory;
import com.bookstore.service.pricing.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceImplTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final CartService cartService = mock(CartService.class);
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                orderRepository,
                mock(BookRepository.class),
                userRepository,
                cartService,
                mock(OrderFactory.class),
                mock(PricingService.class));
    }

    @Test
    void checkoutReturnsExistingOrderForSameUserAndKey() {
        Long userId = 1L;
        String key = "checkout-123";
        Order existingOrder = new Order();
        existingOrder.setId(42L);
        when(orderRepository.findByUserIdAndIdempotencyKey(userId, key))
                .thenReturn(Optional.of(existingOrder));

        var result = orderService.checkout(userId, key);

        assertEquals(42L, result.id());
        verify(orderRepository).findByUserIdAndIdempotencyKey(userId, key);
    }

    @Test
    void checkoutWithSameKeyAndNoExistingOrderLocksCartBeforeProcessing() {
        Long userId = 2L;
        String key = "checkout-123";
        User user = User.builder().id(userId).build();
        when(orderRepository.findByUserIdAndIdempotencyKey(userId, key))
                .thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cartService.getOrCreateCartEntityForUpdate(userId)).thenReturn(new Cart(user));

        assertThrows(EmptyCartException.class, () -> orderService.checkout(userId, key));

        verify(orderRepository, times(2)).findByUserIdAndIdempotencyKey(userId, key);
        verify(cartService).getOrCreateCartEntityForUpdate(userId);
    }

    @Test
    void checkoutWithoutIdempotencyKeyStillLocksCart() {
        Long userId = 2L;
        User user = User.builder().id(userId).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cartService.getOrCreateCartEntityForUpdate(userId)).thenReturn(new Cart(user));

        assertThrows(EmptyCartException.class, () -> orderService.checkout(userId, null));

        verify(cartService).getOrCreateCartEntityForUpdate(userId);
    }

    @Test
    void concurrentRetryReturnsOrderFoundAfterCartLock() {
        Long userId = 3L;
        String key = "checkout-456";
        User user = User.builder().id(userId).build();
        Order existingOrder = new Order();
        existingOrder.setId(43L);
        when(orderRepository.findByUserIdAndIdempotencyKey(userId, key))
                .thenReturn(Optional.empty(), Optional.of(existingOrder));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cartService.getOrCreateCartEntityForUpdate(userId)).thenReturn(new Cart(user));

        var result = orderService.checkout(userId, key);

        assertEquals(43L, result.id());
        verify(orderRepository, times(2)).findByUserIdAndIdempotencyKey(userId, key);
        verify(cartService).getOrCreateCartEntityForUpdate(userId);
    }
}
