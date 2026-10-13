package com.bookstore.service.order;

import com.bookstore.domain.Book;
import com.bookstore.domain.Cart;
import com.bookstore.domain.Order;
import com.bookstore.domain.OrderItem;
import com.bookstore.domain.Role;
import com.bookstore.domain.User;
import com.bookstore.mapper.DtoMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class OrderFactoryTest {

    private final OrderFactory factory = new OrderFactory();

    @Test
    void createsOrderItemsWithLockedBookPricesAndTotal() {
        User user = User.builder().id(1L).role(Role.USER).build();
        Book firstBook = book(10L, "15.25");
        Book secondBook = book(20L, "8.50");
        Cart cart = new Cart(user);
        cart.addItem(firstBook, 2);
        cart.addItem(secondBook, 3);

        Order order = factory.createOrder(user, cart, Map.of(
                firstBook.getId(), firstBook,
                secondBook.getId(), secondBook));

        assertSame(user, order.getUser());
        assertEquals(2, order.getItems().size());
        assertEquals(new BigDecimal("56.00"), order.getTotalAmount());

        OrderItem firstItem = order.getItems().get(0);
        assertSame(order, firstItem.getOrder());
        assertSame(firstBook, firstItem.getBook());
        assertEquals(2, firstItem.getQuantity());
        assertEquals(new BigDecimal("15.25"), firstItem.getUnitPrice());
        assertEquals("Book 10", firstItem.getBookTitle());
        assertEquals("isbn-10", firstItem.getBookIsbn());

        firstBook.setTitle("Renamed later");
        firstBook.setIsbn("changed-isbn");
        assertEquals("Book 10", firstItem.getBookTitle());
        assertEquals("isbn-10", firstItem.getBookIsbn());

        var orderDto = DtoMapper.toOrderDto(order);
        assertEquals("Book 10", orderDto.items().get(0).title());
        assertEquals("isbn-10", orderDto.items().get(0).isbn());
    }

    @Test
    void indexesBooksById() {
        Book firstBook = book(10L, "15.25");
        Book secondBook = book(20L, "8.50");

        Map<Long, Book> indexed = OrderFactory.indexById(List.of(firstBook, secondBook));

        assertEquals(2, indexed.size());
        assertSame(firstBook, indexed.get(10L));
        assertSame(secondBook, indexed.get(20L));
    }

    private Book book(Long id, String price) {
        return Book.builder()
                .id(id)
                .title("Book " + id)
                .author("Author")
                .isbn("isbn-" + id)
                .price(new BigDecimal(price))
                .stock(10)
                .build();
    }
}
