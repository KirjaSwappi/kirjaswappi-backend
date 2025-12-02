/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
import com.kirjaswappi.backend.service.entities.*;
import com.kirjaswappi.backend.service.enums.Condition;
import com.kirjaswappi.backend.service.enums.Language;
import com.kirjaswappi.backend.service.enums.SwapType;
import com.kirjaswappi.backend.service.exceptions.BookNotFoundException;

/**
 * Unit tests for Book API endpoints using WebMvcTest with mocked services.
 * Tests the book lifecycle including creation, retrieval, update, deletion,
 * filtering, pagination, and location-based queries.
 */
@WebMvcTest(BookController.class)
@Import(CustomMockMvcConfiguration.class)
class BookApiIntegrationTest {

  private static final String API_BASE = "/api/v1/books";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private BookService bookService;

  private final MockMultipartFile coverPhoto = new MockMultipartFile(
      "coverPhotos",
      "book.jpg",
      MediaType.IMAGE_JPEG_VALUE,
      "dummy image content".getBytes());

  private User createTestUser() {
    return User.builder()
        .id("user-123")
        .firstName("Test")
        .lastName("User")
        .email("test@example.com")
        .build();
  }

  private Book createTestBook(String id, String title, String author) {
    return Book.builder()
        .id(id)
        .title(title)
        .author(author)
        .condition(Condition.GOOD)
        .language(Language.ENGLISH)
        .owner(createTestUser())
        .genres(List.of())
        .swapCondition(SwapCondition.builder()
            .swapType(SwapType.GIVE_AWAY)
            .giveAway(true)
            .build())
        .build();
  }

  private Book createTestBookWithLocation(String id, String title, String city, String country) {
    return Book.builder()
        .id(id)
        .title(title)
        .author("Author")
        .condition(Condition.GOOD)
        .language(Language.ENGLISH)
        .owner(createTestUser())
        .genres(List.of())
        .location(new BookLocation(60.17, 24.94, "Address", city, country, "00100", 50))
        .swapCondition(SwapCondition.builder()
            .swapType(SwapType.GIVE_AWAY)
            .giveAway(true)
            .build())
        .build();
  }

  @Nested
  @DisplayName("Create Book Tests")
  class CreateBookTests {

