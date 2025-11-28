/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.kirjaswappi.backend.common.service.ImageService;
import com.kirjaswappi.backend.jpa.daos.BookDao;
import com.kirjaswappi.backend.jpa.daos.ChatMessageDao;
import com.kirjaswappi.backend.jpa.daos.SwapRequestDao;
import com.kirjaswappi.backend.jpa.daos.UserDao;
import com.kirjaswappi.backend.jpa.repositories.ChatMessageRepository;
import com.kirjaswappi.backend.jpa.repositories.SwapRequestRepository;
import com.kirjaswappi.backend.service.entities.ChatMessage;
import com.kirjaswappi.backend.service.entities.SwapRequest;
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
  @Mock
  private ImageService imageService;
  @Mock
  private SimpMessagingTemplate messagingTemplate;
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
    senderDao.setId("64e8f5d1a2b3c4d5e6f78905");
    senderDao.setFirstName("John");
    senderDao.setLastName("Sender");

    receiverDao = new UserDao();
    receiverDao.setId("64e8f5d1a2b3c4d5e6f78901");
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
    swapRequestDao.setSwapType("ByBooks");
    swapRequestDao.setSwapStatus("PENDING");

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
    List<ChatMessage> result = chatService.getChatMessages("swap123", "64e8f5d1a2b3c4d5e6f78905");

    // Then
    assertEquals(1, result.size());
    assertEquals("msg123", result.get(0).getId());
    assertEquals("Hello, is this book still available?", result.get(0).getMessage());
    verify(swapRequestRepository, times(2)).findById("swap123"); // Called twice: once in getChatMessages, once in
                                                                 // markMessagesAsRead
    verify(chatMessageRepository, times(2)).findBySwapRequestIdOrderBySentAtAsc("swap123"); // Called twice: once for
                                                                                            // getting messages, once
                                                                                            // for marking as read
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
    when(userService.getUser("64e8f5d1a2b3c4d5e6f78905")).thenReturn(senderEntity);
    when(chatMessageRepository.save(any(ChatMessageDao.class))).thenReturn(chatMessageDao);

    // When
    ChatMessage result = chatService.sendMessage("swap123", "64e8f5d1a2b3c4d5e6f78905", "Hello there!");

    // Then
    assertNotNull(result);
    assertEquals("msg123", result.getId());
    verify(swapRequestRepository).findById("swap123");
    verify(userService).getUser("64e8f5d1a2b3c4d5e6f78905");
    verify(chatMessageRepository).save(any(ChatMessageDao.class));
  }

  @Test
  @DisplayName("Should throw exception when sending empty message")
  void shouldThrowExceptionWhenSendingEmptyMessage() {
    // Given
    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(swapRequestDao));

    // When & Then
    assertThrows(IllegalArgumentException.class,
        () -> chatService.sendMessage("swap123", "64e8f5d1a2b3c4d5e6f78905", ""));
    assertThrows(IllegalArgumentException.class,
        () -> chatService.sendMessage("swap123", "64e8f5d1a2b3c4d5e6f78905", "   "));
    assertThrows(IllegalArgumentException.class,
        () -> chatService.sendMessage("swap123", "64e8f5d1a2b3c4d5e6f78905", null));

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
    chatService.markMessagesAsRead("swap123", "64e8f5d1a2b3c4d5e6f78901");

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
    String receiverHex = "64e8f5d1a2b3c4d5e6f78901";

    when(chatMessageRepository.countBySwapRequestIdAndReadByReceiverFalseAndSenderIdNot("swap123", receiverHex))
        .thenReturn(3L);

    // When
    long count = chatService.getUnreadMessageCount("swap123", receiverHex);

    // Then
    assertEquals(3L, count);
    verify(swapRequestRepository).findById("swap123");
    verify(chatMessageRepository).countBySwapRequestIdAndReadByReceiverFalseAndSenderIdNot("swap123", receiverHex);
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
    List<ChatMessage> result = chatService.getChatMessages("swap123", "64e8f5d1a2b3c4d5e6f78901");

    // Then
    assertEquals(1, result.size());
    verify(swapRequestRepository, times(2)).findById("swap123"); // Called twice: once in getChatMessages, once in
                                                                 // markMessagesAsRead
    verify(chatMessageRepository, times(2)).findBySwapRequestIdOrderBySentAtAsc("swap123"); // Called twice: once for
                                                                                            // getting messages, once
                                                                                            // for marking as read
  }

  @Test
  @DisplayName("Should send message with images when user has access")
  void shouldSendMessageWithImagesWhenUserHasAccess() {
    // Given
    MockMultipartFile image1 = new MockMultipartFile("image1", "test1.jpg", "image/jpeg", "test image 1".getBytes());
    MockMultipartFile image2 = new MockMultipartFile("image2", "test2.jpg", "image/jpeg", "test image 2".getBytes());
    List<MultipartFile> images = Arrays.asList(image1, image2);

    ChatMessageDao savedMessageDao = new ChatMessageDao();
    savedMessageDao.setId("msg789");
    savedMessageDao.setSwapRequestId("swap123");
    savedMessageDao.setSender(senderDao);
    savedMessageDao.setMessage("Check out these photos!");
    savedMessageDao.setImageIds(Arrays.asList("image-id-1", "image-id-2"));
    savedMessageDao.setSentAt(Instant.now());
    savedMessageDao.setReadByReceiver(false);

    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(swapRequestDao));
    when(userService.getUser("64e8f5d1a2b3c4d5e6f78905")).thenReturn(senderEntity);
    doNothing().when(imageService).uploadImage(any(MultipartFile.class), anyString());
    when(imageService.getDownloadUrl(anyString())).thenReturn("https://example.com/image.jpg");
    when(chatMessageRepository.save(any(ChatMessageDao.class))).thenReturn(savedMessageDao);

    // When
    ChatMessage result = chatService.sendMessage("swap123", "64e8f5d1a2b3c4d5e6f78905", "Check out these photos!",
        images);

    // Then
    assertNotNull(result);
    assertEquals("msg789", result.getId());
    assertEquals("Check out these photos!", result.getMessage());
    assertNotNull(result.getImageIds());
    assertEquals(2, result.getImageIds().size());
    verify(swapRequestRepository).findById("swap123");
    verify(userService).getUser("64e8f5d1a2b3c4d5e6f78905");
    verify(imageService, times(2)).uploadImage(any(MultipartFile.class), anyString());
    verify(chatMessageRepository).save(any(ChatMessageDao.class));
  }

  @Test
  @DisplayName("Should send message with only images when text is null")
  void shouldSendMessageWithOnlyImagesWhenTextIsNull() {
    // Given
    MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image".getBytes());
    List<MultipartFile> images = Arrays.asList(image);

    ChatMessageDao savedMessageDao = new ChatMessageDao();
    savedMessageDao.setId("msg999");
    savedMessageDao.setSwapRequestId("swap123");
    savedMessageDao.setSender(senderDao);
    savedMessageDao.setMessage(null);
    savedMessageDao.setImageIds(Arrays.asList("image-id-1"));
    savedMessageDao.setSentAt(Instant.now());
    savedMessageDao.setReadByReceiver(false);

    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(swapRequestDao));
    when(userService.getUser("64e8f5d1a2b3c4d5e6f78905")).thenReturn(senderEntity);
    doNothing().when(imageService).uploadImage(any(MultipartFile.class), anyString());
    when(imageService.getDownloadUrl(anyString())).thenReturn("https://example.com/image.jpg");
    when(chatMessageRepository.save(any(ChatMessageDao.class))).thenReturn(savedMessageDao);

    // When
    ChatMessage result = chatService.sendMessage("swap123", "64e8f5d1a2b3c4d5e6f78905", null, images);

    // Then
    assertNotNull(result);
    assertEquals("msg999", result.getId());
    assertNull(result.getMessage());
    assertNotNull(result.getImageIds());
    assertEquals(1, result.getImageIds().size());
    verify(imageService).uploadImage(any(MultipartFile.class), anyString());
    verify(chatMessageRepository).save(any(ChatMessageDao.class));
  }

  @Test
  @DisplayName("Should throw exception when sending message with no text and no images")
  void shouldThrowExceptionWhenSendingMessageWithNoTextAndNoImages() {
    // Given - validation happens before swap request check, so no need to mock
    // repository

    // When & Then
    assertThrows(IllegalArgumentException.class,
        () -> chatService.sendMessage("swap123", "64e8f5d1a2b3c4d5e6f78905", null, null));
    assertThrows(IllegalArgumentException.class,
        () -> chatService.sendMessage("swap123", "64e8f5d1a2b3c4d5e6f78905", "", new ArrayList<>()));
    assertThrows(IllegalArgumentException.class,
        () -> chatService.sendMessage("swap123", "64e8f5d1a2b3c4d5e6f78905", "   ", new ArrayList<>()));

    // Validation happens first, so repository should not be called
    verifyNoInteractions(swapRequestRepository);
    verifyNoInteractions(userService);
    verifyNoInteractions(imageService);
    verifyNoInteractions(chatMessageRepository);
  }

  @Test
  @DisplayName("Should throw SwapRequestNotFoundException when sending message with images to nonexistent swap")
  void shouldThrowSwapRequestNotFoundExceptionWhenSendingMessageWithImagesToNonexistentSwap() {
    // Given
    MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image".getBytes());
    List<MultipartFile> images = Arrays.asList(image);
    when(swapRequestRepository.findById("nonexistent")).thenReturn(Optional.empty());

    // When & Then
    assertThrows(SwapRequestNotFoundException.class,
        () -> chatService.sendMessage("nonexistent", "64e8f5d1a2b3c4d5e6f78905", "Hello", images));
    verify(swapRequestRepository).findById("nonexistent");
    verifyNoInteractions(userService);
    verifyNoInteractions(imageService);
    verifyNoInteractions(chatMessageRepository);
  }

  @Test
  @DisplayName("Should throw ChatAccessDeniedException when unauthorized user sends message with images")
  void shouldThrowChatAccessDeniedExceptionWhenUnauthorizedUserSendsMessageWithImages() {
    // Given
    MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image".getBytes());
    List<MultipartFile> images = Arrays.asList(image);
    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(swapRequestDao));

    // When & Then
    assertThrows(ChatAccessDeniedException.class,
        () -> chatService.sendMessage("swap123", "unauthorized123", "Hello", images));
    verify(swapRequestRepository).findById("swap123");
    verifyNoInteractions(userService);
    verifyNoInteractions(imageService);
    verifyNoInteractions(chatMessageRepository);
  }

  @Test
  @DisplayName("Should handle empty images list when sending message")
  void shouldHandleEmptyImagesListWhenSendingMessage() {
    // Given
    List<MultipartFile> emptyImages = new ArrayList<>();
    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(swapRequestDao));
    when(userService.getUser("64e8f5d1a2b3c4d5e6f78905")).thenReturn(senderEntity);
    when(chatMessageRepository.save(any(ChatMessageDao.class))).thenReturn(chatMessageDao);

    // When
    ChatMessage result = chatService.sendMessage("swap123", "64e8f5d1a2b3c4d5e6f78905", "Hello", emptyImages);

    // Then
    assertNotNull(result);
    verify(imageService, never()).uploadImage(any(), anyString());
    verify(chatMessageRepository).save(any(ChatMessageDao.class));
  }

  @Test
  @DisplayName("Should get chat messages with image URLs")
  void shouldGetChatMessagesWithImageUrls() {
    // Given
    ChatMessageDao messageWithImages = new ChatMessageDao();
    messageWithImages.setId("msg456");
    messageWithImages.setSwapRequestId("swap123");
    messageWithImages.setSender(senderDao);
    messageWithImages.setMessage("Check these out");
    messageWithImages.setImageIds(Arrays.asList("image-id-1", "image-id-2"));
    messageWithImages.setSentAt(Instant.now());
    messageWithImages.setReadByReceiver(false);

    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(swapRequestDao));
    when(chatMessageRepository.findBySwapRequestIdOrderBySentAtAsc("swap123"))
        .thenReturn(Arrays.asList(messageWithImages));
    when(imageService.getDownloadUrl("image-id-1")).thenReturn("https://example.com/image1.jpg");
    when(imageService.getDownloadUrl("image-id-2")).thenReturn("https://example.com/image2.jpg");

    // When
    List<ChatMessage> result = chatService.getChatMessages("swap123", "64e8f5d1a2b3c4d5e6f78905");

    // Then
    assertEquals(1, result.size());
    ChatMessage message = result.get(0);
    assertNotNull(message.getImageIds());
    assertEquals(2, message.getImageIds().size());
    assertEquals("https://example.com/image1.jpg", message.getImageIds().get(0));
    assertEquals("https://example.com/image2.jpg", message.getImageIds().get(1));
    verify(imageService, times(2)).getDownloadUrl(anyString());
  }

  @Test
  @DisplayName("Should get chat messages with null image URLs when no images")
  void shouldGetChatMessagesWithNullImageUrlsWhenNoImages() {
    // Given
    ChatMessageDao messageWithoutImages = new ChatMessageDao();
    messageWithoutImages.setId("msg789");
    messageWithoutImages.setSwapRequestId("swap123");
    messageWithoutImages.setSender(senderDao);
    messageWithoutImages.setMessage("Text only message");
    messageWithoutImages.setImageIds(null);
    messageWithoutImages.setSentAt(Instant.now());
    messageWithoutImages.setReadByReceiver(false);

    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(swapRequestDao));
    when(chatMessageRepository.findBySwapRequestIdOrderBySentAtAsc("swap123"))
        .thenReturn(Arrays.asList(messageWithoutImages));

    // When
    List<ChatMessage> result = chatService.getChatMessages("swap123", "64e8f5d1a2b3c4d5e6f78905");

    // Then
    assertEquals(1, result.size());
    ChatMessage message = result.get(0);
    assertNull(message.getImageIds());
    verify(imageService, never()).getDownloadUrl(anyString());
  }

  @Test
  @DisplayName("Should not mark own messages as read")
  void shouldNotMarkOwnMessagesAsRead() {
    // Given
    ChatMessageDao ownMessage = new ChatMessageDao();
    ownMessage.setId("msg111");
    ownMessage.setSender(senderDao); // Message from sender
    ownMessage.setReadByReceiver(false);

    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(swapRequestDao));
    when(chatMessageRepository.findBySwapRequestIdOrderBySentAtAsc("swap123"))
        .thenReturn(Arrays.asList(ownMessage));

    // When - sender marks messages as read (should not mark their own message)
    chatService.markMessagesAsRead("swap123", "64e8f5d1a2b3c4d5e6f78905");

    // Then
    verify(swapRequestRepository).findById("swap123");
    verify(chatMessageRepository).findBySwapRequestIdOrderBySentAtAsc("swap123");
    verify(chatMessageRepository, never()).save(any(ChatMessageDao.class)); // Should not save since it's own message
  }

  @Test
  @DisplayName("Should not mark already read messages")
  void shouldNotMarkAlreadyReadMessages() {
    // Given
    ChatMessageDao alreadyReadMessage = new ChatMessageDao();
    alreadyReadMessage.setId("msg222");
    alreadyReadMessage.setSender(senderDao); // Message from sender
    alreadyReadMessage.setReadByReceiver(true); // Already read

    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(swapRequestDao));
    when(chatMessageRepository.findBySwapRequestIdOrderBySentAtAsc("swap123"))
        .thenReturn(Arrays.asList(alreadyReadMessage));

    // When - receiver marks messages as read
    chatService.markMessagesAsRead("swap123", "64e8f5d1a2b3c4d5e6f78901");

    // Then
    verify(chatMessageRepository, never()).save(any(ChatMessageDao.class)); // Should not save already read messages
  }

  @Test
  @DisplayName("Should throw SwapRequestNotFoundException when marking messages as read for nonexistent swap")
  void shouldThrowSwapRequestNotFoundExceptionWhenMarkingMessagesAsReadForNonexistentSwap() {
    // Given
    when(swapRequestRepository.findById("nonexistent")).thenReturn(Optional.empty());

    // When & Then
    assertThrows(SwapRequestNotFoundException.class,
        () -> chatService.markMessagesAsRead("nonexistent", "64e8f5d1a2b3c4d5e6f78905"));
    verify(swapRequestRepository).findById("nonexistent");
    verifyNoInteractions(chatMessageRepository);
  }

  @Test
  @DisplayName("Should throw ChatAccessDeniedException when unauthorized user marks messages as read")
  void shouldThrowChatAccessDeniedExceptionWhenUnauthorizedUserMarksMessagesAsRead() {
    // Given
    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(swapRequestDao));

    // When & Then
    assertThrows(ChatAccessDeniedException.class,
        () -> chatService.markMessagesAsRead("swap123", "unauthorized123"));
    verify(swapRequestRepository).findById("swap123");
    verifyNoInteractions(chatMessageRepository);
  }

  @Test
  @DisplayName("Should return zero unread count when no unread messages")
  void shouldReturnZeroUnreadCountWhenNoUnreadMessages() {
    // Given
    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(swapRequestDao));
    when(chatMessageRepository.countBySwapRequestIdAndReadByReceiverFalseAndSenderIdNot("swap123",
        "64e8f5d1a2b3c4d5e6f78901"))
            .thenReturn(0L);

    // When
    long count = chatService.getUnreadMessageCount("swap123", "64e8f5d1a2b3c4d5e6f78901");

    // Then
    assertEquals(0L, count);
    verify(chatMessageRepository).countBySwapRequestIdAndReadByReceiverFalseAndSenderIdNot("swap123",
        "64e8f5d1a2b3c4d5e6f78901");
  }

  @Test
  @DisplayName("Should throw SwapRequestNotFoundException when getting unread count for nonexistent swap")
  void shouldThrowSwapRequestNotFoundExceptionWhenGettingUnreadCountForNonexistentSwap() {
    // Given
    when(swapRequestRepository.findById("nonexistent")).thenReturn(Optional.empty());

    // When & Then
    assertThrows(SwapRequestNotFoundException.class,
        () -> chatService.getUnreadMessageCount("nonexistent", "64e8f5d1a2b3c4d5e6f78905"));
    verify(swapRequestRepository).findById("nonexistent");
    verifyNoInteractions(chatMessageRepository);
  }

  @Test
  @DisplayName("Should get swap request for chat when user has access")
  void shouldGetSwapRequestForChatWhenUserHasAccess() {
    // Given
    // Add book to swap request to avoid NullPointerException in mapper
    BookDao bookDao = new BookDao();
    bookDao.setId("book123");
    bookDao.setTitle("Test Book");
    bookDao.setAuthor("Test Author");
    bookDao.setLanguage("English");
    bookDao.setCondition("Good");
    bookDao.setOwner(senderDao);
    swapRequestDao.setBookToSwapWith(bookDao);

    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(swapRequestDao));

    // When
    SwapRequest result = chatService.getSwapRequestForChat("swap123", "64e8f5d1a2b3c4d5e6f78905");

    // Then
    assertNotNull(result);
    assertEquals("swap123", result.getId());
    verify(swapRequestRepository).findById("swap123");
  }

  @Test
  @DisplayName("Should throw SwapRequestNotFoundException when getting swap request for nonexistent chat")
  void shouldThrowSwapRequestNotFoundExceptionWhenGettingSwapRequestForNonexistentChat() {
    // Given
    when(swapRequestRepository.findById("nonexistent")).thenReturn(Optional.empty());

    // When & Then
    assertThrows(SwapRequestNotFoundException.class,
        () -> chatService.getSwapRequestForChat("nonexistent", "64e8f5d1a2b3c4d5e6f78905"));
    verify(swapRequestRepository).findById("nonexistent");
  }

  @Test
  @DisplayName("Should throw ChatAccessDeniedException when unauthorized user gets swap request for chat")
  void shouldThrowChatAccessDeniedExceptionWhenUnauthorizedUserGetsSwapRequestForChat() {
    // Given
    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(swapRequestDao));

    // When & Then
    assertThrows(ChatAccessDeniedException.class,
        () -> chatService.getSwapRequestForChat("swap123", "unauthorized123"));
    verify(swapRequestRepository).findById("swap123");
  }

  @Test
  @DisplayName("Should handle null swap request ID gracefully")
  void shouldHandleNullSwapRequestIdGracefully() {
    // Given
    when(swapRequestRepository.findById(null)).thenReturn(Optional.empty());

    // When & Then
    assertThrows(SwapRequestNotFoundException.class,
        () -> chatService.getChatMessages(null, "64e8f5d1a2b3c4d5e6f78905"));
    verify(swapRequestRepository).findById(null);
  }

  @Test
  @DisplayName("Should handle empty chat messages list")
  void shouldHandleEmptyChatMessagesList() {
    // Given
    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(swapRequestDao));
    when(chatMessageRepository.findBySwapRequestIdOrderBySentAtAsc("swap123"))
        .thenReturn(Collections.emptyList());

    // When
    List<ChatMessage> result = chatService.getChatMessages("swap123", "64e8f5d1a2b3c4d5e6f78905");

    // Then
    assertNotNull(result);
    assertEquals(0, result.size());
    verify(chatMessageRepository, times(2)).findBySwapRequestIdOrderBySentAtAsc("swap123");
  }

  @Test
  @DisplayName("Should trim whitespace from message text")
  void shouldTrimWhitespaceFromMessageText() {
    // Given
    when(swapRequestRepository.findById("swap123")).thenReturn(Optional.of(swapRequestDao));
    when(userService.getUser("64e8f5d1a2b3c4d5e6f78905")).thenReturn(senderEntity);
    when(chatMessageRepository.save(any(ChatMessageDao.class))).thenAnswer(invocation -> {
      ChatMessageDao dao = invocation.getArgument(0);
      assertEquals("Hello", dao.getMessage()); // Should be trimmed
      return dao;
    });

    // When
    chatService.sendMessage("swap123", "64e8f5d1a2b3c4d5e6f78905", "  Hello  ");

    // Then
    verify(chatMessageRepository).save(any(ChatMessageDao.class));
  }
}
