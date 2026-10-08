package com.bookstore.mapper;

import com.bookstore.domain.Book;
import com.bookstore.dto.BookDto;

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
}