    @Test
    @DisplayName("Should create book with ByBooks swap type successfully")
    void shouldCreateBookWithByBooksSwapType() throws Exception {
      Book book = createTestBook("book-1", "The Great Book", "Famous Author");
      when(bookService.createBook(any())).thenReturn(book);

      String swapCondition = """
          {
            "swapType": "ByBooks",
            "giveAway": false,
            "openForOffers": false,
            "genres": null,
            "books": [{
              "title": "Wanted Book",
              "author": "Wanted Author",
              "coverPhoto": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADElEQVR42mNgYGBgAAAABAABJzQnCgAAAABJRU5ErkJggg=="
            }]
          }
          """;

      mockMvc.perform(multipart(API_BASE)
          .file(coverPhoto)
          .param("title", "The Great Book")
          .param("author", "Famous Author")
          .param("description", "An amazing book")
          .param("language", "English")
          .param("condition", "New")
          .param("genres", "Fiction")
          .param("ownerId", "user-123")
          .param("swapCondition", swapCondition))
          .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should create book with ByGenres swap type successfully")
    void shouldCreateBookWithByGenresSwapType() throws Exception {
      Book book = createTestBook("book-2", "Genre Swap Book", "Genre Author");
      when(bookService.createBook(any())).thenReturn(book);

      String swapCondition = """
          {
            "swapType": "ByGenres",
            "giveAway": false,
            "openForOffers": false,
            "genres": "Fiction",
            "books": null
          }
          """;

      mockMvc.perform(multipart(API_BASE)
          .file(coverPhoto)
          .param("title", "Genre Swap Book")
          .param("author", "Genre Author")
          .param("description", "A book for genre swap")
          .param("language", "Finnish")
          .param("condition", "Good")
          .param("genres", "Fiction")
          .param("ownerId", "user-123")
          .param("swapCondition", swapCondition))
          .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should create book with GiveAway swap type successfully")
    void shouldCreateBookWithGiveAwaySwapType() throws Exception {
      Book book = createTestBook("book-3", "Free Book", "Generous Author");
      when(bookService.createBook(any())).thenReturn(book);

      String swapCondition = """
          {
            "swapType": "GiveAway",
            "giveAway": true,
            "openForOffers": false,
            "genres": null,
            "books": null
          }
          """;

      mockMvc.perform(multipart(API_BASE)
          .file(coverPhoto)
          .param("title", "Free Book")
          .param("author", "Generous Author")
          .param("language", "Swedish")
          .param("condition", "Fair")
          .param("genres", "Fiction")
          .param("ownerId", "user-123")
          .param("swapCondition", swapCondition))
          .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should create book with OpenForOffers swap type successfully")
    void shouldCreateBookWithOpenForOffersSwapType() throws Exception {
      Book book = createTestBook("book-4", "Offer Book", "Open Author");
      when(bookService.createBook(any())).thenReturn(book);

      String swapCondition = """
          {
            "swapType": "OpenForOffers",
            "giveAway": false,
            "openForOffers": true,
            "genres": null,
            "books": null
          }
          """;

      mockMvc.perform(multipart(API_BASE)
          .file(coverPhoto)
          .param("title", "Offer Book")
          .param("author", "Open Author")
          .param("language", "English")
          .param("condition", "Like New")
          .param("genres", "Fiction")
          .param("ownerId", "user-123")
          .param("swapCondition", swapCondition))
          .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should create book with location data")
    void shouldCreateBookWithLocation() throws Exception {
      Book book = createTestBookWithLocation("book-5", "Located Book", "Helsinki", "Finland");
      when(bookService.createBook(any())).thenReturn(book);

      String swapCondition = """
          {
            "swapType": "GiveAway",
            "giveAway": true,
            "openForOffers": false,
            "genres": null,
            "books": null
          }
          """;

      mockMvc.perform(multipart(API_BASE)
          .file(coverPhoto)
          .param("title", "Located Book")
          .param("author", "Local Author")
          .param("language", "English")
          .param("condition", "Good")
          .param("genres", "Fiction")
          .param("ownerId", "user-123")
          .param("swapCondition", swapCondition)
          .param("location.latitude", "60.1699")
          .param("location.longitude", "24.9384")
          .param("location.address", "Mannerheimintie 1")
          .param("location.city", "Helsinki")
          .param("location.country", "Finland")
          .param("location.postalCode", "00100"))
          .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should return 400 when title is missing")
    void shouldReturn400WhenTitleMissing() throws Exception {
      String swapCondition = """
          {
            "swapType": "GiveAway",
            "giveAway": true,
            "openForOffers": false,
            "genres": null,
            "books": null
          }
          """;

      mockMvc.perform(multipart(API_BASE)
          .file(coverPhoto)
          .param("author", "Author Name")
          .param("language", "English")
          .param("condition", "Good")
          .param("genres", "Fiction")
          .param("ownerId", "user-123")
          .param("swapCondition", swapCondition))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when invalid swap condition JSON")
    void shouldReturn400WhenInvalidSwapCondition() throws Exception {
      mockMvc.perform(multipart(API_BASE)
          .file(coverPhoto)
          .param("title", "Book Title")
          .param("author", "Author Name")
          .param("language", "English")
          .param("condition", "Good")
          .param("genres", "Fiction")
          .param("ownerId", "user-123")
          .param("swapCondition", "invalid json"))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("Get Book Tests")
  class GetBookTests {

    @Test
    @DisplayName("Should return book by ID")
    void shouldReturnBookById() throws Exception {
      Book book = createTestBook("book-1", "Test Book", "Test Author");
      when(bookService.getBookById("book-1")).thenReturn(book);

      mockMvc.perform(get(API_BASE + "/book-1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value("book-1"))
          .andExpect(jsonPath("$.title").value("Test Book"))
          .andExpect(jsonPath("$.author").value("Test Author"));
    }

    @Test
    @DisplayName("Should return 400 for non-existent book")
    void shouldReturn404ForNonExistentBook() throws Exception {
      // BookNotFoundException extends BusinessException which returns 400
      when(bookService.getBookById("nonexistent-id"))
          .thenThrow(new BookNotFoundException());

      mockMvc.perform(get(API_BASE + "/nonexistent-id"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return more books of the same user")
    void shouldReturnMoreBooksOfUser() throws Exception {
      Book book2 = createTestBook("book-2", "Book 2", "Author");
      Book book3 = createTestBook("book-3", "Book 3", "Author");
      when(bookService.getMoreBooksOfTheUser("book-1")).thenReturn(List.of(book2, book3));

      mockMvc.perform(get(API_BASE + "/book-1/more-books"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Should return empty list when no other books exist")
    void shouldReturnEmptyListWhenNoOtherBooks() throws Exception {
      when(bookService.getMoreBooksOfTheUser("book-1")).thenReturn(List.of());

      mockMvc.perform(get(API_BASE + "/book-1/more-books"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    }
  }

  @Nested
  @DisplayName("List Books Tests")
  class ListBooksTests {

    @Test
    @DisplayName("Should return paginated books")
    void shouldReturnPaginatedBooks() throws Exception {
      List<Book> books = List.of(
          createTestBook("book-1", "Book 1", "Author 1"),
          createTestBook("book-2", "Book 2", "Author 2"),
          createTestBook("book-3", "Book 3", "Author 3"),
          createTestBook("book-4", "Book 4", "Author 4"),
          createTestBook("book-5", "Book 5", "Author 5"));

      when(bookService.getAllBooksByFilter(any(), any(Pageable.class)))
          .thenReturn(new PageImpl<>(books, PageRequest.of(0, 5), 15));

      mockMvc.perform(get(API_BASE)
          .param("page", "0")
          .param("size", "5"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$._embedded.books.length()").value(5))
          .andExpect(jsonPath("$.page.totalElements").value(15));
    }

    @Test
    @DisplayName("Should return second page of books")
    void shouldReturnSecondPageOfBooks() throws Exception {
      List<Book> books = List.of(
          createTestBook("book-6", "Book 6", "Author 6"),
          createTestBook("book-7", "Book 7", "Author 7"),
          createTestBook("book-8", "Book 8", "Author 8"),
          createTestBook("book-9", "Book 9", "Author 9"),
          createTestBook("book-10", "Book 10", "Author 10"));

      when(bookService.getAllBooksByFilter(any(), any(Pageable.class)))
          .thenReturn(new PageImpl<>(books, PageRequest.of(1, 5), 15));

      mockMvc.perform(get(API_BASE)
          .param("page", "1")
          .param("size", "5"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$._embedded.books.length()").value(5));
    }

    @Test
    @DisplayName("Should return empty page when no books exist")
    void shouldReturnEmptyPageWhenNoBooks() throws Exception {
      when(bookService.getAllBooksByFilter(any(), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

      mockMvc.perform(get(API_BASE))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.page.totalElements").value(0));
    }
  }

  @Nested
  @DisplayName("Location-Based Query Tests")
  class LocationBasedQueryTests {

    @Test
    @DisplayName("Should find books near location")
    void shouldFindBooksNearLocation() throws Exception {
      Book book = createTestBookWithLocation("book-1", "Helsinki Book", "Helsinki", "Finland");
      when(bookService.findBooksNearLocation(any(Double.class), any(Double.class), any(Integer.class),
          any(Pageable.class)))
              .thenReturn(new PageImpl<>(List.of(book), PageRequest.of(0, 10), 1));

      mockMvc.perform(get(API_BASE + "/near")
          .param("latitude", "60.17")
          .param("longitude", "24.94")
          .param("radiusKm", "10"))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return 400 when latitude is missing")
    void shouldReturn400WhenLatitudeMissing() throws Exception {
      mockMvc.perform(get(API_BASE + "/near")
          .param("longitude", "24.94"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when longitude is missing")
    void shouldReturn400WhenLongitudeMissing() throws Exception {
      mockMvc.perform(get(API_BASE + "/near")
          .param("latitude", "60.17"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when latitude is invalid")
    void shouldReturn400WhenLatitudeInvalid() throws Exception {
      mockMvc.perform(get(API_BASE + "/near")
          .param("latitude", "100") // Invalid: > 85
          .param("longitude", "24.94"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when longitude is invalid")
    void shouldReturn400WhenLongitudeInvalid() throws Exception {
      mockMvc.perform(get(API_BASE + "/near")
          .param("latitude", "60.17")
          .param("longitude", "200")) // Invalid: > 180
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should find books in city")
    void shouldFindBooksInCity() throws Exception {
      Book book = createTestBookWithLocation("book-1", "Helsinki Book", "Helsinki", "Finland");
      when(bookService.findBooksInCity(any(String.class), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(book), PageRequest.of(0, 10), 1));

      mockMvc.perform(get(API_BASE + "/city/Helsinki"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$._embedded.books[0].location.city").value("Helsinki"));
    }

    @Test
    @DisplayName("Should find books in country")
    void shouldFindBooksInCountry() throws Exception {
      Book book = createTestBookWithLocation("book-1", "Finnish Book", "Helsinki", "Finland");
      when(bookService.findBooksInCountry(any(String.class), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(book), PageRequest.of(0, 10), 1));

      mockMvc.perform(get(API_BASE + "/country/Finland"))
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("Update Book Tests")
  class UpdateBookTests {

    @Test
    @DisplayName("Should update book successfully")
    void shouldUpdateBookSuccessfully() throws Exception {
      Book book = createTestBook("book-1", "Updated Title", "Updated Author");
      when(bookService.updateBook(any())).thenReturn(book);

      String swapCondition = """
          {
            "swapType": "GiveAway",
            "giveAway": true,
            "openForOffers": false,
            "genres": null,
            "books": null
          }
          """;

      mockMvc.perform(multipart(API_BASE + "/book-1")
          .file(coverPhoto)
          .param("id", "book-1")
          .param("title", "Updated Title")
          .param("author", "Updated Author")
          .param("description", "Updated description")
          .param("language", "Finnish")
          .param("condition", "Fair")
          .param("genres", "Fiction")
          .param("ownerId", "user-123")
          .param("swapCondition", swapCondition)
          .with(request -> {
            request.setMethod("PUT");
            return request;
          }))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return 400 when path ID and body ID mismatch")
    void shouldReturn400WhenIdMismatch() throws Exception {
      String swapCondition = """
          {
            "swapType": "GiveAway",
            "giveAway": true,
            "openForOffers": false,
            "genres": null,
            "books": null
          }
          """;

      mockMvc.perform(multipart(API_BASE + "/book-1")
          .file(coverPhoto)
          .param("id", "different-id")
          .param("title", "Updated Title")
          .param("author", "Updated Author")
          .param("language", "English")
          .param("condition", "Good")
          .param("genres", "Fiction")
          .param("ownerId", "user-123")
          .param("swapCondition", swapCondition)
          .with(request -> {
            request.setMethod("PUT");
            return request;
          }))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("Delete Book Tests")
  class DeleteBookTests {

    @Test
    @DisplayName("Should delete book successfully")
    void shouldDeleteBookSuccessfully() throws Exception {
      mockMvc.perform(delete(API_BASE + "/book-1"))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should delete all books successfully")
    void shouldDeleteAllBooksSuccessfully() throws Exception {
      mockMvc.perform(delete(API_BASE))
          .andExpect(status().isNoContent());
    }
  }

  @Nested
  @DisplayName("Supported Values Tests")
  class SupportedValuesTests {

    @Test
    @DisplayName("Should return supported languages")
    void shouldReturnSupportedLanguages() throws Exception {
      mockMvc.perform(get(API_BASE + "/supported-languages"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").isArray())
          .andExpect(jsonPath("$.length()").isNumber());
    }

    @Test
    @DisplayName("Should return supported conditions")
    void shouldReturnSupportedConditions() throws Exception {
      mockMvc.perform(get(API_BASE + "/supported-conditions"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").isArray())
          .andExpect(jsonPath("$.length()").isNumber());
    }

    @Test
    @DisplayName("Should return supported swap types")
    void shouldReturnSupportedSwapTypes() throws Exception {
      mockMvc.perform(get(API_BASE + "/supported-swap-types"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").isArray())
          .andExpect(jsonPath("$.length()").isNumber());
    }
  }
}
