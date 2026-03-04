/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.kirjaswappi.backend.config.TestContainersConfig;
import com.kirjaswappi.backend.jpa.daos.ChatMessageDao;
import com.kirjaswappi.backend.jpa.daos.UserDao;
import com.kirjaswappi.backend.jpa.repositories.ChatMessageRepository;
import com.kirjaswappi.backend.jpa.repositories.UserRepository;

@SpringBootTest
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
class ChatMessageRepositoryIntegrationTest {

  @Autowired
  private ChatMessageRepository chatMessageRepository;

  @Autowired
  private UserRepository userRepository;

  private UserDao userA;
  private UserDao userB;

  private final String SWAP_REQUEST_ID = "swap123";
  private final String OTHER_SWAP_REQUEST_ID = "swap456";

  @BeforeEach
  void setUp() {
    chatMessageRepository.deleteAll();
    userRepository.deleteAll();

    userA = userRepository.save(UserDao.builder()
        .firstName("User")
        .lastName("A")
        .email("user.a@example.com")
        .password("password")
        .salt("salt")
        .isEmailVerified(true)
        .build());

    userB = userRepository.save(UserDao.builder()
        .firstName("User")
        .lastName("B")
        .email("user.b@example.com")
        .password("password")
        .salt("salt")
        .isEmailVerified(true)
        .build());
  }

  @Test
  @DisplayName("Should correctly count unread messages not sent by the specified user")
  void shouldCountUnreadMessagesProperly() {
    // 1. Message from User A (unread) -> Should count for User B
    saveMessage(SWAP_REQUEST_ID, userA, false);

    // 2. Message from User A (read) -> Should not count
    saveMessage(SWAP_REQUEST_ID, userA, true);

    // 3. Message from User B (unread) -> Should not count for User B (User B sent
    // it)
    saveMessage(SWAP_REQUEST_ID, userB, false);

    // 4. Message from User A (unread) in a different swap request -> Should not
    // count
    saveMessage(OTHER_SWAP_REQUEST_ID, userA, false);

    // When we ask "how many unread messages are there for User B in
    // SWAP_REQUEST_ID?"
    // The query excludes messages where sender == User B.
    // It should find 1 message (Message 1).
    long unreadForUserB = chatMessageRepository.countBySwapRequestIdAndReadByReceiverFalseAndSenderIdNot(
        SWAP_REQUEST_ID, new ObjectId(userB.id()));

    assertEquals(1, unreadForUserB, "Should find exactly 1 unread message for User B in the specific swap request");

    // When we check for User A in SWAP_REQUEST_ID
    // It should find 1 unread message (Message 3).
    long unreadForUserA = chatMessageRepository.countBySwapRequestIdAndReadByReceiverFalseAndSenderIdNot(
        SWAP_REQUEST_ID, new ObjectId(userA.id()));

    assertEquals(1, unreadForUserA, "Should find exactly 1 unread message for User A in the specific swap request");
  }

  private ChatMessageDao saveMessage(String swapRequestId, UserDao sender, boolean readByReceiver) {
    return chatMessageRepository.save(ChatMessageDao.builder()
        .swapRequestId(swapRequestId)
        .sender(sender)
        .message("Test Message")
        .sentAt(Instant.now())
        .readByReceiver(readByReceiver)
        .build());
  }
}
