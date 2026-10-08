package com.bookstore.service.impl;

import com.bookstore.dto.BookDto;
import com.bookstore.mapper.DtoMapper;
import com.bookstore.repository.BookRepository;
import com.bookstore.service.BookService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;

    public BookServiceImpl(BookRepository bookRepository){
        this.bookRepository = bookRepository;
    }

    @Override
    public List<BookDto> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(DtoMapper::toBookDto).toList();
    }

    @Override
    public BookDto getBookById(Long id) {
        return null;
    }
}
