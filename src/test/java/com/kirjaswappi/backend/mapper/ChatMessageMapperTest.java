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
    var userDao = new UserDao();
    userDao.setId("user123");
    userDao.setFirstName("John");
    userDao.setLastName("Doe");
    userDao.setEmail("john.doe@example.com");
    userDao.setPassword("hashedPassword");
    userDao.setSalt("salt");
    userDao.setEmailVerified(true);

    var dao = new ChatMessageDao();
    dao.setId("msg123");
    dao.setSwapRequestId("swap123");
    dao.setSender(userDao);
    dao.setMessage("Hello, is this book still available?");
    dao.setSentAt(Instant.parse("2025-01-01T10:00:00Z"));
    dao.setReadByReceiver(false);

    // When
    var entity = ChatMessageMapper.toEntity(dao);

    // Then
    assertNotNull(entity);
    assertEquals("msg123", entity.getId());
    assertEquals("swap123", entity.getSwapRequestId());
    assertNotNull(entity.getSender());
    assertEquals("user123", entity.getSender().getId());
    assertEquals("John", entity.getSender().getFirstName());
    assertEquals("Hello, is this book still available?", entity.getMessage());
    assertEquals(Instant.parse("2025-01-01T10:00:00Z"), entity.getSentAt());
    assertFalse(entity.isReadByReceiver());
  }

  @Test
  @DisplayName("Should map ChatMessage entity to ChatMessageDao correctly")
  void shouldMapEntityToDao() {
    // Given
    var user = new User();
    user.setId("user123");
    user.setFirstName("John");
    user.setLastName("Doe");
    user.setEmail("john.doe@example.com");
    user.setPassword("hashedPassword");

    var entity = new ChatMessage();
    entity.setId("msg123");
    entity.setSwapRequestId("swap123");
    entity.setSender(user);
    entity.setMessage("Hello, is this book still available?");
    entity.setSentAt(Instant.parse("2025-01-01T10:00:00Z"));
    entity.setReadByReceiver(false);

    // When
    var dao = ChatMessageMapper.toDao(entity);

    // Then
    assertNotNull(dao);
    assertEquals("msg123", dao.getId());
    assertEquals("swap123", dao.getSwapRequestId());
    assertNotNull(dao.getSender());
    assertEquals("user123", dao.getSender().getId());
    assertEquals("John", dao.getSender().getFirstName());
    assertEquals("Hello, is this book still available?", dao.getMessage());
    assertEquals(Instant.parse("2025-01-01T10:00:00Z"), dao.getSentAt());
    assertFalse(dao.isReadByReceiver());
  }

  @Test
  @DisplayName("Should set current time when sentAt is null in entity")
  void shouldSetCurrentTimeWhenSentAtIsNull() {
    // Given
    var user = new User();
    user.setId("user123");
    user.setFirstName("John");
    user.setLastName("Doe");
    user.setEmail("john.doe@example.com");
    user.setPassword("hashedPassword");

    var entity = new ChatMessage();
    entity.setId("msg123");
    entity.setSwapRequestId("swap123");
    entity.setSender(user);
    entity.setMessage("Hello, is this book still available?");
    entity.setSentAt(null); // null sentAt
    entity.setReadByReceiver(false);

    var beforeMapping = Instant.now();

    // When
    var dao = ChatMessageMapper.toDao(entity);

    var afterMapping = Instant.now();

    // Then
    assertNotNull(dao.getSentAt());
    assertTrue(dao.getSentAt().isAfter(beforeMapping.minusSeconds(1)));
    assertTrue(dao.getSentAt().isBefore(afterMapping.plusSeconds(1)));
  }
}