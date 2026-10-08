package com.bookstore.service.impl;

import com.bookstore.dto.BookDto;
import com.bookstore.service.BookService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookServiceImpl implements BookService {
    @Override
    public List<BookDto> getAllBooks() {
        return List.of();
    }

    @Override
    public BookDto getBookById(Long id) {
        return null;
    }
}
