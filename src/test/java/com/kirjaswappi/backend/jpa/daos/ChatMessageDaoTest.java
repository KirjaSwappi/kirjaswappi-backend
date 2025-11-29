/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.jpa.daos;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatMessageDaoTest {

  @Test
  @DisplayName("Should create ChatMessageDao with all properties")
  void shouldCreateChatMessageDaoWithAllProperties() {
    // Given
    var userDao = UserDao.builder()
        .id("user123")
        .firstName("John")
        .lastName("Doe")
        .build();

    var sentAt = Instant.now();

    // When
    var chatMessageDao = ChatMessageDao.builder()
        .id("msg123")
        .swapRequestId("swap123")
        .sender(userDao)
        .message("Hello, is this book still available?")
        .sentAt(sentAt)
        .readByReceiver(false)
        .build();

    // Then
    assertEquals("msg123", chatMessageDao.id());
    assertEquals("swap123", chatMessageDao.swapRequestId());
    assertEquals(userDao, chatMessageDao.sender());
    assertEquals("Hello, is this book still available?", chatMessageDao.message());
    assertEquals(sentAt, chatMessageDao.sentAt());
    assertFalse(chatMessageDao.readByReceiver());
  }

  @Test
  @DisplayName("Should create ChatMessageDao with AllArgsConstructor")
  void shouldCreateChatMessageDaoWithAllArgsConstructor() {
    // Given
    var userDao = UserDao.builder()
        .id("user123")
        .firstName("John")
        .lastName("Doe")
        .build();

    var sentAt = Instant.now();

    // When
    var chatMessageDao = new ChatMessageDao(
        "msg123",
        "swap123",
        userDao,
        "Hello, is this book still available?",
        null, // imageIds
        sentAt,
        false);

    // Then
    assertEquals("msg123", chatMessageDao.id());
    assertEquals("swap123", chatMessageDao.swapRequestId());
    assertEquals(userDao, chatMessageDao.sender());
    assertEquals("Hello, is this book still available?", chatMessageDao.message());
    assertNull(chatMessageDao.imageIds()); // Test the new field
    assertEquals(sentAt, chatMessageDao.sentAt());
    assertFalse(chatMessageDao.readByReceiver());
  }

  @Test
  @DisplayName("Should create ChatMessageDao with NoArgsConstructor")
  void shouldCreateChatMessageDaoWithNoArgsConstructor() {
    // When
    var chatMessageDao = new ChatMessageDao();

    // Then
    assertNotNull(chatMessageDao);
    assertNull(chatMessageDao.id());
    assertNull(chatMessageDao.swapRequestId());
    assertNull(chatMessageDao.sender());
    assertNull(chatMessageDao.message());
    assertNull(chatMessageDao.sentAt());
    assertFalse(chatMessageDao.readByReceiver()); // boolean defaults to false
  }

  @Test
  @DisplayName("Should allow setting readByReceiver to true")
  void shouldAllowSettingReadByReceiverToTrue() {
    // Given
    var chatMessageDao = new ChatMessageDao();

    // When
    chatMessageDao.readByReceiver(true);

    // Then
    assertTrue(chatMessageDao.readByReceiver());
  }
}
