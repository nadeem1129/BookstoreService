package com.bookstore.service.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bookstore.domain.Book;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.BookRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BookServiceImplTest {

    private final BookRepository bookRepository = mock(BookRepository.class);
    private final BookServiceImpl bookService = new BookServiceImpl(bookRepository);
    private final Logger logger = (Logger) LoggerFactory.getLogger(BookServiceImpl.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void attachLogAppender() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachLogAppender() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void logsBookListLookupAndResultCount() {
        when(bookRepository.findAll()).thenReturn(List.of(
                Book.builder().id(10L).title("Test").author("Author")
                        .price(new BigDecimal("10.00")).stock(1).build()));

        bookService.getAllBooks();

        assertLogContains("BOOK_LIST_REQUEST");
        assertLogContains("BOOK_LIST_SUCCESS count=1");
    }

    @Test
    void logsBookLookupById() {
        when(bookRepository.findById(10L)).thenReturn(Optional.of(
                Book.builder().id(10L).title("Test").author("Author")
                        .price(new BigDecimal("10.00")).stock(1).build()));

        bookService.getBookById(10L);

        assertLogContains("BOOK_LOOKUP_REQUEST bookId=10");
        assertLogContains("BOOK_LOOKUP_SUCCESS bookId=10");
    }

    @Test
    void logsMissingBookLookup() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookService.getBookById(99L));

        assertLogContains("BOOK_LOOKUP_NOT_FOUND bookId=99");
    }

    private void assertLogContains(String message) {
        assertTrue(appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .anyMatch(logMessage -> logMessage.contains(message)), message);
    }
}
