/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.responses;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.kirjaswappi.backend.service.entities.ChatMessage;
import com.kirjaswappi.backend.service.entities.User;

class ChatMessageResponseTest {

  @Test
  @DisplayName("Should create ChatMessageResponse with all fields from ChatMessage entity")
  void shouldCreateChatMessageResponseWithAllFields() {
    // Given
    User sender = User.builder()
        .id("sender123")
        .firstName("John")
        .lastName("Doe")
        .build();

    var chatMessage = ChatMessage.builder()
        .id("msg123")
        .swapRequestId("swap123")
        .sender(sender)
        .message("Hello, is this book still available?")
        .sentAt(Instant.parse("2025-01-01T10:00:00Z"))
        .readByReceiver(false)
        .build();

    // When
    ChatMessageResponse response = new ChatMessageResponse(chatMessage);

    // Then
    assertEquals("msg123", response.getId());
    assertEquals("swap123", response.getSwapRequestId());
    assertEquals("Hello, is this book still available?", response.getMessage());
    assertEquals(Instant.parse("2025-01-01T10:00:00Z"), response.getSentAt());
    assertFalse(response.isReadByReceiver());

    // Check sender
    assertNotNull(response.getSender());
    assertEquals("sender123", response.getSender().getId());
    assertEquals("John Doe", response.getSender().getName());
  }

  @Test
  @DisplayName("Should handle read message")
  void shouldHandleReadMessage() {
    // Given
    User sender = User.builder()
        .id("sender456")
        .firstName("John")
        .lastName("Smith")
        .build();

    ChatMessage chatMessage = ChatMessage.builder()
        .id("msg456")
        .swapRequestId("swap456")
        .sender(sender)
        .message("Yes, it's still available!")
        .sentAt(Instant.parse("2025-01-01T11:00:00Z"))
        .readByReceiver(true)
        .build();

    // When
    ChatMessageResponse response = new ChatMessageResponse(chatMessage);

    // Then
    assertEquals("msg456", response.getId());
    assertEquals("swap456", response.getSwapRequestId());
    assertEquals("Yes, it's still available!", response.getMessage());
    assertEquals(Instant.parse("2025-01-01T11:00:00Z"), response.getSentAt());
    assertTrue(response.isReadByReceiver());

    // Check sender
    assertNotNull(response.getSender());
    assertEquals("sender456", response.getSender().getId());
    assertEquals("Jane Smith", response.getSender().getName());
  }

  @Test
  @DisplayName("Should handle sender with null names")
  void shouldHandleSenderWithNullNames() {
    // Given
    User sender = User.builder()
        .id("sender789")
        .firstName(null)
        .lastName(null)
        .build();

    ChatMessage chatMessage = ChatMessage.builder()
        .id("msg789")
        .swapRequestId("swap789")
        .sender(sender)
        .message("Test message")
        .sentAt(Instant.parse("2025-01-01T12:00:00Z"))
        .readByReceiver(false)
        .build();

    // When
    ChatMessageResponse response = new ChatMessageResponse(chatMessage);

    // Then
    assertEquals("msg789", response.getId());
    assertEquals("Test message", response.getMessage());

    // Check sender with null names
    assertNotNull(response.getSender());
    assertEquals("sender789", response.getSender().getId());
    assertEquals("null null", response.getSender().getName());
  }
}
