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
import org.springframework.context.ApplicationEventPublisher;

import com.kirjaswappi.backend.events.InboxUpdateEvent;
import com.kirjaswappi.backend.jpa.daos.BookDao;
import com.kirjaswappi.backend.jpa.daos.SwapRequestDao;
import com.kirjaswappi.backend.jpa.daos.UserDao;
import com.kirjaswappi.backend.jpa.repositories.SwapRequestRepository;
import com.kirjaswappi.backend.service.entities.SwapRequest;
import com.kirjaswappi.backend.service.entities.User;
import com.kirjaswappi.backend.service.enums.SwapStatus;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;
import com.kirjaswappi.backend.service.exceptions.SwapRequestNotFoundException;

class InboxServiceTest {
  @Mock
  private SwapRequestRepository swapRequestRepository;
  @Mock
  private UserService userService;
  @Mock
  private ChatService chatService;
  @Mock
  private ApplicationEventPublisher eventPublisher;
  @InjectMocks
  private InboxService inboxService;

  private UserDao senderDao;
  private UserDao receiverDao;
  private BookDao bookDao1;
  private BookDao bookDao2;
  private SwapRequestDao receivedSwapRequest;
  private SwapRequestDao sentSwapRequest;
  private User userEntity;

  @BeforeEach
  @DisplayName("Setup mocks for InboxService tests")
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // Create test users
    senderDao = new UserDao();
    senderDao.setId("sender123");
    senderDao.setFirstName("Alice");
    senderDao.setLastName("Smith");

    receiverDao = new UserDao();
    receiverDao.setId("receiver123");
    receiverDao.setFirstName("Bob");
    receiverDao.setLastName("Johnson");

    userEntity = new User();
    userEntity.setId("receiver123");
    userEntity.setFirstName("Bob");
    userEntity.setLastName("Johnson");

    // Create test books
    bookDao1 = new BookDao();
    bookDao1.setId("book1");
    bookDao1.setTitle("Book A");
    bookDao1.setAuthor("Author A");
    bookDao1.setLanguage("English");
    bookDao1.setCondition("Good");

    bookDao2 = new BookDao();
    bookDao2.setId("book2");
    bookDao2.setTitle("Book B");
    bookDao2.setAuthor("Author B");
    bookDao2.setLanguage("English");
    bookDao2.setCondition("Good");

    // Create received swap request
    receivedSwapRequest = new SwapRequestDao();
    receivedSwapRequest.setId("received1");
    receivedSwapRequest.setSender(senderDao);
    receivedSwapRequest.setReceiver(receiverDao);
    receivedSwapRequest.setBookToSwapWith(bookDao1);
    receivedSwapRequest.setSwapType("ByBooks");
    receivedSwapRequest.setAskForGiveaway(false);
    receivedSwapRequest.setSwapStatus(SwapStatus.PENDING.getCode());
    receivedSwapRequest.setRequestedAt(Instant.parse("2025-01-01T10:00:00Z"));
    receivedSwapRequest.setUpdatedAt(Instant.parse("2025-01-01T10:00:00Z"));

