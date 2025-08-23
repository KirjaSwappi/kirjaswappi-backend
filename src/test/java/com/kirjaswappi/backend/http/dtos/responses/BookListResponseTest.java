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
    User owner = new User();
    owner.setId("owner1");
    owner.setFirstName("John");
    owner.setLastName("Doe");
    owner.setCity("Espoo");
    Book book = new Book();
    book.setId("book1");
    book.setTitle("Test Book");
    book.setAuthor("Author");
    book.setGenres(List.of(new Genre("Fiction")));
    book.setLanguage(Language.ENGLISH);
    book.setDescription("A book");
    book.setCondition(Condition.NEW);
    book.setCoverPhotos(List.of("url"));
    book.setOwner(owner);
    book.setBookAddedAt(Instant.now().minusSeconds(3600)); // 1 hour ago
    book.setBookUpdatedAt(Instant.now().minusSeconds(3600)); // 1 hour ago
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
    Book book = new Book();
    book.setId("book2");
    book.setTitle("T");
    book.setAuthor("A");
    book.setGenres(List.of());
    book.setLanguage(Language.ENGLISH);
    book.setDescription("D");
    book.setCondition(Condition.NEW);
    book.setCoverPhotos(List.of("U"));
    book.setOwner(null);
    book.setBookAddedAt(Instant.now().minusSeconds(60)); // 1 min ago
    book.setBookUpdatedAt(Instant.now().minusSeconds(60)); // 1 min ago
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
    User owner = new User();
    owner.setId("owner2");
    owner.setFirstName(null);
    owner.setLastName(null);
    owner.setCity(null);
    Book book = new Book();
    book.setId("id");
    book.setTitle("t");
    book.setAuthor("a");
    book.setGenres(List.of());
    book.setLanguage(Language.ENGLISH);
    book.setDescription("d");
    book.setCondition(Condition.NEW);
    book.setCoverPhotos(List.of("u"));
    book.setOwner(owner);
    book.setBookAddedAt(Instant.now().minusSeconds(86400)); // 1 day ago
    book.setBookUpdatedAt(Instant.now().minusSeconds(86400)); // 1 day ago
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
    Book book = new Book();
    book.setId("book3");
    book.setTitle("Title");
    book.setAuthor("Author");
    book.setGenres(List.of(new Genre("Fiction")));
    book.setLanguage(Language.ENGLISH);
    book.setDescription("Desc");
    book.setCondition(Condition.GOOD);
    book.setCoverPhotos(List.of("url"));
    book.setOwner(null);
    book.setBookAddedAt(Instant.now().minusSeconds(2592000));
    book.setBookUpdatedAt(Instant.now().minusSeconds(2592000)); // 1 month ago
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
    User owner = new User();
    owner.setId("owner-city");
    owner.setFirstName("Jane");
    owner.setLastName("Smith");
    owner.setCity("Helsinki");
    Book book = new Book();
    book.setId("bookAgo");
    book.setTitle("Ago Book");
    book.setAuthor("Ago Author");
    book.setGenres(List.of(new Genre("History")));
    book.setLanguage(Language.ENGLISH);
    book.setDescription("Ago Desc");
    book.setCondition(Condition.NEW);
    book.setCoverPhotos(List.of("ago-url"));
    book.setOwner(owner);

    // Test seconds ago
    book.setBookAddedAt(Instant.now().minusSeconds(45));
    book.setBookUpdatedAt(Instant.now().minusSeconds(45));
    BookListResponse respSec = new BookListResponse(book);
    assertTrue(respSec.getOfferedAgo().contains("seconds ago"));

    // Test minutes ago
    book.setBookUpdatedAt(Instant.now().minusSeconds(120));
    BookListResponse respMin = new BookListResponse(book);
    assertTrue(respMin.getOfferedAgo().contains("mins ago"));

    // Test hours ago
    book.setBookUpdatedAt(Instant.now().minusSeconds(7200));
    BookListResponse respHour = new BookListResponse(book);
    assertTrue(respHour.getOfferedAgo().contains("hours ago"));

    // Test days ago
    book.setBookUpdatedAt(Instant.now().minusSeconds(172800));
    BookListResponse respDay = new BookListResponse(book);
    assertTrue(respDay.getOfferedAgo().contains("days ago"));

    // Test months ago
    book.setBookUpdatedAt(Instant.now().minusSeconds(2592000 * 3)); // 3 months
    BookListResponse respMonth = new BookListResponse(book);
    assertTrue(respMonth.getOfferedAgo().contains("months ago"));

    // Test years ago
    book.setBookUpdatedAt(Instant.now().minusSeconds(31536000 * 2)); // 2 years
    BookListResponse respYear = new BookListResponse(book);
    assertTrue(respYear.getOfferedAgo().contains("years ago"));

    // Test bookLocation
    assertEquals("Helsinki", respSec.getBookLocation());
  }
}
