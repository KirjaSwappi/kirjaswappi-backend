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
    var userDao = new UserDao();
    userDao.setId("user123");
    userDao.setFirstName("John");
    userDao.setLastName("Doe");

    var sentAt = Instant.now();

    // When
    var chatMessageDao = new ChatMessageDao();
    chatMessageDao.setId("msg123");
    chatMessageDao.setSwapRequestId("swap123");
    chatMessageDao.setSender(userDao);
    chatMessageDao.setMessage("Hello, is this book still available?");
    chatMessageDao.setSentAt(sentAt);
    chatMessageDao.setReadByReceiver(false);

    // Then
    assertEquals("msg123", chatMessageDao.getId());
    assertEquals("swap123", chatMessageDao.getSwapRequestId());
    assertEquals(userDao, chatMessageDao.getSender());
    assertEquals("Hello, is this book still available?", chatMessageDao.getMessage());
    assertEquals(sentAt, chatMessageDao.getSentAt());
    assertFalse(chatMessageDao.isReadByReceiver());
  }

  @Test
  @DisplayName("Should create ChatMessageDao with AllArgsConstructor")
  void shouldCreateChatMessageDaoWithAllArgsConstructor() {
    // Given
    var userDao = new UserDao();
    userDao.setId("user123");
    userDao.setFirstName("John");
    userDao.setLastName("Doe");

    var sentAt = Instant.now();

    // When
    var chatMessageDao = new ChatMessageDao(
        "msg123",
        "swap123",
        userDao,
        "Hello, is this book still available?",
        sentAt,
        false);

    // Then
    assertEquals("msg123", chatMessageDao.getId());
    assertEquals("swap123", chatMessageDao.getSwapRequestId());
    assertEquals(userDao, chatMessageDao.getSender());
    assertEquals("Hello, is this book still available?", chatMessageDao.getMessage());
    assertEquals(sentAt, chatMessageDao.getSentAt());
    assertFalse(chatMessageDao.isReadByReceiver());
  }

  @Test
  @DisplayName("Should create ChatMessageDao with NoArgsConstructor")
  void shouldCreateChatMessageDaoWithNoArgsConstructor() {
    // When
    var chatMessageDao = new ChatMessageDao();

    // Then
    assertNotNull(chatMessageDao);
    assertNull(chatMessageDao.getId());
    assertNull(chatMessageDao.getSwapRequestId());
    assertNull(chatMessageDao.getSender());
    assertNull(chatMessageDao.getMessage());
    assertNull(chatMessageDao.getSentAt());
    assertFalse(chatMessageDao.isReadByReceiver()); // boolean defaults to false
  }

  @Test
  @DisplayName("Should allow setting readByReceiver to true")
  void shouldAllowSettingReadByReceiverToTrue() {
    // Given
    var chatMessageDao = new ChatMessageDao();

    // When
    chatMessageDao.setReadByReceiver(true);

    // Then
    assertTrue(chatMessageDao.isReadByReceiver());
  }
}