    // Create sent swap request
    sentSwapRequest = new SwapRequestDao();
    sentSwapRequest.setId("sent1");
    sentSwapRequest.setSender(receiverDao);
    sentSwapRequest.setReceiver(senderDao);
    sentSwapRequest.setBookToSwapWith(bookDao2);
    sentSwapRequest.setSwapType("GiveAway");
    sentSwapRequest.setAskForGiveaway(true);
    sentSwapRequest.setSwapStatus(SwapStatus.ACCEPTED.getCode());
    sentSwapRequest.setRequestedAt(Instant.parse("2025-01-02T10:00:00Z"));
    sentSwapRequest.setUpdatedAt(Instant.parse("2025-01-02T10:00:00Z"));
  }

  @Test
  @DisplayName("Should get unified inbox without status filter")
  void shouldGetUnifiedInboxWithoutStatusFilter() {
    // Given
    when(userService.getUser("receiver123")).thenReturn(userEntity);
    when(swapRequestRepository.findByReceiverIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(Arrays.asList(receivedSwapRequest));
    when(swapRequestRepository.findBySenderIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(Arrays.asList(sentSwapRequest));
    when(chatService.getLatestMessageTimestamp("received1"))
        .thenReturn(Optional.of(Instant.parse("2025-01-03T10:00:00Z")));
    when(chatService.getLatestMessageTimestamp("sent1")).thenReturn(Optional.of(Instant.parse("2025-01-04T10:00:00Z")));

    // When
    List<SwapRequest> result = inboxService.getUnifiedInbox("receiver123", null, null);

    // Then
    assertEquals(2, result.size());
    // Should be sorted by latest message timestamp (sent1 has newer message)
    assertEquals("sent1", result.get(0).getId());
    assertEquals("received1", result.get(1).getId());
    verify(userService).getUser("receiver123");
    verify(swapRequestRepository).findByReceiverIdOrderByRequestedAtDesc("receiver123");
    verify(swapRequestRepository).findBySenderIdOrderByRequestedAtDesc("receiver123");
    verify(chatService).getLatestMessageTimestamp("received1");
    verify(chatService).getLatestMessageTimestamp("sent1");
  }

  @Test
  @DisplayName("Should get unified inbox with status filter")
  void shouldGetUnifiedInboxWithStatusFilter() {
    // Given
    when(userService.getUser("receiver123")).thenReturn(userEntity);
    when(swapRequestRepository.findByReceiverIdAndSwapStatusOrderByRequestedAtDesc("receiver123",
        SwapStatus.PENDING.getCode()))
            .thenReturn(Arrays.asList(receivedSwapRequest));
    when(swapRequestRepository.findBySenderIdAndSwapStatusOrderByRequestedAtDesc("receiver123",
        SwapStatus.PENDING.getCode()))
            .thenReturn(Arrays.asList());

    // When
    List<SwapRequest> result = inboxService.getUnifiedInbox("receiver123", SwapStatus.PENDING.getCode(), null);

    // Then
    assertEquals(1, result.size());
    assertEquals("received1", result.get(0).getId());
    assertEquals(SwapStatus.PENDING, result.get(0).getSwapStatus());
    verify(userService).getUser("receiver123");
    verify(swapRequestRepository).findByReceiverIdAndSwapStatusOrderByRequestedAtDesc("receiver123",
        SwapStatus.PENDING.getCode());
    verify(swapRequestRepository).findBySenderIdAndSwapStatusOrderByRequestedAtDesc("receiver123",
        SwapStatus.PENDING.getCode());
  }

  @Test
  @DisplayName("Should throw BadRequestException for invalid status in unified inbox")
  void shouldThrowBadRequestExceptionForInvalidStatusInUnifiedInbox() {
    // Given
    when(userService.getUser("receiver123")).thenReturn(userEntity);

    // When & Then
    assertThrows(BadRequestException.class,
        () -> inboxService.getUnifiedInbox("receiver123", "INVALID_STATUS", null));
    verify(userService).getUser("receiver123");
    verifyNoInteractions(swapRequestRepository);
  }

  @Test
  @DisplayName("Should sort unified inbox by book title")
  void shouldSortUnifiedInboxByBookTitle() {
    // Given
    when(userService.getUser("receiver123")).thenReturn(userEntity);
    when(swapRequestRepository.findByReceiverIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(Arrays.asList(receivedSwapRequest)); // Book A
    when(swapRequestRepository.findBySenderIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(Arrays.asList(sentSwapRequest)); // Book B

    // When
    List<SwapRequest> result = inboxService.getUnifiedInbox("receiver123", null, "book_title");

    // Then
    assertEquals(2, result.size());
    assertEquals("Book A", result.get(0).getBookToSwapWith().getTitle()); // Should be first after sorting
    assertEquals("Book B", result.get(1).getBookToSwapWith().getTitle());
  }

  @Test
  @DisplayName("Should sort unified inbox by latest message timestamp")
  void shouldSortUnifiedInboxByLatestMessageTimestamp() {
    // Given
    when(userService.getUser("receiver123")).thenReturn(userEntity);
    when(swapRequestRepository.findByReceiverIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(Arrays.asList(receivedSwapRequest));
    when(swapRequestRepository.findBySenderIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(Arrays.asList(sentSwapRequest));
    // received1 has older message, sent1 has newer message
    when(chatService.getLatestMessageTimestamp("received1"))
        .thenReturn(Optional.of(Instant.parse("2025-01-03T10:00:00Z")));
    when(chatService.getLatestMessageTimestamp("sent1")).thenReturn(Optional.of(Instant.parse("2025-01-04T10:00:00Z")));

    // When
    List<SwapRequest> result = inboxService.getUnifiedInbox("receiver123", null, "latest_message");

    // Then
    assertEquals(2, result.size());
    assertEquals("sent1", result.get(0).getId()); // Should be first (newer message)
    assertEquals("received1", result.get(1).getId());
    verify(chatService).getLatestMessageTimestamp("received1");
    verify(chatService).getLatestMessageTimestamp("sent1");
  }

  @Test
  @DisplayName("Should prioritize conversations with messages over those without")
  void shouldPrioritizeConversationsWithMessagesOverThoseWithout() {
    // Given
    when(userService.getUser("receiver123")).thenReturn(userEntity);
    when(swapRequestRepository.findByReceiverIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(Arrays.asList(receivedSwapRequest));
    when(swapRequestRepository.findBySenderIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(Arrays.asList(sentSwapRequest));
    // Only received1 has messages, sent1 has no messages
    when(chatService.getLatestMessageTimestamp("received1"))
        .thenReturn(Optional.of(Instant.parse("2025-01-03T10:00:00Z")));
    when(chatService.getLatestMessageTimestamp("sent1")).thenReturn(Optional.empty());

    // When
    List<SwapRequest> result = inboxService.getUnifiedInbox("receiver123", null, null);

    // Then
    assertEquals(2, result.size());
    assertEquals("received1", result.get(0).getId()); // Should be first (has messages)
    assertEquals("sent1", result.get(1).getId());
    verify(chatService).getLatestMessageTimestamp("received1");
    verify(chatService).getLatestMessageTimestamp("sent1");
  }

  @Test
  @DisplayName("Should update swap request status when authorized")
  void shouldUpdateSwapRequestStatusWhenAuthorized() {
    // Given
    when(swapRequestRepository.findById("received1")).thenReturn(Optional.of(receivedSwapRequest));
    when(swapRequestRepository.save(any(SwapRequestDao.class))).thenReturn(receivedSwapRequest);

    // When
    SwapRequest result = inboxService.updateSwapRequestStatus("received1", SwapStatus.ACCEPTED.getCode(),
        "receiver123");

    // Then
    assertNotNull(result);
    assertEquals(SwapStatus.ACCEPTED.getCode(), receivedSwapRequest.getSwapStatus());
    verify(swapRequestRepository).findById("received1");
    verify(swapRequestRepository).save(receivedSwapRequest);
    // Verify events are published for both sender and receiver
    verify(eventPublisher, times(2)).publishEvent(any(InboxUpdateEvent.class));
  }

  @Test
  @DisplayName("Should throw SwapRequestNotFoundException when swap request does not exist")
  void shouldThrowSwapRequestNotFoundExceptionWhenSwapRequestDoesNotExist() {
    // Given
    when(swapRequestRepository.findById("nonexistent")).thenReturn(Optional.empty());

    // When & Then
    assertThrows(SwapRequestNotFoundException.class,
        () -> inboxService.updateSwapRequestStatus("nonexistent", SwapStatus.ACCEPTED.getCode(), "receiver123"));
    verify(swapRequestRepository).findById("nonexistent");
    verify(swapRequestRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should throw exception when user not authorized to update status")
  void shouldThrowExceptionWhenUserNotAuthorizedToUpdateStatus() {
    // Given
    when(swapRequestRepository.findById("received1")).thenReturn(Optional.of(receivedSwapRequest));

    // When & Then
    assertThrows(IllegalArgumentException.class,
        () -> inboxService.updateSwapRequestStatus("received1", SwapStatus.ACCEPTED.getCode(), "unauthorized123"));
    verify(swapRequestRepository).findById("received1");
    verify(swapRequestRepository, never()).save(any());
  }
}
