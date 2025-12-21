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

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kirjaswappi.backend.common.http.controllers.mockMvc.config.CustomMockMvcConfiguration;
import com.kirjaswappi.backend.http.controllers.BookController;
import com.kirjaswappi.backend.service.BookService;
import com.kirjaswappi.backend.service.entities.Book;
import com.kirjaswappi.backend.service.entities.User;
import com.kirjaswappi.backend.service.enums.Condition;
import com.kirjaswappi.backend.service.enums.Language;
import com.kirjaswappi.backend.service.enums.SwapType;

@WebMvcTest(BookController.class)
@Import(CustomMockMvcConfiguration.class)
class BookControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private BookService bookService;

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

    Book book = Book.builder().title("The Alchemist").build();

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

    Book book = Book.builder().title("The Alchemist").build();
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

    Book book = Book.builder().title("The Alchemist").build();

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

    Book book = Book.builder().title("The Alchemist").build();
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

    Book book = Book.builder().id("123").title("The Alchemist").build();
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

    Book book = Book.builder().id("123").title("The Alchemist").build();
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

    Book book = Book.builder().id("123").title("The Alchemist").build();
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

    Book book = Book.builder().id("123").title("The Alchemist").build();
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
    Book book = Book.builder().id("book123").title("Test Book").build();

    when(bookService.getBookById("book123")).thenReturn(book);

    mockMvc.perform(get(BASE_PATH + "/book123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("book123"))
        .andExpect(jsonPath("$.title").value("Test Book"));
  }

  @Test
  @DisplayName("Should return more books of this user successfully")
  void shouldReturnListOfOtherBooksThisUserHave() throws Exception {
    Book b1 = Book.builder().id("b1").title("B1").build();
    Book b2 = Book.builder().id("b2").title("B2").build();

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

    var owner = User.builder()
        .id("owner-1")
        .firstName("Alice")
        .lastName("Smith")
        .build();

    var book = Book.builder()
        .id("book123")
        .title("Test")
        .genres(List.of())
        .language(Language.ENGLISH)
        .condition(Condition.FAIR)
        .owner(owner) // Add owner info
        .build();

    when(bookService.getAllBooksByFilter(any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(book), PageRequest.of(0, 10), 1));

    mockMvc.perform(get(BASE_PATH))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.books.length()").value(1))
        .andExpect(jsonPath("$._embedded.books[0].ownerId").value("owner-1"))
        .andExpect(jsonPath("$._embedded.books[0].offeredBy").value("Alice Smith"));
  }

  @Test
  @DisplayName("Should filter books by city")
  void shouldFilterBooksByCity() throws Exception {
    var owner = User.builder()
        .id("owner-1")
        .firstName("Alice")
        .lastName("Smith")
        .build();

    var location = com.kirjaswappi.backend.service.entities.BookLocation.builder()
        .city("Helsinki")
        .country("Finland")
        .build();

    var book = Book.builder()
        .id("book123")
        .title("Test Book")
        .genres(List.of())
        .language(Language.ENGLISH)
        .condition(Condition.FAIR)
        .location(location)
        .owner(owner)
        .build();

    when(bookService.getAllBooksByFilter(any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(book), PageRequest.of(0, 10), 1));

    mockMvc.perform(get(BASE_PATH).param("city", "Helsinki"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.books.length()").value(1))
        .andExpect(jsonPath("$._embedded.books[0].location.city").value("Helsinki"));
  }

  @Test
  @DisplayName("Should filter books by parent genre")
  void shouldFilterBooksByParentGenre() throws Exception {
    var owner = User.builder()
        .id("owner-1")
        .firstName("Alice")
        .lastName("Smith")
        .build();

    var book = Book.builder()
        .id("book123")
        .title("Test Book")
        .genres(List.of())
        .language(Language.ENGLISH)
        .condition(Condition.FAIR)
        .owner(owner)
        .build();

    when(bookService.getAllBooksByFilter(any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(book), PageRequest.of(0, 10), 1));

    mockMvc.perform(get(BASE_PATH).param("genres", "Fiction"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.books.length()").value(1))
        .andExpect(jsonPath("$._embedded.books[0].ownerId").value("owner-1"));
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
