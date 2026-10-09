package com.bookstore.service.order;

import com.bookstore.domain.Book;
import com.bookstore.domain.Cart;
import com.bookstore.domain.Order;
import com.bookstore.domain.OrderItem;
import com.bookstore.domain.User;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OrderFactory {

    public Order createOrder(User user, Cart cart, Map<Long, Book> lockedBooks) {
        Order order = Order.builder().user(user).build();

        cart.getItems().forEach(cartItem -> {
            Book book = lockedBooks.get(cartItem.getBook().getId());
            OrderItem orderItem = OrderItem.builder()
                    .book(book)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(book.getPrice())
                    .build();
            order.addItem(orderItem);

        });

        order.recalculateTotal();
        return order;
    }

    public static Map<Long, Book> indexById(Iterable<Book> books) {
        return java.util.stream.StreamSupport.stream(books.spliterator(), false)
                .collect(Collectors.toMap(Book::getId, Function.identity()));
    }
}