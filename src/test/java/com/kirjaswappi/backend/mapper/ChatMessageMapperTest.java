/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.kirjaswappi.backend.jpa.daos.ChatMessageDao;
import com.kirjaswappi.backend.jpa.daos.UserDao;
import com.kirjaswappi.backend.service.entities.ChatMessage;
import com.kirjaswappi.backend.service.entities.User;

class ChatMessageMapperTest {

  @Test
  @DisplayName("Should map ChatMessageDao to ChatMessage entity correctly")
  void shouldMapDaoToEntity() {
    // Given
    var userDao = UserDao.builder()
        .id("user123")
        .firstName("John")
        .lastName("Doe")
        .email("john.doe@example.com")
        .password("hashedPassword")
        .salt("salt")
        .isEmailVerified(true)
        .build();

    var dao = ChatMessageDao.builder()
        .id("msg123")
        .swapRequestId("swap123")
        .sender(userDao)
        .message("Hello, is this book still available?")
        .sentAt(Instant.parse("2025-01-01T10:00:00Z"))
        .readByReceiver(false)
        .build();

    // When
    var entity = ChatMessageMapper.toEntity(dao);

    // Then
    assertNotNull(entity);
    assertEquals("msg123", entity.id());
    assertEquals("swap123", entity.swapRequestId());
    assertNotNull(entity.sender());
    assertEquals("user123", entity.sender().id());
    assertEquals("John", entity.sender().firstName());
    assertEquals("Hello, is this book still available?", entity.message());
    assertEquals(Instant.parse("2025-01-01T10:00:00Z"), entity.sentAt());
    assertFalse(entity.readByReceiver());
  }

  @Test
  @DisplayName("Should map ChatMessage entity to ChatMessageDao correctly")
  void shouldMapEntityToDao() {
    // Given
    var user = User.builder()
        .id("user123")
        .firstName("John")
        .lastName("Doe")
        .email("john.doe@example.com")
        .password("hashedPassword")
        .build();

    var entity = ChatMessage.builder()
        .id("msg123")
        .swapRequestId("swap123")
        .sender(user)
        .message("Hello, is this book still available?")
        .sentAt(Instant.parse("2025-01-01T10:00:00Z"))
        .readByReceiver(false)
        .build();

    // When
    var dao = ChatMessageMapper.toDao(entity);

    // Then
    assertNotNull(dao);
    assertEquals("msg123", dao.id());
    assertEquals("swap123", dao.swapRequestId());
    assertNotNull(dao.sender());
    assertEquals("user123", dao.sender().id());
    assertEquals("John", dao.sender().firstName());
    assertEquals("Hello, is this book still available?", dao.message());
    assertEquals(Instant.parse("2025-01-01T10:00:00Z"), dao.sentAt());
    assertFalse(dao.readByReceiver());
  }

  @Test
  @DisplayName("Should set current time when sentAt is null in entity")
  void shouldSetCurrentTimeWhenSentAtIsNull() {
    // Given
    var user = User.builder()
        .id("user123")
        .firstName("John")
        .lastName("Doe")
        .email("john.doe@example.com")
        .password("hashedPassword")
        .build();

    var entity = ChatMessage.builder()
        .id("msg123")
        .swapRequestId("swap123")
        .sender(user)
        .message("Hello, is this book still available?")
        .sentAt(null) // null sentAt
        .readByReceiver(false)
        .build();

    var beforeMapping = Instant.now();

    // When
    var dao = ChatMessageMapper.toDao(entity);

    var afterMapping = Instant.now();

    // Then
    assertNotNull(dao.sentAt());
    assertTrue(dao.sentAt().isAfter(beforeMapping.minusSeconds(1)));
    assertTrue(dao.sentAt().isBefore(afterMapping.plusSeconds(1)));
  }
}
