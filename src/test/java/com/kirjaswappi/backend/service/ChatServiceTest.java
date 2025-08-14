/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.kirjaswappi.backend.jpa.daos.ChatMessageDao;
import com.kirjaswappi.backend.jpa.daos.SwapRequestDao;
import com.kirjaswappi.backend.jpa.daos.UserDao;
import com.kirjaswappi.backend.jpa.repositories.ChatMessageRepository;
import com.kirjaswappi.backend.jpa.repositories.SwapRequestRepository;
import com.kirjaswappi.backend.service.entities.ChatMessage;
import com.kirjaswappi.backend.service.entities.User;
import com.kirjaswappi.backend.service.exceptions.ChatAccessDeniedException;
import com.kirjaswappi.backend.service.exceptions.SwapRequestNotFoundException;

class ChatServiceTest {
  @Mock
  private ChatMessageRepository chatMessageRepository;
  @Mock
  private SwapRequestRepository swapRequestRepository;
  @Mock
  private UserService userService;
  @InjectMocks
  private ChatService chatService;

  private SwapRequestDao swapRequestDao;
  private UserDao senderDao;
  private UserDao receiverDao;
  private User senderEntity;
  private ChatMessageDao chatMessageDao;

  @BeforeEach
  @DisplayName("Setup mocks for ChatService tests")
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // Create test users
    senderDao = new UserDao();
    senderDao.setId("sender123");
    senderDao.setFirstName("John");
    senderDao.setLastName("Sender");

    receiverDao = new UserDao();
    receiverDao.setId("receiver123");
    receiverDao.setFirstName("Jane");
    receiverDao.setLastName("Receiver");

    senderEntity = new User();
    senderEntity.setId("sender123");
    senderEntity.setFirstName("John");
    senderEntity.setLastName("Sender");

    // Create test swap request
    swapRequestDao = new SwapRequestDao();
    swapRequestDao.setId("swap123");
    swapRequestDao.setSender(senderDao);
    swapRequestDao.setReceiver(receiverDao);

