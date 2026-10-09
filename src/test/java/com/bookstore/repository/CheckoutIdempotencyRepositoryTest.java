package com.bookstore.repository;

import com.bookstore.domain.Book;
import com.bookstore.domain.Cart;
import com.bookstore.domain.Order;
import com.bookstore.domain.Role;
import com.bookstore.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
class CheckoutIdempotencyRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void sameIdempotencyKeyCanBeUsedByDifferentUsers() {
        User firstUser = createUser("first");
        User secondUser = createUser("second");
        orderRepository.saveAndFlush(orderFor(firstUser, "shared-key"));
        orderRepository.saveAndFlush(orderFor(secondUser, "shared-key"));

        assertTrue(orderRepository.findByUserIdAndIdempotencyKey(firstUser.getId(), "shared-key").isPresent());
        assertTrue(orderRepository.findByUserIdAndIdempotencyKey(secondUser.getId(), "shared-key").isPresent());
        assertTrue(orderRepository.findByUserIdAndIdempotencyKey(firstUser.getId(), "other-user-key").isEmpty());
    }

    @Test
    void duplicateIdempotencyKeyIsRejectedForSameUser() {
        User user = createUser("duplicate");
        orderRepository.saveAndFlush(orderFor(user, "unique-per-user"));

        assertThrows(DataIntegrityViolationException.class,
                () -> orderRepository.saveAndFlush(orderFor(user, "unique-per-user")));
    }

    @Test
    void cartCanBeFetchedWithWriteLockForCheckout() {
        User user = createUser("cart-lock");
        Cart cart = cartRepository.saveAndFlush(new Cart(user));

        Cart lockedCart = cartRepository.findByUserIdForUpdate(user.getId()).orElseThrow();

        assertEquals(cart.getId(), lockedCart.getId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void secondCheckoutWaitsForFirstTransactionToReleaseCartLock() throws Exception {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        User user = transactionTemplate.execute(status -> createUser("concurrent"));
        Cart cart = transactionTemplate.execute(status -> cartRepository.saveAndFlush(new Cart(user)));
        java.util.concurrent.CountDownLatch firstHasLock = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch releaseFirst = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch secondStarted = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch secondHasLock = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);

        try {
            java.util.concurrent.Future<?> firstCheckout = executor.submit(() ->
                    transactionTemplate.executeWithoutResult(status -> {
                        cartRepository.findByUserIdForUpdate(user.getId()).orElseThrow();
                        firstHasLock.countDown();
                        await(releaseFirst);
                    }));
            assertTrue(firstHasLock.await(5, java.util.concurrent.TimeUnit.SECONDS));

            java.util.concurrent.Future<?> secondCheckout = executor.submit(() ->
                    transactionTemplate.executeWithoutResult(status -> {
                        secondStarted.countDown();
                        cartRepository.findByUserIdForUpdate(user.getId()).orElseThrow();
                        secondHasLock.countDown();
                    }));
            assertTrue(secondStarted.await(5, java.util.concurrent.TimeUnit.SECONDS));
            assertFalse(secondHasLock.await(200, java.util.concurrent.TimeUnit.MILLISECONDS));

            releaseFirst.countDown();
            firstCheckout.get(5, java.util.concurrent.TimeUnit.SECONDS);
            secondCheckout.get(5, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(secondHasLock.await(5, java.util.concurrent.TimeUnit.SECONDS));
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }

        assertEquals(cart.getId(), cartRepository.findByUserId(user.getId()).orElseThrow().getId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentStockReservationsCannotOversell() throws Exception {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        Book book = transactionTemplate.execute(status -> bookRepository.saveAndFlush(Book.builder()
                .title("Limited stock")
                .author("Author")
                .price(java.math.BigDecimal.TEN)
                .stock(1)
                .build()));
        java.util.concurrent.CountDownLatch firstHasLock = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch releaseFirst = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch secondStarted = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch secondFinished = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);

        try {
            java.util.concurrent.Future<Boolean> firstReservation = executor.submit(() ->
                    transactionTemplate.execute(status -> {
                        Book lockedBook = bookRepository.findAllByIdForUpdate(java.util.List.of(book.getId()))
                                .get(0);
                        boolean available = lockedBook.hasStock(1);
                        if (available) {
                            lockedBook.reduceStock(1);
                            bookRepository.saveAndFlush(lockedBook);
                        }
                        firstHasLock.countDown();
                        await(releaseFirst);
                        return available;
                    }));
            assertTrue(firstHasLock.await(5, java.util.concurrent.TimeUnit.SECONDS));

            java.util.concurrent.Future<Boolean> secondReservation = executor.submit(() -> {
                secondStarted.countDown();
                try {
                    return transactionTemplate.execute(status -> {
                        Book lockedBook = bookRepository.findAllByIdForUpdate(java.util.List.of(book.getId()))
                                .get(0);
                        boolean available = lockedBook.hasStock(1);
                        if (available) {
                            lockedBook.reduceStock(1);
                            bookRepository.saveAndFlush(lockedBook);
                        }
                        return available;
                    });
                } finally {
                    secondFinished.countDown();
                }
            });
            assertTrue(secondStarted.await(5, java.util.concurrent.TimeUnit.SECONDS));
            assertFalse(secondFinished.await(200, java.util.concurrent.TimeUnit.MILLISECONDS));

            releaseFirst.countDown();
            assertTrue(firstReservation.get(5, java.util.concurrent.TimeUnit.SECONDS));
            assertFalse(secondReservation.get(5, java.util.concurrent.TimeUnit.SECONDS));
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }

        assertEquals(0, bookRepository.findById(book.getId()).orElseThrow().getStock());
    }

    private User createUser(String suffix) {
        return userRepository.saveAndFlush(User.builder()
                .username("checkout-" + suffix)
                .email("checkout-" + suffix + "@example.com")
                .password("encoded-password")
                .role(Role.USER)
                .build());
    }

    private void await(java.util.concurrent.CountDownLatch latch) {
        try {
            if (!latch.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting to release cart lock");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for cart lock release", ex);
        }
    }

    private Order orderFor(User user, String key) {
        Order order = Order.builder().user(user).build();
        order.setIdempotencyKey(key);
        return order;
    }
}
