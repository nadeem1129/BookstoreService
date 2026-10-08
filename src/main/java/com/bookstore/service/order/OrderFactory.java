package com.bookstore.service.order;

import com.bookstore.domain.*;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OrderFactory {

    public Order createOrder(User user, Cart cart, Map<Long, Book> lockedBooks) {
        Order order = new Order();
        order.setUser(user);

        cart.getItems().forEach(cartItem -> {
            Book book = lockedBooks.get(cartItem.getBook().getId());
            OrderItem orderItem = new OrderItem(book, cartItem.getQuantity(), book.getPrice());
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