    // Create test chat message
    chatMessageDao = new ChatMessageDao();
    chatMessageDao.setId("msg123");
    chatMessageDao.setSwapRequestId("swap123");
    chatMessageDao.setSender(senderDao);
    chatMessageDao.setMessage("Hello, is this book still available?");
    chatMessageDao.setSentAt(Instant.now());
    chatMessageDao.setReadByReceiver(false);
  }

  @Test
  @DisplayName("Should get chat messages when user has access")
  void shouldGetChatMessagesWhenUserHasAccess() {
    // Given
    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(swapRequestDao));
    when(chatMessageRepository.findBySwapRequestIdOrderBySentAtAsc("swap123"))
        .thenReturn(Arrays.asList(chatMessageDao));

    // When
    List<ChatMessage> result = chatService.getChatMessages("swap123", "sender123");

    // Then
    assertEquals(1, result.size());
    assertEquals("msg123", result.get(0).getId());
    assertEquals("Hello, is this book still available?", result.get(0).getMessage());
    verify(swapRequestRepository).findById("swap123");
    verify(chatMessageRepository).findBySwapRequestIdOrderBySentAtAsc("swap123");
  }

  @Test
  @DisplayName("Should throw SwapRequestNotFoundException when swap request does not exist")
  void shouldThrowSwapRequestNotFoundExceptionWhenSwapRequestDoesNotExist() {
    // Given
    when(swapRequestRepository.findById("nonexistent")).thenReturn(Optional.empty());

    // When & Then
    assertThrows(SwapRequestNotFoundException.class,
        () -> chatService.getChatMessages("nonexistent", "sender123"));
    verify(swapRequestRepository).findById("nonexistent");
    verifyNoInteractions(chatMessageRepository);
  }

  @Test
  @DisplayName("Should throw ChatAccessDeniedException when user has no access")
  void shouldThrowChatAccessDeniedExceptionWhenUserHasNoAccess() {
    // Given
    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(swapRequestDao));

    // When & Then
    assertThrows(ChatAccessDeniedException.class,
        () -> chatService.getChatMessages("swap123", "unauthorized123"));
    verify(swapRequestRepository).findById("swap123");
    verifyNoInteractions(chatMessageRepository);
  }

  @Test
  @DisplayName("Should send message when user has access")
  void shouldSendMessageWhenUserHasAccess() {
    // Given
    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(swapRequestDao));
    when(userService.getUser("sender123")).thenReturn(senderEntity);
    when(chatMessageRepository.save(any(ChatMessageDao.class))).thenReturn(chatMessageDao);

    // When
    ChatMessage result = chatService.sendMessage("swap123", "sender123", "Hello there!");

    // Then
    assertNotNull(result);
    assertEquals("msg123", result.getId());
    verify(swapRequestRepository).findById("swap123");
    verify(userService).getUser("sender123");
    verify(chatMessageRepository).save(any(ChatMessageDao.class));
  }

  @Test
  @DisplayName("Should throw exception when sending empty message")
  void shouldThrowExceptionWhenSendingEmptyMessage() {
    // Given
    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(swapRequestDao));

    // When & Then
    assertThrows(IllegalArgumentException.class,
        () -> chatService.sendMessage("swap123", "sender123", ""));
    assertThrows(IllegalArgumentException.class,
        () -> chatService.sendMessage("swap123", "sender123", "   "));
    assertThrows(IllegalArgumentException.class,
        () -> chatService.sendMessage("swap123", "sender123", null));

    verify(swapRequestRepository, times(3)).findById("swap123");
    verifyNoInteractions(userService);
    verifyNoInteractions(chatMessageRepository);
  }

  @Test
  @DisplayName("Should throw ChatAccessDeniedException when unauthorized user tries to send message")
  void shouldThrowChatAccessDeniedExceptionWhenUnauthorizedUserTriesToSendMessage() {
    // Given
    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(swapRequestDao));

    // When & Then
    assertThrows(ChatAccessDeniedException.class,
        () -> chatService.sendMessage("swap123", "unauthorized123", "Hello"));
    verify(swapRequestRepository).findById("swap123");
    verifyNoInteractions(userService);
    verifyNoInteractions(chatMessageRepository);
  }

  @Test
  @DisplayName("Should mark messages as read when user has access")
  void shouldMarkMessagesAsReadWhenUserHasAccess() {
    // Given
    ChatMessageDao unreadMessage = new ChatMessageDao();
    unreadMessage.setId("msg456");
    unreadMessage.setSender(senderDao); // Message from sender
    unreadMessage.setReadByReceiver(false);

    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(swapRequestDao));
    when(chatMessageRepository.findBySwapRequestIdOrderBySentAtAsc("swap123"))
        .thenReturn(Arrays.asList(unreadMessage));
    when(chatMessageRepository.save(any(ChatMessageDao.class))).thenReturn(unreadMessage);

    // When - receiver marks messages as read
    chatService.markMessagesAsRead("swap123", "receiver123");

    // Then
    verify(swapRequestRepository).findById("swap123");
    verify(chatMessageRepository).findBySwapRequestIdOrderBySentAtAsc("swap123");
    verify(chatMessageRepository).save(unreadMessage);
    assertTrue(unreadMessage.isReadByReceiver());
  }

  @Test
  @DisplayName("Should get unread message count when user has access")
  void shouldGetUnreadMessageCountWhenUserHasAccess() {
    // Given
    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(swapRequestDao));
    when(chatMessageRepository.countBySwapRequestIdAndSenderIdNotAndReadByReceiverFalse("swap123", "receiver123"))
        .thenReturn(3L);

    // When
    long count = chatService.getUnreadMessageCount("swap123", "receiver123");

    // Then
    assertEquals(3L, count);
    verify(swapRequestRepository).findById("swap123");
    verify(chatMessageRepository).countBySwapRequestIdAndSenderIdNotAndReadByReceiverFalse("swap123", "receiver123");
  }

  @Test
  @DisplayName("Should throw ChatAccessDeniedException when unauthorized user tries to get unread count")
  void shouldThrowChatAccessDeniedExceptionWhenUnauthorizedUserTriesToGetUnreadCount() {
    // Given
    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(swapRequestDao));

    // When & Then
    assertThrows(ChatAccessDeniedException.class,
        () -> chatService.getUnreadMessageCount("swap123", "unauthorized123"));
    verify(swapRequestRepository).findById("swap123");
    verifyNoInteractions(chatMessageRepository);
  }

  @Test
  @DisplayName("Should allow receiver to access chat")
  void shouldAllowReceiverToAccessChat() {
    // Given
    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(swapRequestDao));
    when(chatMessageRepository.findBySwapRequestIdOrderBySentAtAsc("swap123"))
        .thenReturn(Arrays.asList(chatMessageDao));

    // When
    List<ChatMessage> result = chatService.getChatMessages("swap123", "receiver123");

    // Then
    assertEquals(1, result.size());
    verify(swapRequestRepository).findById("swap123");
    verify(chatMessageRepository).findBySwapRequestIdOrderBySentAtAsc("swap123");
  }
}