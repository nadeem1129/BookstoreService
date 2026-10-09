package com.bookstore.repository;

import com.bookstore.domain.Cart;
import com.bookstore.domain.Order;
import com.bookstore.domain.Role;
import com.bookstore.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    private UserRepository userRepository;

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

    private User createUser(String suffix) {
        return userRepository.saveAndFlush(User.builder()
                .username("checkout-" + suffix)
                .email("checkout-" + suffix + "@example.com")
                .password("encoded-password")
                .role(Role.USER)
                .build());
    }

    private Order orderFor(User user, String key) {
        Order order = Order.builder().user(user).build();
        order.setIdempotencyKey(key);
        return order;
    }
}
