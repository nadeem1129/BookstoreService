package com.bookstore.mapper;

import com.bookstore.domain.Book;
import com.bookstore.domain.Cart;
import com.bookstore.domain.Order;
import com.bookstore.dto.BookDto;
import com.bookstore.dto.CartDto;
import com.bookstore.dto.OrderDto;

import java.util.List;

public final class DtoMapper {
    private DtoMapper(){}

    public static BookDto toBookDto(Book book){
        return new BookDto(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getDescription(),
                book.getPrice(),
                book.getStock()
        );
    }

    public static CartDto toCartDto(Cart cart) {
        List<CartDto.CartItemDto> items = cart.getItems().stream()
                .map(item -> new CartDto.CartItemDto(
                        item.getBook().getId(),
                        item.getBook().getTitle(),
                        item.getBook().getAuthor(),
                        item.getBook().getPrice(),
                        item.getQuantity(),
                        item.getLineTotal()))
                .toList();
        return new CartDto(cart.getId(), items, cart.getsubtotal());
    }

        public static OrderDto toOrderDto(Order order) {
            List<OrderDto.OrderItemDto> items = order.getItems().stream()
                    .map(item -> new OrderDto.OrderItemDto(
                            item.getBook().getId(),
                            item.getBook().getTitle(),
                            item.getQuantity(),
                            item.getUnitPrice(),
                            item.getLineTotal()))
                    .toList();
            return new OrderDto(
                    order.getId(),
                    order.getStatus(),
                    order.getTotalAmount(),
                    order.getCreatedAt(),
                    items);

        }

}
