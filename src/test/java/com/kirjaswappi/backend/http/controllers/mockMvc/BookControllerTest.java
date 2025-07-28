/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.controllers.mockMvc;

import static com.kirjaswappi.backend.common.utils.Constants.API_BASE;
import static com.kirjaswappi.backend.common.utils.Constants.BOOKS;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirjaswappi.backend.common.http.controllers.mockMvc.config.CustomMockMvcConfiguration;
import com.kirjaswappi.backend.http.controllers.BookController;
import com.kirjaswappi.backend.service.BookService;
import com.kirjaswappi.backend.service.entities.Book;
import com.kirjaswappi.backend.service.enums.Condition;
import com.kirjaswappi.backend.service.enums.Language;
import com.kirjaswappi.backend.service.enums.SwapType;

@WebMvcTest(BookController.class)
@Import(CustomMockMvcConfiguration.class)
class BookControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private BookService bookService;

  @Autowired
  private ObjectMapper objectMapper;

  private static final String BASE_PATH = API_BASE + BOOKS;

  MockMultipartFile coverPhoto = new MockMultipartFile("coverPhotos", "book.jpg", MediaType.IMAGE_JPEG_VALUE,
      "dummy".getBytes());

  @Test
  @DisplayName("Should create a Book with swap type ByBooks successfully")
  void shouldCreateBookWithConditionTypeByBooksSuccessfully() throws Exception {
    String swapCondition = """
        {
          "swapType": "ByBooks",
          "giveAway": false,
          "openForOffers": false,
          "genres": null,
          "books": [{
            "title": "The Alchemist",
            "author": "Paulo Coelho",
            "coverPhoto": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADElEQVR42mNgYGBgAAAABAABJzQnCgAAAABJRU5ErkJggg=="
          }]
        }
        """;

    Book book = new Book();
    book.setTitle("The Alchemist");
    Mockito.when(bookService.createBook(any(Book.class))).thenReturn(book);

    mockMvc.perform(multipart(BASE_PATH)
        .file(coverPhoto)
        .param("title", "The Alchemist")
        .param("author", "Paulo Coelho")
        .param("description", "A novel by Paulo Coelho")
        .param("language", "English")
        .param("condition", "New")
        .param("genres", "Fiction")
        .param("ownerId", "user-123")
        .param("swapCondition", swapCondition))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("The Alchemist"));
  }

  @Test
  @DisplayName("Should create a Book with swap type ByGenres successfully")
  void shouldCreateBookWithConditionTypeByGenresSuccessfully() throws Exception {
    String swapCondition = """
        {
          "swapType": "ByGenres",
          "giveAway": false,
          "openForOffers": false,
          "genres": "Fiction",
          "books": null
        }
        """;

    Book book = new Book();
    book.setTitle("The Alchemist");
    Mockito.when(bookService.createBook(any(Book.class))).thenReturn(book);

    mockMvc.perform(multipart(BASE_PATH)
        .file(coverPhoto)
        .param("title", "The Alchemist")
        .param("author", "Paulo Coelho")
        .param("description", "A novel by Paulo Coelho")
        .param("language", "English")
        .param("condition", "New")
        .param("genres", "Fiction")
        .param("ownerId", "user-123")
        .param("swapCondition", swapCondition))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("The Alchemist"));
  }

  @Test
  @DisplayName("Should create a Book with swap type GiveAway successfully")
  void shouldCreateBookWithConditionTypeByGiveAwaySuccessfully() throws Exception {
    String swapCondition = """
        {
          "swapType": "GiveAway",
          "giveAway": true,
          "openForOffers": false,
          "genres": null,
          "books": null
        }
        """;

    Book book = new Book();
    book.setTitle("The Alchemist");
    Mockito.when(bookService.createBook(any(Book.class))).thenReturn(book);

    mockMvc.perform(multipart(BASE_PATH)
        .file(coverPhoto)
        .param("title", "The Alchemist")
        .param("author", "Paulo Coelho")
        .param("description", "A novel by Paulo Coelho")
        .param("language", "English")
        .param("condition", "New")
        .param("genres", "Fiction")
        .param("ownerId", "user-123")
        .param("swapCondition", swapCondition))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("The Alchemist"));
  }

  @Test
  @DisplayName("Should create a Book with swap type OpenForOffers successfully")
  void shouldCreateBookWithConditionTypeByOpenForOffersSuccessfully() throws Exception {
    String swapCondition = """
        {
          "swapType": "OpenForOffers",
          "giveAway": false,
          "openForOffers": true,
          "genres": null,
          "books": null
        }
        """;

    Book book = new Book();
    book.setTitle("The Alchemist");
    Mockito.when(bookService.createBook(any(Book.class))).thenReturn(book);

    mockMvc.perform(multipart(BASE_PATH)
        .file(coverPhoto)
        .param("title", "The Alchemist")
        .param("author", "Paulo Coelho")
        .param("description", "A novel by Paulo Coelho")
        .param("language", "English")
        .param("condition", "New")
        .param("genres", "Fiction")
        .param("ownerId", "user-123")
        .param("swapCondition", swapCondition))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("The Alchemist"));
  }

  @Test
  @DisplayName("Should update a Book with swap type ByBooks successfully")
  void shouldUpdateBookWithConditionTypeByBooksSuccessfully() throws Exception {
    String swapCondition = """
        {
          "swapType": "ByBooks",
          "giveAway": false,
          "openForOffers": false,
          "genres": null,
          "books": [{
            "title": "The Alchemist",
            "author": "Paulo Coelho",
            "coverPhoto": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADElEQVR42mNgYGBgAAAABAABJzQnCgAAAABJRU5ErkJggg=="
          }]
        }
        """;

    Book book = new Book();
    book.setId("123");
    book.setTitle("The Alchemist");
    Mockito.when(bookService.updateBook(any(Book.class))).thenReturn(book);

    mockMvc.perform(multipart(BASE_PATH + "/123")
        .file(coverPhoto)
        .param("id", "123")
        .param("title", "The Alchemist")
        .param("author", "Paulo Coelho")
        .param("description", "A novel by Paulo Coelho")
        .param("language", "English")
        .param("condition", "New")
        .param("genres", "Fiction")
        .param("ownerId", "user-123")
        .param("swapCondition", swapCondition)
        .with(request -> {
          request.setMethod("PUT");
          return request;
        }))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("The Alchemist"));
  }

  @Test
  @DisplayName("Should update a Book with swap type ByGenres successfully")
  void shouldUpdateBookWithConditionTypeByGenresSuccessfully() throws Exception {
    String swapCondition = """
        {
          "swapType": "ByGenres",
          "giveAway": false,
          "openForOffers": false,
          "genres": "Fiction",
          "books": null
        }
        """;

    Book book = new Book();
    book.setId("123");
    book.setTitle("The Alchemist");
    Mockito.when(bookService.updateBook(any(Book.class))).thenReturn(book);

    mockMvc.perform(multipart(BASE_PATH + "/123")
        .file(coverPhoto)
        .param("id", "123")
        .param("title", "The Alchemist")
        .param("author", "Paulo Coelho")
        .param("description", "A novel by Paulo Coelho")
        .param("language", "English")
        .param("condition", "New")
        .param("genres", "Fiction")
        .param("ownerId", "user-123")
        .param("swapCondition", swapCondition)
        .with(request -> {
          request.setMethod("PUT");
          return request;
        }))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("The Alchemist"));
  }

  @Test
  @DisplayName("Should update a Book with swap type GiveAway successfully")
  void shouldUpdateBookWithConditionTypeByGiveAwaySuccessfully() throws Exception {
    String swapCondition = """
        {
          "swapType": "GiveAway",
          "giveAway": true,
          "openForOffers": false,
          "genres": null,
          "books": null
        }
        """;

    Book book = new Book();
    book.setId("123");
    book.setTitle("The Alchemist");
    Mockito.when(bookService.updateBook(any(Book.class))).thenReturn(book);

    mockMvc.perform(multipart(BASE_PATH + "/123")
        .file(coverPhoto)
        .param("id", "123")
        .param("title", "The Alchemist")
        .param("author", "Paulo Coelho")
        .param("description", "A novel by Paulo Coelho")
        .param("language", "English")
        .param("condition", "New")
        .param("genres", "Fiction")
        .param("ownerId", "user-123")
        .param("swapCondition", swapCondition)
        .with(request -> {
          request.setMethod("PUT");
          return request;
        }))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("The Alchemist"));
  }

  @Test
  @DisplayName("Should update a Book with swap type OpenForOffers successfully")
  void shouldUpdateBookWithConditionTypeByOpenForOffersSuccessfully() throws Exception {
    String swapCondition = """
        {
          "swapType": "OpenForOffers",
          "giveAway": false,
          "openForOffers": true,
          "genres": null,
          "books": null
        }
        """;

    Book book = new Book();
    book.setId("123");
    book.setTitle("The Alchemist");
    Mockito.when(bookService.updateBook(any(Book.class))).thenReturn(book);

    mockMvc.perform(multipart(BASE_PATH + "/123")
        .file(coverPhoto)
        .param("id", "123")
        .param("title", "The Alchemist")
        .param("author", "Paulo Coelho")
        .param("description", "A novel by Paulo Coelho")
        .param("language", "English")
        .param("condition", "New")
        .param("genres", "Fiction")
        .param("ownerId", "user-123")
        .param("swapCondition", swapCondition)
        .with(request -> {
          request.setMethod("PUT");
          return request;
        }))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("The Alchemist"));
  }

  @Test
  @DisplayName("Should return a Book successfully")
  void shouldReturnBookWhenFound() throws Exception {
    Book book = new Book();
    book.setId("book123");
    book.setTitle("Test Book");

    when(bookService.getBookById("book123")).thenReturn(book);

    mockMvc.perform(get(BASE_PATH + "/book123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("book123"))
        .andExpect(jsonPath("$.title").value("Test Book"));
  }

  @Test
  @DisplayName("Should return more books of this user successfully")
  void shouldReturnListOfOtherBooksThisUserHave() throws Exception {
    Book b1 = new Book();
    b1.setId("b1");
    b1.setTitle("B1");
    Book b2 = new Book();
    b2.setId("b2");
    b2.setTitle("B2");
    when(bookService.getMoreBooksOfTheUser("b3")).thenReturn(List.of(b1, b2));

    mockMvc.perform(get(BASE_PATH + "/b3/more-books"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  @DisplayName("Should return supported book languages")
  void shouldReturnSupportedLanguages() throws Exception {
    mockMvc.perform(get(BASE_PATH + "/supported-languages"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(Language.values().length));
  }

  @Test
  @DisplayName("Should return supported book conditions")
  void shouldReturnSupportedConditions() throws Exception {
    mockMvc.perform(get(BASE_PATH + "/supported-conditions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(Condition.values().length));
  }

  @Test
  @DisplayName("Should return supported book swap types")
  void shouldReturnSupportedSwapTypes() throws Exception {
    mockMvc.perform(get(BASE_PATH + "/supported-swap-types"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(SwapType.values().length));
  }

  @Test
  @DisplayName("Should return paged books by optional filter criteria")
  void shouldReturnPagedBooks() throws Exception {
    Book book = new Book();
    book.setId("book123");
    book.setTitle("Test");
    book.setGenres(new ArrayList<>());
    book.setLanguage(Language.ENGLISH);
    book.setCondition(Condition.FAIR);
    // Add owner info
    com.kirjaswappi.backend.service.entities.User owner = new com.kirjaswappi.backend.service.entities.User();
    owner.setId("owner-1");
    owner.setFirstName("Alice");
    owner.setLastName("Smith");
    book.setOwner(owner);
    when(bookService.getAllBooksByFilter(any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(book), PageRequest.of(0, 10), 1));

    mockMvc.perform(get(BASE_PATH))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.books.length()").value(1))
        .andExpect(jsonPath("$._embedded.books[0].ownerId").value("owner-1"))
        .andExpect(jsonPath("$._embedded.books[0].offeredBy").value("Alice Smith"));
  }

  @Test
  @DisplayName("Should delete a book successfully")
  void shouldReturnNoContentWhenDeletingSingleBook() throws Exception {
    doNothing().when(bookService).deleteBook("book123");
    mockMvc.perform(delete(BASE_PATH + "/book123"))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("Should delete all books successfully")
  void shouldReturnNoContentWhenDeletingAllBooks() throws Exception {
    doNothing().when(bookService).deleteAllBooks();
    mockMvc.perform(delete(BASE_PATH))
        .andExpect(status().isNoContent());
  }
}
