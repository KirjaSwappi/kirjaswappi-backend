/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kirjaswappi.backend.http.dtos.responses.ChatMessageResponse;
import com.kirjaswappi.backend.http.dtos.responses.InboxItemResponse;
import com.kirjaswappi.backend.jpa.daos.*;
import com.kirjaswappi.backend.jpa.repositories.*;
import com.kirjaswappi.backend.service.entities.*;
import com.kirjaswappi.backend.service.enums.*;

@ExtendWith(MockitoExtension.class)
public class ReadStatusTrackingTest {

  @Mock
  private SwapRequestRepository swapRequestRepository;

  @Mock
  private ChatMessageRepository chatMessageRepository;

  @Mock
  private UserService userService;

  @InjectMocks
  private InboxService inboxService;

  @InjectMocks
  private ChatService chatService;

  private UserDao senderUser;
  private UserDao receiverUser;
  private SwapRequestDao testSwapRequest;

  @BeforeEach
  void setUp() {
    // Create test users
    senderUser = new UserDao()
        .id("sender123")
        .firstName("John")
        .lastName("Sender");

    receiverUser = new UserDao()
        .id("receiver123")
        .firstName("Jane")
        .lastName("Receiver");

    // Create test swap request
    testSwapRequest = new SwapRequestDao()
        .id("swap123")
        .sender(senderUser)
        .receiver(receiverUser)
        .swapStatus("PENDING")
        .requestedAt(Instant.now())
        .updatedAt(Instant.now());
  }

  @Test
  void testMarkInboxItemAsRead_ReceiverMarksAsRead() {
    // Given
    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(testSwapRequest));
    when(swapRequestRepository.save(any(SwapRequestDao.class))).thenReturn(testSwapRequest);

    // When
    inboxService.markInboxItemAsRead("swap123", "receiver123");

    // Then
    verify(swapRequestRepository).save(argThat(swapRequest -> swapRequest.readByReceiverAt() != null &&
        swapRequest.readBySenderAt() == null));
  }

  @Test
  void testMarkInboxItemAsRead_SenderMarksAsRead() {
    // Given
    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(testSwapRequest));
    when(swapRequestRepository.save(any(SwapRequestDao.class))).thenReturn(testSwapRequest);

    // When
    inboxService.markInboxItemAsRead("swap123", "sender123");

    // Then
    verify(swapRequestRepository).save(argThat(swapRequest -> swapRequest.readBySenderAt() != null &&
        swapRequest.readByReceiverAt() == null));
  }

  @Test
  void testIsInboxItemUnread_InitiallyUnread() {
    // Given
    User receiver = new User().id("receiver123");

    SwapRequest swapRequest = SwapRequest.builder()
        .receiver(receiver)
        .readByReceiverAt(null)
        .build();

    // When
    boolean isUnread = inboxService.isInboxItemUnread(swapRequest, "receiver123");

    // Then
    assertTrue(isUnread);
  }

  @Test
  void testIsInboxItemUnread_AfterRead() {
    // Given

    User receiver = new User().id("receiver123");
    SwapRequest swapRequest = SwapRequest.builder()
        .receiver(receiver)
        .readByReceiverAt(Instant.now())
        .build();

    // When
    boolean isUnread = inboxService.isInboxItemUnread(swapRequest, "receiver123");

    // Then
    assertFalse(isUnread);
  }

  @Test
  void testChatMessageResponseWithCurrentUser() {
    // Given

    User sender = User.builder()
        .id("sender123")
        .firstName("John")
        .lastName("Sender")
        .build();

    ChatMessage message = ChatMessage.builder()
        .id("msg123")
        .swapRequestId("swap123")
        .message("Test message")
        .sentAt(Instant.now())
        .readByReceiver(false)
        .sender(sender)
        .build();

    // When - viewing as sender (own message)
    ChatMessageResponse response1 = new ChatMessageResponse(message, "sender123");

    // When - viewing as receiver (not own message)
    ChatMessageResponse response2 = new ChatMessageResponse(message, "receiver123");

    // Then
    assertTrue(response1.isOwnMessage());
    assertFalse(response2.isOwnMessage());
  }

  @Test
  void testInboxItemResponseNotificationIndicators() {
    // Given
    User sender = User.builder()
        .id("sender123")
        .firstName("John")
        .lastName("Sender")
        .build();

    User receiver = User.builder()
        .id("receiver123")
        .firstName("Jane")
        .lastName("Receiver")
        .build();

    Book book = Book.builder()
        .id("book123")
        .title("Test Book")
        .author("Test Author")
        .build();

    SwapRequest swapRequest = SwapRequest.builder()
        .id("swap123")
        .sender(sender)
        .receiver(receiver)
        .bookToSwapWith(book)
        .swapType(SwapType.BY_BOOKS)
        .swapStatus(SwapStatus.PENDING)
        .requestedAt(Instant.now())
        .updatedAt(Instant.now())
        .build();

    // When
    InboxItemResponse response = new InboxItemResponse(swapRequest);
    response.setUnreadMessageCount(3);
    response.setUnread(true);
    response.setHasNewMessages(true);

    // Then
    assertEquals(3, response.getUnreadMessageCount());
    assertTrue(response.isUnread());
    assertTrue(response.isHasNewMessages());
  }
}
