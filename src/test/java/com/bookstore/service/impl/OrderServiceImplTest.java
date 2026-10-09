package com.bookstore.service.impl;

import com.bookstore.domain.Book;
import com.bookstore.domain.Cart;
import com.bookstore.domain.Order;
import com.bookstore.domain.OrderStatus;
import com.bookstore.domain.User;
import com.bookstore.exception.EmptyCartException;
import com.bookstore.exception.InsufficientStockException;
import com.bookstore.exception.PaymentFailedException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.service.CartService;
import com.bookstore.service.order.OrderFactory;
import com.bookstore.service.payment.PaymentProcessor;
import com.bookstore.service.pricing.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class OrderServiceImplTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final BookRepository bookRepository = mock(BookRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final CartService cartService = mock(CartService.class);
    private final PaymentProcessor paymentProcessor = mock(PaymentProcessor.class);
    private final PricingService pricingService = mock(PricingService.class);
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                orderRepository,
                bookRepository,
                userRepository,
                cartService,
                new OrderFactory(),
                pricingService,
                paymentProcessor);
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
    void checkoutCreatesOrderWithoutProcessingPayment() {
        Long userId = 8L;
        User user = User.builder().id(userId).build();
        Book book = Book.builder()
                .id(48L)
                .title("Test book")
                .price(new BigDecimal("25.00"))
                .stock(10)
                .build();
        Cart cart = new Cart(user);
        cart.addItem(book, 1);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cartService.getOrCreateCartEntityForUpdate(userId)).thenReturn(cart);
        when(bookRepository.findAllByIdForUpdate(List.of(book.getId()))).thenReturn(List.of(book));
        when(pricingService.calculateTotal(new BigDecimal("25.00"))).thenReturn(new BigDecimal("25.00"));
        when(orderRepository.saveAndFlush(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = orderService.checkout(userId, null);

        assertEquals(OrderStatus.CREATED, result.status());
        verify(cartService).clearCart(userId);
        org.mockito.Mockito.verifyNoInteractions(paymentProcessor);
    }

    @Test
    void checkoutWithInsufficientStockDoesNotCreateOrderOrClearCart() {
        Long userId = 9L;
        User user = User.builder().id(userId).build();
        Book book = Book.builder()
                .id(49L)
                .title("Limited book")
                .price(new BigDecimal("15.00"))
                .stock(1)
                .build();
        Cart cart = new Cart(user);
        cart.addItem(book, 2);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cartService.getOrCreateCartEntityForUpdate(userId)).thenReturn(cart);
        when(bookRepository.findAllByIdForUpdate(List.of(book.getId()))).thenReturn(List.of(book));

        assertThrows(InsufficientStockException.class, () -> orderService.checkout(userId, "stock-failure"));

        assertEquals(1, book.getStock());
        assertEquals(1, cart.getItems().size());
        verify(orderRepository, never()).saveAndFlush(any(Order.class));
        verify(cartService, never()).clearCart(userId);
        org.mockito.Mockito.verifyNoInteractions(paymentProcessor);
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

    @Test
    void successfulExplicitPaymentMarksOrderPaid() {
        Long userId = 4L;
        Long orderId = 44L;
        Order order = Order.builder().user(User.builder().id(userId).build()).build();
        order.setId(orderId);
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(paymentProcessor.processPayment(order)).thenReturn(true);

        var result = orderService.pay(userId, orderId);

        assertEquals(OrderStatus.PAID, result.status());
        verify(paymentProcessor).processPayment(order);
    }

    @Test
    void failedPaymentLeavesOrderUnpaid() {
        Long userId = 5L;
        Long orderId = 45L;
        Order order = Order.builder().user(User.builder().id(userId).build()).build();
        order.setId(orderId);
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(paymentProcessor.processPayment(order)).thenReturn(false);

        assertThrows(PaymentFailedException.class, () -> orderService.pay(userId, orderId));

        assertEquals(OrderStatus.CREATED, order.getStatus());
    }

    @Test
    void alreadyPaidOrderDoesNotChargeAgain() {
        Long userId = 6L;
        Long orderId = 46L;
        Order order = Order.builder().user(User.builder().id(userId).build()).build();
        order.setId(orderId);
        order.markPaid();
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        var result = orderService.pay(userId, orderId);

        assertEquals(OrderStatus.PAID, result.status());
        org.mockito.Mockito.verifyNoInteractions(paymentProcessor);
    }

    @Test
    void cancelledOrderCannotBePaid() {
        Long userId = 7L;
        Long orderId = 47L;
        Order order = Order.builder().user(User.builder().id(userId).build()).build();
        order.setId(orderId);
        order.cancel();
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> orderService.pay(userId, orderId));

        org.mockito.Mockito.verifyNoInteractions(paymentProcessor);
    }

    @Test
    void paymentCannotBeAttemptedByAnotherUser() {
        Long orderId = 48L;
        Order order = Order.builder().user(User.builder().id(100L).build()).build();
        order.setId(orderId);
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        assertThrows(ResourceNotFoundException.class, () -> orderService.pay(101L, orderId));

        org.mockito.Mockito.verifyNoInteractions(paymentProcessor);
    }

    @Test
    void getOrderHidesOrdersOwnedByAnotherUser() {
        Long orderId = 49L;
        Order order = Order.builder().user(User.builder().id(200L).build()).build();
        order.setId(orderId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrder(201L, orderId));
    }

    @Test
    void getOrderReturnsNotFoundForMissingOrder() {
        when(orderRepository.findById(500L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrder(202L, 500L));
    }
}
