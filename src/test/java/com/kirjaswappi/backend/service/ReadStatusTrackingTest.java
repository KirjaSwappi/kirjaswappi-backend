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
    senderUser = new UserDao();
    senderUser.setId("sender123");
    senderUser.setFirstName("John");
    senderUser.setLastName("Sender");

    receiverUser = new UserDao();
    receiverUser.setId("receiver123");
    receiverUser.setFirstName("Jane");
    receiverUser.setLastName("Receiver");

    // Create test swap request
    testSwapRequest = new SwapRequestDao();
    testSwapRequest.setId("swap123");
    testSwapRequest.setSender(senderUser);
    testSwapRequest.setReceiver(receiverUser);
    testSwapRequest.setSwapStatus("PENDING");
    testSwapRequest.setRequestedAt(Instant.now());
    testSwapRequest.setUpdatedAt(Instant.now());
  }

  @Test
  void testMarkInboxItemAsRead_ReceiverMarksAsRead() {
    // Given
    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(testSwapRequest));
    when(swapRequestRepository.save(any(SwapRequestDao.class))).thenReturn(testSwapRequest);

    // When
    inboxService.markInboxItemAsRead("swap123", "receiver123");

    // Then
    verify(swapRequestRepository).save(argThat(swapRequest -> swapRequest.getReadByReceiverAt() != null &&
        swapRequest.getReadBySenderAt() == null));
  }

  @Test
  void testMarkInboxItemAsRead_SenderMarksAsRead() {
    // Given
    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(testSwapRequest));
    when(swapRequestRepository.save(any(SwapRequestDao.class))).thenReturn(testSwapRequest);

    // When
    inboxService.markInboxItemAsRead("swap123", "sender123");

    // Then
    verify(swapRequestRepository).save(argThat(swapRequest -> swapRequest.getReadBySenderAt() != null &&
        swapRequest.getReadByReceiverAt() == null));
  }

  @Test
  void testIsInboxItemUnread_InitiallyUnread() {
    // Given
    SwapRequest swapRequest = new SwapRequest();
    User receiver = new User();
    receiver.setId("receiver123");
    swapRequest.setReceiver(receiver);
    swapRequest.setReadByReceiverAt(null);

    // When
    boolean isUnread = inboxService.isInboxItemUnread(swapRequest, "receiver123");

    // Then
    assertTrue(isUnread);
  }

  @Test
  void testIsInboxItemUnread_AfterRead() {
    // Given
    SwapRequest swapRequest = new SwapRequest();
    User receiver = new User();
    receiver.setId("receiver123");
    swapRequest.setReceiver(receiver);
    swapRequest.setReadByReceiverAt(Instant.now());

    // When
    boolean isUnread = inboxService.isInboxItemUnread(swapRequest, "receiver123");

    // Then
    assertFalse(isUnread);
  }

  @Test
  void testChatMessageResponseWithCurrentUser() {
    // Given
    ChatMessage message = new ChatMessage();
    message.setId("msg123");
    message.setSwapRequestId("swap123");
    message.setMessage("Test message");
    message.setSentAt(Instant.now());
    message.setReadByReceiver(false);

    User sender = new User();
    sender.setId("sender123");
    sender.setFirstName("John");
    sender.setLastName("Sender");
    message.setSender(sender);

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
    SwapRequest swapRequest = new SwapRequest();
    swapRequest.setId("swap123");

    User sender = new User();
    sender.setId("sender123");
    sender.setFirstName("John");
    sender.setLastName("Sender");
    swapRequest.setSender(sender);

    User receiver = new User();
    receiver.setId("receiver123");
    receiver.setFirstName("Jane");
    receiver.setLastName("Receiver");
    swapRequest.setReceiver(receiver);

    Book book = new Book();
    book.setId("book123");
    book.setTitle("Test Book");
    book.setAuthor("Test Author");
    swapRequest.setBookToSwapWith(book);

    swapRequest.setSwapType(SwapType.BY_BOOKS);
    swapRequest.setSwapStatus(SwapStatus.PENDING);
    swapRequest.setRequestedAt(Instant.now());
    swapRequest.setUpdatedAt(Instant.now());

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
