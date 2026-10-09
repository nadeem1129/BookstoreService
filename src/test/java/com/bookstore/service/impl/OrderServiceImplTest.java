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
    void checkoutDoesNotReturnAnotherUsersOrderForSameKey() {
        Long userId = 2L;
        String key = "checkout-123";
        User user = User.builder().id(userId).build();
        when(orderRepository.findByUserIdAndIdempotencyKey(userId, key))
                .thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cartService.getOrCreateCartEntity(userId)).thenReturn(new Cart(user));

        assertThrows(EmptyCartException.class, () -> orderService.checkout(userId, key));

        verify(orderRepository).findByUserIdAndIdempotencyKey(userId, key);
    }
}
