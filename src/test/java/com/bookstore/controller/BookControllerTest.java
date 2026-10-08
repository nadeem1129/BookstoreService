package com.bookstore.controller;

import com.bookstore.dto.BookDto;
import com.bookstore.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:3000",
        "app.security.jwt.secret=4a7420f8e8b937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b",
        "app.security.jwt.expiration-ms=3600000"
})
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    @Test
    void getAllBooks_shouldReturnListOfBooks() throws Exception {
        BookDto firstBook = new BookDto(1L, "Clean Code", "Robert C. Martin", "9780132350884",
                "A handbook of agile software craftsmanship.", new BigDecimal("29.99"), 10);
        BookDto secondBook = new BookDto(2L, "The Pragmatic Programmer", "Andrew Hunt", "9780201616224",
                "A classic book for software developers.", new BigDecimal("34.50"), 7);

        given(bookService.getAllBooks()).willReturn(List.of(firstBook, secondBook));

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Clean Code"))
                .andExpect(jsonPath("$[1].isbn").value("9780201616224"));
    }

    @Test
    void getBook_shouldReturnBookById() throws Exception {
        BookDto book = new BookDto(1L, "Spring in Action", "Craig Walls", "9781617294945",
                "Comprehensive guide to Spring Framework.", new BigDecimal("42.00"), 5);

        given(bookService.getBookById(1L)).willReturn(book);

        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Spring in Action"))
                .andExpect(jsonPath("$.author").value("Craig Walls"));
    }
}
