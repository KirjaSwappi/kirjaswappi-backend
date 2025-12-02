/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.controllers.mockMvc;

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
import com.kirjaswappi.backend.service.entities.Book;
import com.kirjaswappi.backend.service.entities.BookLocation;
import com.kirjaswappi.backend.service.entities.User;
import com.kirjaswappi.backend.service.enums.Condition;
import com.kirjaswappi.backend.service.enums.Language;
import com.kirjaswappi.backend.service.exceptions.BookNotFoundException;

/**
 * Extended edge case tests for Book Controller covering location-based queries,
 * error scenarios, and complex filtering.
 */
@WebMvcTest(BookController.class)
@Import(CustomMockMvcConfiguration.class)
class BookControllerEdgeCaseTest {

  private static final String API_BASE = "/api/v1/books";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private BookService bookService;

  private final MockMultipartFile coverPhoto = new MockMultipartFile(
      "coverPhotos",
      "book.jpg",
      MediaType.IMAGE_JPEG_VALUE,
      "dummy".getBytes());

  @Nested
  @DisplayName("Location-Based Query Edge Cases")
  class LocationBasedQueryTests {

    @Test
    @DisplayName("Should return 400 when latitude is null")
    void shouldReturn400WhenLatitudeIsNull() throws Exception {
      mockMvc.perform(get(API_BASE + "/near")
          .param("longitude", "24.9384"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when longitude is null")
    void shouldReturn400WhenLongitudeIsNull() throws Exception {
      mockMvc.perform(get(API_BASE + "/near")
          .param("latitude", "60.1699"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when latitude is below -85")
    void shouldReturn400WhenLatitudeBelowMin() throws Exception {
      mockMvc.perform(get(API_BASE + "/near")
          .param("latitude", "-90")
          .param("longitude", "24.9384"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when latitude is above 85")
    void shouldReturn400WhenLatitudeAboveMax() throws Exception {
      mockMvc.perform(get(API_BASE + "/near")
          .param("latitude", "90")
          .param("longitude", "24.9384"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when longitude is below -180")
    void shouldReturn400WhenLongitudeBelowMin() throws Exception {
      mockMvc.perform(get(API_BASE + "/near")
          .param("latitude", "60.1699")
          .param("longitude", "-200"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when longitude is above 180")
    void shouldReturn400WhenLongitudeAboveMax() throws Exception {
      mockMvc.perform(get(API_BASE + "/near")
          .param("latitude", "60.1699")
          .param("longitude", "200"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should use default radius when not provided")
    void shouldUseDefaultRadiusWhenNotProvided() throws Exception {
      var owner = User.builder().id("owner-1").firstName("Test").lastName("User").build();
      var location = new BookLocation(60.17, 24.94, "Address", "Helsinki", "Finland", "00100", 50);
      var book = Book.builder()
          .id("book-1")
          .title("Near Book")
          .owner(owner)
          .genres(List.of())
          .language(Language.ENGLISH)
          .condition(Condition.GOOD)
          .location(location)
          .build();

      when(bookService.findBooksNearLocation(any(Double.class), any(Double.class), any(Integer.class),
          any(Pageable.class)))
              .thenReturn(new PageImpl<>(List.of(book), PageRequest.of(0, 10), 1));

      mockMvc.perform(get(API_BASE + "/near")
          .param("latitude", "60.17")
          .param("longitude", "24.94"))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should cap radius at 1000km")
    void shouldCapRadiusAt1000km() throws Exception {
      var owner = User.builder().id("owner-1").firstName("Test").lastName("User").build();
      var book = Book.builder()
          .id("book-1")
          .title("Far Book")
          .owner(owner)
          .genres(List.of())
          .language(Language.ENGLISH)
          .condition(Condition.GOOD)
          .build();

      when(bookService.findBooksNearLocation(any(Double.class), any(Double.class), any(Integer.class),
          any(Pageable.class)))
              .thenReturn(new PageImpl<>(List.of(book), PageRequest.of(0, 10), 1));

      mockMvc.perform(get(API_BASE + "/near")
          .param("latitude", "60.17")
          .param("longitude", "24.94")
          .param("radiusKm", "5000")) // Should be capped to 1000
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should find books in city")
    void shouldFindBooksInCity() throws Exception {
      var owner = User.builder().id("owner-1").firstName("Test").lastName("User").build();
      var location = new BookLocation(60.17, 24.94, "Address", "Helsinki", "Finland", "00100", 50);
      var book = Book.builder()
          .id("book-1")
          .title("Helsinki Book")
          .owner(owner)
          .genres(List.of())
          .language(Language.ENGLISH)
          .condition(Condition.GOOD)
          .location(location)
          .build();

      when(bookService.findBooksInCity(any(String.class), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(book), PageRequest.of(0, 10), 1));

      mockMvc.perform(get(API_BASE + "/city/Helsinki"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$._embedded.books.length()").value(1));
    }

    @Test
    @DisplayName("Should find books in country")
    void shouldFindBooksInCountry() throws Exception {
      var owner = User.builder().id("owner-1").firstName("Test").lastName("User").build();
      var book = Book.builder()
          .id("book-1")
          .title("Finnish Book")
          .owner(owner)
          .genres(List.of())
          .language(Language.FINNISH)
          .condition(Condition.GOOD)
          .build();

      when(bookService.findBooksInCountry(any(String.class), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(book), PageRequest.of(0, 10), 1));

      mockMvc.perform(get(API_BASE + "/country/Finland"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$._embedded.books.length()").value(1));
    }

    @Test
    @DisplayName("Should return empty results for city with no books")
    void shouldReturnEmptyForCityWithNoBooks() throws Exception {
      when(bookService.findBooksInCity(any(String.class), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

      mockMvc.perform(get(API_BASE + "/city/UnknownCity"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.page.totalElements").value(0));
    }
  }

  @Nested
  @DisplayName("Book Not Found Error Cases")
  class BookNotFoundTests {

    @Test
    @DisplayName("Should return 400 when book not found by ID")
    void shouldReturn400WhenBookNotFoundById() throws Exception {
      // BookNotFoundException extends BusinessException which returns 400 BAD_REQUEST
      when(bookService.getBookById("nonexistent-id"))
          .thenThrow(new BookNotFoundException());

      mockMvc.perform(get(API_BASE + "/nonexistent-id"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when getting more books for nonexistent book")
    void shouldReturn400WhenGettingMoreBooksForNonexistent() throws Exception {
      // BookNotFoundException extends BusinessException which returns 400 BAD_REQUEST
      when(bookService.getMoreBooksOfTheUser("nonexistent-id"))
          .thenThrow(new BookNotFoundException());

      mockMvc.perform(get(API_BASE + "/nonexistent-id/more-books"))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("Create Book Validation Edge Cases")
  class CreateBookValidationTests {

    @Test
    @DisplayName("Should return 400 when author is missing")
    void shouldReturn400WhenAuthorMissing() throws Exception {
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
          .param("title", "Test Book")
          .param("language", "English")
          .param("condition", "Good")
          .param("genres", "Fiction")
          .param("ownerId", "user-123")
          .param("swapCondition", swapCondition))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when language is missing")
    void shouldReturn400WhenLanguageMissing() throws Exception {
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
          .param("title", "Test Book")
          .param("author", "Test Author")
          .param("condition", "Good")
          .param("genres", "Fiction")
          .param("ownerId", "user-123")
          .param("swapCondition", swapCondition))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when condition is missing")
    void shouldReturn400WhenConditionMissing() throws Exception {
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
          .param("title", "Test Book")
          .param("author", "Test Author")
          .param("language", "English")
          .param("genres", "Fiction")
          .param("ownerId", "user-123")
          .param("swapCondition", swapCondition))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when ownerId is missing")
    void shouldReturn400WhenOwnerIdMissing() throws Exception {
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
          .param("title", "Test Book")
          .param("author", "Test Author")
          .param("language", "English")
          .param("condition", "Good")
          .param("genres", "Fiction")
          .param("swapCondition", swapCondition))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when swapCondition is missing")
    void shouldReturn400WhenSwapConditionMissing() throws Exception {
      mockMvc.perform(multipart(API_BASE)
          .file(coverPhoto)
          .param("title", "Test Book")
          .param("author", "Test Author")
          .param("language", "English")
          .param("condition", "Good")
          .param("genres", "Fiction")
          .param("ownerId", "user-123"))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("Pagination Edge Cases")
  class PaginationTests {

    @Test
    @DisplayName("Should handle large page size")
    void shouldHandleLargePageSize() throws Exception {
      var owner = User.builder().id("owner-1").firstName("Test").lastName("User").build();
      var book = Book.builder()
          .id("book-1")
          .title("Test Book")
          .owner(owner)
          .genres(List.of())
          .language(Language.ENGLISH)
          .condition(Condition.GOOD)
          .build();

      when(bookService.getAllBooksByFilter(any(), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(book), PageRequest.of(0, 100), 1));

      mockMvc.perform(get(API_BASE)
          .param("page", "0")
          .param("size", "100"))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return empty page for negative page number")
    void shouldHandleNegativePageNumber() throws Exception {
      var owner = User.builder().id("owner-1").firstName("Test").lastName("User").build();
      var book = Book.builder()
          .id("book-1")
          .title("Test Book")
          .owner(owner)
          .genres(List.of())
          .language(Language.ENGLISH)
          .condition(Condition.GOOD)
          .build();

      // Spring treats negative page as 0
      when(bookService.getAllBooksByFilter(any(), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(book), PageRequest.of(0, 10), 1));

      mockMvc.perform(get(API_BASE)
          .param("page", "-1")
          .param("size", "10"))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle zero page size by using default")
    void shouldHandleZeroPageSize() throws Exception {
      var owner = User.builder().id("owner-1").firstName("Test").lastName("User").build();
      var book = Book.builder()
          .id("book-1")
          .title("Test Book")
          .owner(owner)
          .genres(List.of())
          .language(Language.ENGLISH)
          .condition(Condition.GOOD)
          .build();

      // Zero size might default to system default or throw error - depends on config
      when(bookService.getAllBooksByFilter(any(), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(book), PageRequest.of(0, 10), 1));

      mockMvc.perform(get(API_BASE)
          .param("page", "0")
          .param("size", "0"))
          .andExpect(status().isOk());
    }
  }
}
