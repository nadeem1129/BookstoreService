package com.bookstore.service.impl;

import com.bookstore.domain.Cart;
import com.bookstore.dto.CartDto;
import com.bookstore.exception.InsufficientStockException;
import com.bookstore.service.CartService;
import org.springframework.stereotype.Service;

import com.bookstore.domain.Book;
import com.bookstore.domain.User;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.mapper.DtoMapper;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CartRepository;
import com.bookstore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class CartServiceImpl implements CartService {

    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final int maxQuantityPerItem;

    public CartServiceImpl(CartRepository cartRepository,
                           UserRepository userRepository,
                           BookRepository bookRepository,
                           @Value("${app.cart.max-quantity-per-item:50}") int maxQuantityPerItem){
        this.cartRepository=cartRepository;
        this.userRepository =userRepository;
        this.bookRepository =bookRepository;
        this.maxQuantityPerItem = maxQuantityPerItem;

    }

    @Override
    @Transactional(readOnly = true)
    public CartDto getCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> new Cart(requireUser(userId)));
        return DtoMapper.toCartDto(cart);
    }

    @Override
    public CartDto addToCart(Long userId, Long bookId, int quantity) {
        Cart cart = getOrCreateCartEntity(userId);
        Book book = requireBook(bookId);

        int alreadyInCart = cart.findItemByBook(bookId).map(i -> i.getQuantity()).orElse(0);
        int desiredTotal = alreadyInCart + quantity;
        validateQuantityBounds(desiredTotal);
        if (!book.hasStock(desiredTotal)) {
            throw new InsufficientStockException(book.getTitle(), desiredTotal, book.getStock());
        }

        cart.addItem(book, quantity);
        Cart saved = cartRepository.save(cart);
        log.info("ADD_TO_CART userId={} bookId={} quantity={} cartsubtotal={}",
                userId, bookId, quantity, saved.getsubtotal());
        return DtoMapper.toCartDto(saved);
    }

    @Override
    public CartDto updateItemQuantity(Long userId, Long bookId, int quantity) {
        Cart cart = requireCart(userId);
        Book book = requireBook(bookId);
        validateQuantityBounds(quantity);
        if (!book.hasStock(quantity)) {
            throw new InsufficientStockException(book.getTitle(), quantity, book.getStock());
        }

        cart.updateItemQuantity(bookId, quantity);
        Cart saved = cartRepository.save(cart);
        log.info("UPDATE_CART_ITEM userId={} bookId={} quantity={}", userId, bookId, quantity);
        return DtoMapper.toCartDto(saved);
    }

    @Override
    public CartDto removeItem(Long userId, Long bookId) {
        Cart cart = requireCart(userId);
        cart.removeItem(bookId);
        Cart saved = cartRepository.save(cart);
        log.info("REMOVE_CART_ITEM userId={} bookId={}", userId, bookId);
        return DtoMapper.toCartDto(saved);
    }


    @Override
    public Cart getOrCreateCartEntity(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(new Cart(requireUser(userId))));

    }

    @Override
    public Cart getOrCreateCartEntityForUpdate(Long userId) {
        return cartRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> cartRepository.save(new Cart(requireUser(userId))));
    }

    private void validateQuantityBounds(int quantity) {
        if (quantity > maxQuantityPerItem) {
            throw new IllegalArgumentException(
                    "Quantity per item cannot exceed " + maxQuantityPerItem);
        }
    }

    @Override
    public void clearCart(Long userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cart.clear();
            cartRepository.save(cart);
            log.info("CLEAR_CART userId={}", userId);

        });
    }

    private Cart requireCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart for user", userId));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private Book requireBook(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", bookId));
    }

}