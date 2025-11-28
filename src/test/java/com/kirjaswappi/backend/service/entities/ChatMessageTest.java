/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service.entities;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatMessageTest {

  @Test
  @DisplayName("Should create ChatMessage with all properties")
  void shouldCreateChatMessageWithAllProperties() {
    // Given
    User user = User.builder()
        .id("sender123")
        .firstName("John")
        .lastName("Doe")
        .build();

    var sentAt = Instant.now();

    // When
    var chatMessage = ChatMessage.builder()
        .id("msg123")
        .swapRequestId("swap123")
        .sender(user)
        .message("Hello, is this book still available?")
        .sentAt(sentAt)
        .readByReceiver(false)
        .build();

    // Then
    assertEquals("msg123", chatMessage.id());
    assertEquals("swap123", chatMessage.swapRequestId());
    assertEquals(user, chatMessage.sender());
    assertEquals("Hello, is this book still available?", chatMessage.message());
    assertEquals(sentAt, chatMessage.sentAt());
    assertFalse(chatMessage.readByReceiver());
  }

  @Test
  @DisplayName("Should create ChatMessage with default constructor")
  void shouldCreateChatMessageWithDefaultConstructor() {
    // When
    var chatMessage = ChatMessage.builder().build();

    // Then
    assertNotNull(chatMessage);
    assertNull(chatMessage.id());
    assertNull(chatMessage.swapRequestId());
    assertNull(chatMessage.sender());
    assertNull(chatMessage.message());
    assertNull(chatMessage.sentAt());
    assertFalse(chatMessage.readByReceiver()); // boolean defaults to false
  }

  @Test
  @DisplayName("Should allow setting readByReceiver to true")
  void shouldAllowSettingReadByReceiverToTrue() {
    // Given
    var chatMessage = ChatMessage.builder()
        .readByReceiver(true) // When
        .build();

    // Then
    assertTrue(chatMessage.readByReceiver());
  }
}
