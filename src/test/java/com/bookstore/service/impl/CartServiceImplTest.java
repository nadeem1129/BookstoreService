package com.bookstore.service.impl;

import com.bookstore.domain.Book;
import com.bookstore.domain.Cart;
import com.bookstore.domain.Role;
import com.bookstore.domain.User;
import com.bookstore.exception.InsufficientStockException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CartRepository;
import com.bookstore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class CartServiceImplTest {

    private final CartRepository cartRepository = mock(CartRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final BookRepository bookRepository = mock(BookRepository.class);
    private CartServiceImpl cartService;
    private User user;
    private Book book;
    private Cart cart;

    @BeforeEach
    void setUp() {
        cartService = new CartServiceImpl(cartRepository, userRepository, bookRepository, 5);
        user = User.builder()
                .id(1L)
                .username("reader")
                .email("reader@example.com")
                .password("encoded")
                .role(Role.USER)
                .build();
        book = Book.builder()
                .id(10L)
                .title("Test book")
                .author("Author")
                .price(new BigDecimal("12.50"))
                .stock(10)
                .build();
        cart = new Cart(user);
    }

    @Test
    void addToCartCreatesCartAndAddsItem() {
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = cartService.addToCart(user.getId(), book.getId(), 2);

        assertEquals(2, result.items().get(0).quantity());
        assertEquals(new BigDecimal("25.00"), result.subtotal());
        verify(cartRepository, times(2)).save(any(Cart.class));
    }

    @Test
    void addToCartMergesQuantitiesForTheSameBook() {
        cart.addItem(book, 2);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(cartRepository.save(cart)).thenReturn(cart);

        var result = cartService.addToCart(user.getId(), book.getId(), 1);

        assertEquals(1, result.items().size());
        assertEquals(3, result.items().get(0).quantity());
        assertEquals(new BigDecimal("37.50"), result.subtotal());
    }

    @Test
    void addToCartRejectsRequestExceedingAvailableStockWithoutSaving() {
        book.setStock(2);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));

        assertThrows(InsufficientStockException.class,
                () -> cartService.addToCart(user.getId(), book.getId(), 3));

        verify(cartRepository, never()).save(any(Cart.class));
        assertEquals(0, cart.getItems().size());
    }

    @Test
    void addToCartRejectsQuantityAboveConfiguredLimit() {
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));

        assertThrows(IllegalArgumentException.class,
                () -> cartService.addToCart(user.getId(), book.getId(), 6));

        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void updateQuantityChecksStockBeforeChangingCart() {
        cart.addItem(book, 2);
        book.setStock(2);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));

        assertThrows(InsufficientStockException.class,
                () -> cartService.updateItemQuantity(user.getId(), book.getId(), 3));

        assertEquals(2, cart.getItems().get(0).getQuantity());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void updateQuantityPersistsValidQuantity() {
        cart.addItem(book, 2);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(cartRepository.save(cart)).thenReturn(cart);

        var result = cartService.updateItemQuantity(user.getId(), book.getId(), 4);

        assertEquals(4, result.items().get(0).quantity());
        assertEquals(new BigDecimal("50.00"), result.subtotal());
        verify(cartRepository).save(cart);
    }

    @Test
    void updateQuantityRejectsUnknownCartItem() {
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));

        assertThrows(IllegalArgumentException.class,
                () -> cartService.updateItemQuantity(user.getId(), book.getId(), 1));
    }

    @Test
    void getCartFailsForUnknownUserWhenNoCartExists() {
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cartService.getCart(user.getId()));
    }

    @Test
    void clearCartRemovesItemsAndPersistsCart() {
        cart.addItem(book, 2);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(cartRepository.save(cart)).thenReturn(cart);

        cartService.clearCart(user.getId());

        assertEquals(0, cart.getItems().size());
        verify(cartRepository).save(cart);
    }

    @Test
    void removeItemPersistsUpdatedCart() {
        cart.addItem(book, 1);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(cartRepository.save(cart)).thenReturn(cart);

        var result = cartService.removeItem(user.getId(), book.getId());

        assertEquals(0, result.items().size());
        assertEquals(BigDecimal.ZERO, result.subtotal());
        verify(cartRepository).save(cart);
    }
}
