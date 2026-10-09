package com.bookstore.service.impl;

import com.bookstore.domain.Book;
import com.bookstore.domain.Cart;
import com.bookstore.domain.Order;
import com.bookstore.domain.OrderStatus;
import com.bookstore.domain.User;
import com.bookstore.dto.OrderDto;
import com.bookstore.exception.DuplicateResourceException;
import com.bookstore.exception.EmptyCartException;
import com.bookstore.exception.InsufficientStockException;
import com.bookstore.exception.PaymentFailedException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.mapper.DtoMapper;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.service.CartService;
import com.bookstore.service.OrderService;
import com.bookstore.service.order.OrderFactory;
import com.bookstore.service.payment.PaymentProcessor;
import com.bookstore.service.pricing.PricingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final OrderFactory orderFactory;
    private final PricingService pricingService;
    private final PaymentProcessor paymentProcessor;

    public OrderServiceImpl(OrderRepository orderRepository,
                            BookRepository bookRepository,
                            UserRepository userRepository,
                            CartService cartService,
                            OrderFactory orderFactory,
                            PricingService pricingService,
                            PaymentProcessor paymentProcessor) {
        this.orderRepository = orderRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.cartService = cartService;
        this.orderFactory = orderFactory;
        this.pricingService = pricingService;
        this.paymentProcessor = paymentProcessor;
    }

    @Override
    @Transactional
    public OrderDto checkout(Long userId, String idempotencyKey) {
        if (idempotencyKey != null && ! idempotencyKey.isBlank()) {
            Order existing = orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey).orElse(null);
            if (existing != null) {
                log.info("ORDER_CHECKOUT_IDEMPOTENT_HIT userId={} orderId={} key={}",
                        userId, existing.getId(), idempotencyKey);
                return DtoMapper.toOrderDto(existing);

            }
        }

        User user = requireUser(userId);
        Cart cart = cartService.getOrCreateCartEntityForUpdate(userId);

        // Recheck after taking the cart lock so concurrent retries can return the committed order.
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Order existing = orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey).orElse(null);
            if (existing != null) {
                log.info("ORDER_CHECKOUT_IDEMPOTENT_HIT userId={} orderId={} key={}",
                        userId, existing.getId(), idempotencyKey);
                return DtoMapper.toOrderDto(existing);
            }
        }

        if (cart.isEmpty()) {
            throw new EmptyCartException();
        }

        List<Long> bookIds = cart.getItems().stream()
                .map(item -> item.getBook().getId())
                .toList();



// Acquires PESSIMISTIC_WRITE locks - the heart of oversell prevention.
        Map<Long, Book> lockedBooks = OrderFactory.indexById(
                bookRepository.findAllByIdForUpdate(bookIds));

        cart.getItems().forEach(item -> {
            Book book = lockedBooks.get(item.getBook().getId());
            if (book == null) {
                throw new ResourceNotFoundException("Book", item.getBook().getId());
            }

            if (!book.hasStock(item.getQuantity())) {
                throw new InsufficientStockException(
                        book.getTitle(), item.getQuantity(), book.getStock());

            }
            book.reduceStock(item.getQuantity());


        });

        Order order = orderFactory.createOrder(user, cart, lockedBooks);
        order.setTotalAmount(pricingService.calculateTotal(order.getTotalAmount()));
        order.setIdempotencyKey(idempotencyKey);



        try {
            Order saved = orderRepository.saveAndFlush(order);
            cartService.clearCart(userId);
            log.info("ORDER_CHECKOUT_SUCCESS userId={} orderId={} total={} lineItems={}",
                    userId, saved.getId(), saved.getTotalAmount(), saved.getItems().size());
            return DtoMapper.toOrderDto(saved);
        } catch (DataIntegrityViolationException ex) {
       // A concurrent request with the same idempotency key committed first.
            log.warn("ORDER_CHECKOUT_DUPLICATE_KEY userId={} key={}", userId, idempotencyKey);
            throw new DuplicateResourceException(
                    "A checkout with this idempotency key has already been processed");
        }
    }

    @Override
    @Transactional
    public OrderDto pay(Long userId, Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Order", orderId);
        }
        if (order.getStatus() == OrderStatus.PAID) {
            return DtoMapper.toOrderDto(order);
        }
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new IllegalStateException("Order cannot be paid from status " + order.getStatus());
        }
        if (!paymentProcessor.processPayment(order)) {
            throw new PaymentFailedException(orderId);
        }

        order.markPaid();
        log.info("ORDER_PAYMENT_SUCCESS userId={} orderId={} total={}",
                userId, order.getId(), order.getTotalAmount());
        return DtoMapper.toOrderDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getOrdersForUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(DtoMapper::toOrderDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (!order.getUser().getId().equals(userId)) {
            // Do not disclose existence of another user's order.
            throw new ResourceNotFoundException("Order", orderId);
        }
        return DtoMapper.toOrderDto(order);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

    }

}