package com.bookstore.service.impl;

import com.bookstore.dto.BookDto;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.mapper.DtoMapper;
import com.bookstore.repository.BookRepository;
import com.bookstore.service.BookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;

    public BookServiceImpl(BookRepository bookRepository){
        this.bookRepository = bookRepository;
    }

    @Override
    public List<BookDto> getAllBooks() {
        log.info("BOOK_LIST_REQUEST");
        List<BookDto> books = bookRepository.findAll().stream()
                .map(DtoMapper::toBookDto).toList();
        log.info("BOOK_LIST_SUCCESS count={}", books.size());
        return books;
    }

    @Override
    public BookDto getBookById(Long id) {
        log.info("BOOK_LOOKUP_REQUEST bookId={}", id);
        return bookRepository.findById(id)
                .map(book -> {
                    log.info("BOOK_LOOKUP_SUCCESS bookId={}", id);
                    return DtoMapper.toBookDto(book);
                })
                .orElseThrow(() -> {
                    log.warn("BOOK_LOOKUP_NOT_FOUND bookId={}", id);
                    return new ResourceNotFoundException("Book", id);
                });
    }
}
