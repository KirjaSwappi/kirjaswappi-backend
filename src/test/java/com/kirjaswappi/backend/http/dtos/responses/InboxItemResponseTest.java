/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.responses;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.kirjaswappi.backend.service.entities.Book;
import com.kirjaswappi.backend.service.entities.SwapOffer;
import com.kirjaswappi.backend.service.entities.SwapRequest;
import com.kirjaswappi.backend.service.entities.SwappableBook;
import com.kirjaswappi.backend.service.entities.User;
import com.kirjaswappi.backend.service.enums.Condition;
import com.kirjaswappi.backend.service.enums.SwapStatus;
import com.kirjaswappi.backend.service.enums.SwapType;

class InboxItemResponseTest {

  @Test
  @DisplayName("Should create InboxItemResponse with all fields from SwapRequest entity")
  void shouldCreateInboxItemResponseWithAllFields() {
    // Given
    User sender = new User()
        .id("sender123")
        .firstName("John")
        .lastName("Doe");

    User receiver = new User()
        .id("receiver123")
        .firstName("Jane")
        .lastName("Smith");

    Book book = Book.builder()
        .id("book123")
        .title("Test Book")
        .author("Test Author")
        .condition(Condition.GOOD)
        .build();

    SwappableBook offeredBook = new SwappableBook();
    offeredBook.setId("offered123");
    offeredBook.setTitle("Offered Book");

    SwapOffer swapOffer = new SwapOffer()
        .offeredBook(offeredBook);

    SwapRequest swapRequest = SwapRequest.builder()
        .id("swap123")
        .swapType(SwapType.BY_BOOKS)
        .swapStatus(SwapStatus.PENDING)
        .note("Test note")
        .requestedAt(Instant.parse("2025-01-01T10:00:00Z"))
        .updatedAt(Instant.parse("2025-01-01T11:00:00Z"))
        .sender(sender)
        .receiver(receiver)
        .bookToSwapWith(book)
        .swapOffer(swapOffer)
        .askForGiveaway(false)
        .build();

    // When
    InboxItemResponse response = new InboxItemResponse(swapRequest);

    // Then
    assertEquals("swap123", response.getId());
    assertEquals("ByBooks", response.getSwapType());
    assertEquals("Pending", response.getSwapStatus());
    assertEquals("Test note", response.getNote());
    assertEquals(Instant.parse("2025-01-01T10:00:00Z"), response.getRequestedAt());
    assertEquals(Instant.parse("2025-01-01T11:00:00Z"), response.getUpdatedAt());
    assertFalse(response.isAskForGiveaway());
    assertEquals(0L, response.getUnreadMessageCount());

    // Check sender
    assertNotNull(response.getSender());
    assertEquals("sender123", response.getSender().getId());
    assertEquals("John Doe", response.getSender().getName());

    // Check receiver
    assertNotNull(response.getReceiver());
    assertEquals("receiver123", response.getReceiver().getId());
    assertEquals("Jane Smith", response.getReceiver().getName());

    // Check book
    assertNotNull(response.getBookToSwapWith());
    assertEquals("book123", response.getBookToSwapWith().getId());
    assertEquals("Test Book", response.getBookToSwapWith().getTitle());
    assertEquals("Test Author", response.getBookToSwapWith().getAuthor());
    assertEquals("Good", response.getBookToSwapWith().getCondition());

    // Check swap offer
    assertNotNull(response.getSwapOffer());
    assertEquals("Offered Book", response.getSwapOffer().getOfferedBookTitle());
    assertNull(response.getSwapOffer().getOfferedGenreName());
  }

  @Test
  @DisplayName("Should handle null swap offer")
  void shouldHandleNullSwapOffer() {
    // Given
    User sender = new User()
        .id("sender123")
        .firstName("John")
        .lastName("Doe");

    User receiver = new User()
        .id("receiver123")
        .firstName("Jane")
        .lastName("Smith");

    Book book = Book.builder()
        .id("book123")
        .title("Test Book")
        .author("Test Author")
        .build();

    SwapRequest swapRequest = SwapRequest.builder()
        .id("swap123")
        .swapType(SwapType.GIVE_AWAY)
        .swapStatus(SwapStatus.ACCEPTED)
        .requestedAt(Instant.parse("2025-01-01T10:00:00Z"))
        .updatedAt(Instant.parse("2025-01-01T11:00:00Z"))
        .sender(sender)
        .receiver(receiver)
        .bookToSwapWith(book)
        .swapOffer(null)
        .askForGiveaway(true)
        .build();

    // When
    InboxItemResponse response = new InboxItemResponse(swapRequest);

    // Then
    assertEquals("swap123", response.getId());
    assertEquals("GiveAway", response.getSwapType());
    assertEquals("Accepted", response.getSwapStatus());
    assertTrue(response.isAskForGiveaway());
    assertNull(response.getSwapOffer());
  }

  @Test
  @DisplayName("Should handle null book condition")
  void shouldHandleNullBookCondition() {
    // Given
    User sender = new User()
        .id("sender123")
        .firstName("John")
        .lastName("Doe");

    User receiver = new User()
        .id("receiver123")
        .firstName("Jane")
        .lastName("Smith");

    Book book = Book.builder()
        .id("book123")
        .title("Test Book")
        .author("Test Author")
        .condition(null)
        .build();

    SwapRequest swapRequest = SwapRequest.builder()
        .id("swap123")
        .swapType(SwapType.OPEN_FOR_OFFERS)
        .swapStatus(SwapStatus.REJECTED)
        .requestedAt(Instant.parse("2025-01-01T10:00:00Z"))
        .updatedAt(Instant.parse("2025-01-01T11:00:00Z"))
        .sender(sender)
        .receiver(receiver)
        .bookToSwapWith(book)
        .askForGiveaway(false)
        .build();

    // When
    InboxItemResponse response = new InboxItemResponse(swapRequest);

    // Then
    assertNotNull(response.getBookToSwapWith());
    assertEquals("book123", response.getBookToSwapWith().getId());
    assertEquals("Test Book", response.getBookToSwapWith().getTitle());
    assertEquals("Test Author", response.getBookToSwapWith().getAuthor());
    assertNull(response.getBookToSwapWith().getCondition());
  }
}
