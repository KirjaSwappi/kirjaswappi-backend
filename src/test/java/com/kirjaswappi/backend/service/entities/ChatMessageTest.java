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
    var user = new User();
    user.setId("user123");
    user.setFirstName("John");
    user.setLastName("Doe");

    var sentAt = Instant.now();

    // When
    var chatMessage = new ChatMessage();
    chatMessage.setId("msg123");
    chatMessage.setSwapRequestId("swap123");
    chatMessage.setSender(user);
    chatMessage.setMessage("Hello, is this book still available?");
    chatMessage.setSentAt(sentAt);
    chatMessage.setReadByReceiver(false);

    // Then
    assertEquals("msg123", chatMessage.getId());
    assertEquals("swap123", chatMessage.getSwapRequestId());
    assertEquals(user, chatMessage.getSender());
    assertEquals("Hello, is this book still available?", chatMessage.getMessage());
    assertEquals(sentAt, chatMessage.getSentAt());
    assertFalse(chatMessage.isReadByReceiver());
  }

  @Test
  @DisplayName("Should create ChatMessage with default constructor")
  void shouldCreateChatMessageWithDefaultConstructor() {
    // When
    var chatMessage = new ChatMessage();

    // Then
    assertNotNull(chatMessage);
    assertNull(chatMessage.getId());
    assertNull(chatMessage.getSwapRequestId());
    assertNull(chatMessage.getSender());
    assertNull(chatMessage.getMessage());
    assertNull(chatMessage.getSentAt());
    assertFalse(chatMessage.isReadByReceiver()); // boolean defaults to false
  }

  @Test
  @DisplayName("Should allow setting readByReceiver to true")
  void shouldAllowSettingReadByReceiverToTrue() {
    // Given
    var chatMessage = new ChatMessage();

    // When
    chatMessage.setReadByReceiver(true);

    // Then
    assertTrue(chatMessage.isReadByReceiver());
  }
}