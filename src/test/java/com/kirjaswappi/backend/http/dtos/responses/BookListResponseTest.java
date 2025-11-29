/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.responses;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.kirjaswappi.backend.service.entities.Book;
import com.kirjaswappi.backend.service.entities.Genre;
import com.kirjaswappi.backend.service.entities.User;
import com.kirjaswappi.backend.service.enums.Condition;
import com.kirjaswappi.backend.service.enums.Language;

class BookListResponseTest {
  @Test
  @DisplayName("Populates all BookListResponse fields including offeredAgo and bookLocation")
  void constructor_populatesAllFields_withCompleteOwnerInfo() {
    User owner = new User()
        .id("owner1")
        .firstName("John")
        .lastName("Doe")
        .city("Espoo");

    Book book = Book.builder()
        .id("book1")
        .title("Test Book")
        .author("Author")
        .genres(List.of(new Genre("Fiction")))
        .language(Language.ENGLISH)
        .description("A book")
        .condition(Condition.NEW)
        .coverPhotos(List.of("url"))
        .owner(owner)
        .bookAddedAt(Instant.now().minusSeconds(3600)) // 1 hour ago
        .bookUpdatedAt(Instant.now().minusSeconds(3600)) // 1 hour ago
        .build();
    // Act
    BookListResponse response = new BookListResponse(book);
    // Assert
    assertEquals("book1", response.getId());
    assertEquals("Test Book", response.getTitle());
    assertEquals("Author", response.getAuthor());
    assertEquals(List.of("Fiction"), response.getGenres());
    assertEquals("English", response.getLanguage());
    assertEquals("A book", response.getDescription());
    assertEquals("New", response.getCondition());
    assertEquals("url", response.getCoverPhotoUrl());
    assertEquals("owner1", response.getOwnerId());
    assertEquals("John Doe", response.getOfferedBy());
    assertEquals("Espoo", response.getBookLocation());
    assertTrue(response.getOfferedAgo().contains("hour ago"));
  }

  @Test
  @DisplayName("Handles null owner and null location")
  void constructor_handlesNullOwner() {
    Book book = Book.builder()
        .id("book2")
        .title("T")
        .author("A")
        .genres(List.of())
        .language(Language.ENGLISH)
        .description("D")
        .condition(Condition.NEW)
        .coverPhotos(List.of("U"))
        .owner(null)
        .bookAddedAt(Instant.now().minusSeconds(60)) // 1 min ago
        .bookUpdatedAt(Instant.now().minusSeconds(60)) // 1 min ago
        .build();
    // Act
    BookListResponse response = new BookListResponse(book);
    // Assert
    assertNull(response.getOwnerId());
    assertNull(response.getOfferedBy());
    assertNull(response.getBookLocation());
    assertTrue(response.getOfferedAgo().contains("min ago"));
  }

  @Test
  @DisplayName("Handles partial owner data")
  void constructor_handlesPartialOwnerData() {
    User owner = new User()
        .id("owner2")
        .firstName(null)
        .lastName(null)
        .city(null);

    Book book = Book.builder()
        .id("id")
        .title("t")
        .author("a")
        .genres(List.of())
        .language(Language.ENGLISH)
        .description("d")
        .condition(Condition.NEW)
        .coverPhotos(List.of("u"))
        .owner(owner)
        .bookAddedAt(Instant.now().minusSeconds(86400)) // 1 day ago
        .bookUpdatedAt(Instant.now().minusSeconds(86400)) // 1 day ago
        .build();
    // Act
    BookListResponse response = new BookListResponse(book);
    // Assert
    assertEquals("owner2", response.getOwnerId());
    assertEquals("null null", response.getOfferedBy());
    assertNull(response.getBookLocation());
    assertTrue(response.getOfferedAgo().contains("day ago"));
  }

  @Test
  @DisplayName("Existing fields remain unchanged and offeredAgo is correct")
  void existingFieldsRemainUnchanged() {
    Book book = Book.builder()
        .id("book3")
        .title("Title")
        .author("Author")
        .genres(List.of(new Genre("Fiction")))
        .language(Language.ENGLISH)
        .description("Desc")
        .condition(Condition.GOOD)
        .coverPhotos(List.of("url"))
        .owner(null)
        .bookAddedAt(Instant.now().minusSeconds(2592000))
        .bookUpdatedAt(Instant.now().minusSeconds(2592000)) // 1 month ago
        .build();
    // Act
    BookListResponse response = new BookListResponse(book);
    // Assert
    assertEquals("book3", response.getId());
    assertEquals("Title", response.getTitle());
    assertEquals("Author", response.getAuthor());
    assertEquals(List.of("Fiction"), response.getGenres());
    assertEquals("English", response.getLanguage());
    assertEquals("Desc", response.getDescription());
    assertEquals("Good", response.getCondition());
    assertEquals("url", response.getCoverPhotoUrl());
    assertTrue(response.getOfferedAgo().contains("month ago"));
  }

  @Test
  @DisplayName("OfferedAgo for different time intervals should be correct")
  void offeredAgo_humanReadableFormats() {
    User owner = new User()
        .id("owner-city")
        .firstName("Jane")
        .lastName("Smith")
        .city("Helsinki");

    Book book = Book.builder()
        .id("bookAgo")
        .title("Ago Book")
        .author("Ago Author")
        .genres(List.of(new Genre("History")))
        .language(Language.ENGLISH)
        .description("Ago Desc")
        .condition(Condition.NEW)
        .coverPhotos(List.of("ago-url"))
        .owner(owner)
        .build();

    // Test seconds ago
    book = book.withBookAddedAt(Instant.now().minusSeconds(45))
        .withBookUpdatedAt(Instant.now().minusSeconds(45));
    BookListResponse respSec = new BookListResponse(book);
    assertTrue(respSec.getOfferedAgo().contains("seconds ago"));

    // Test minutes ago
    book = book.withBookUpdatedAt(Instant.now().minusSeconds(120));
    BookListResponse respMin = new BookListResponse(book);
    assertTrue(respMin.getOfferedAgo().contains("mins ago"));

    // Test hours ago
    book = book.withBookUpdatedAt(Instant.now().minusSeconds(7200));
    BookListResponse respHour = new BookListResponse(book);
    assertTrue(respHour.getOfferedAgo().contains("hours ago"));

    // Test days ago
    book = book.withBookUpdatedAt(Instant.now().minusSeconds(172800));
    BookListResponse respDay = new BookListResponse(book);
    assertTrue(respDay.getOfferedAgo().contains("days ago"));

    // Test months ago
    book = book.withBookUpdatedAt(Instant.now().minusSeconds(2592000 * 3)); // 3 months
    BookListResponse respMonth = new BookListResponse(book);
    assertTrue(respMonth.getOfferedAgo().contains("months ago"));

    // Test years ago
    book = book.withBookUpdatedAt(Instant.now().minusSeconds(31536000 * 2)); // 2 years
    BookListResponse respYear = new BookListResponse(book);
    assertTrue(respYear.getOfferedAgo().contains("years ago"));

    // Test bookLocation
    assertEquals("Helsinki", respSec.getBookLocation());
  }
}
