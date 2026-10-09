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
        return BookDto.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .description(book.getDescription())
                .price(book.getPrice())
                .stock(book.getStock())
                .build();
    }

    public static CartDto toCartDto(Cart cart) {
        List<CartDto.CartItemDto> items = cart.getItems().stream()
                .map(item -> CartDto.CartItemDto.builder()
                        .bookId(item.getBook().getId())
                        .title(item.getBook().getTitle())
                        .author(item.getBook().getAuthor())
                        .unitPrice(item.getBook().getPrice())
                        .quantity(item.getQuantity())
                        .lineTotal(item.getLineTotal())
                        .build())
                .toList();
        return CartDto.builder()
                .id(cart.getId())
                .items(items)
                .subtotal(cart.getsubtotal())
                .build();
    }

        public static OrderDto toOrderDto(Order order) {
            List<OrderDto.OrderItemDto> items = order.getItems().stream()
                    .map(item -> OrderDto.OrderItemDto.builder()
                            .bookId(item.getBook().getId())
                            .title(item.getBook().getTitle())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .lineTotal(item.getLineTotal())
                            .build())
                    .toList();
            return OrderDto.builder()
                    .id(order.getId())
                    .status(order.getStatus())
                    .totalAmount(order.getTotalAmount())
                    .createdAt(order.getCreatedAt())
                    .items(items)
                    .build();

        }

}
