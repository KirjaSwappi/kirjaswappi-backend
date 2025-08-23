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
    User sender = new User();
    sender.setId("sender123");
    sender.setFirstName("John");
    sender.setLastName("Doe");

    ChatMessage chatMessage = new ChatMessage();
    chatMessage.setId("msg123");
    chatMessage.setSwapRequestId("swap123");
    chatMessage.setSender(sender);
    chatMessage.setMessage("Hello, is this book still available?");
    chatMessage.setSentAt(Instant.parse("2025-01-01T10:00:00Z"));
    chatMessage.setReadByReceiver(false);

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
    User sender = new User();
    sender.setId("sender456");
    sender.setFirstName("Jane");
    sender.setLastName("Smith");

    ChatMessage chatMessage = new ChatMessage();
    chatMessage.setId("msg456");
    chatMessage.setSwapRequestId("swap456");
    chatMessage.setSender(sender);
    chatMessage.setMessage("Yes, it's still available!");
    chatMessage.setSentAt(Instant.parse("2025-01-01T11:00:00Z"));
    chatMessage.setReadByReceiver(true);

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
    User sender = new User();
    sender.setId("sender789");
    sender.setFirstName(null);
    sender.setLastName(null);

    ChatMessage chatMessage = new ChatMessage();
    chatMessage.setId("msg789");
    chatMessage.setSwapRequestId("swap789");
    chatMessage.setSender(sender);
    chatMessage.setMessage("Test message");
    chatMessage.setSentAt(Instant.parse("2025-01-01T12:00:00Z"));
    chatMessage.setReadByReceiver(false);

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
