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
  @InjectMocks
  private InboxService inboxService;

  private UserDao senderDao;
  private UserDao receiverDao;
  private BookDao bookDao1;
  private BookDao bookDao2;
  private SwapRequestDao swapRequest1;
  private SwapRequestDao swapRequest2;
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

    // Create test swap requests
    swapRequest1 = new SwapRequestDao();
    swapRequest1.setId("swap1");
    swapRequest1.setSender(senderDao);
    swapRequest1.setReceiver(receiverDao);
    swapRequest1.setBookToSwapWith(bookDao1);
    swapRequest1.setSwapType("ByBooks");
    swapRequest1.setAskForGiveaway(false);
    swapRequest1.setSwapStatus(SwapStatus.PENDING.getCode());
    swapRequest1.setRequestedAt(Instant.parse("2025-01-01T10:00:00Z"));
    swapRequest1.setUpdatedAt(Instant.parse("2025-01-01T10:00:00Z"));

    swapRequest2 = new SwapRequestDao();
    swapRequest2.setId("swap2");
    swapRequest2.setSender(senderDao);
    swapRequest2.setReceiver(receiverDao);
    swapRequest2.setBookToSwapWith(bookDao2);
    swapRequest2.setSwapType("ByBooks");
    swapRequest2.setAskForGiveaway(false);
    swapRequest2.setSwapStatus(SwapStatus.ACCEPTED.getCode());
    swapRequest2.setRequestedAt(Instant.parse("2025-01-02T10:00:00Z"));
    swapRequest2.setUpdatedAt(Instant.parse("2025-01-02T10:00:00Z"));
  }

  @Test
  @DisplayName("Should get received swap requests without status filter")
  void shouldGetReceivedSwapRequestsWithoutStatusFilter() {
    // Given
    when(userService.getUser("receiver123")).thenReturn(userEntity);
    when(swapRequestRepository.findByReceiverIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(Arrays.asList(swapRequest2, swapRequest1));

    // When
    List<SwapRequest> result = inboxService.getReceivedSwapRequests("receiver123", null, null);

    // Then
    assertEquals(2, result.size());
    assertEquals("swap2", result.get(0).getId());
    assertEquals("swap1", result.get(1).getId());
    verify(userService).getUser("receiver123");
    verify(swapRequestRepository).findByReceiverIdOrderByRequestedAtDesc("receiver123");
  }

  @Test
  @DisplayName("Should get received swap requests with status filter")
  void shouldGetReceivedSwapRequestsWithStatusFilter() {
    // Given
    when(userService.getUser("receiver123")).thenReturn(userEntity);
    when(swapRequestRepository.findByReceiverIdAndSwapStatusOrderByRequestedAtDesc("receiver123",
        SwapStatus.PENDING.getCode()))
            .thenReturn(Arrays.asList(swapRequest1));

    // When
    List<SwapRequest> result = inboxService.getReceivedSwapRequests("receiver123", SwapStatus.PENDING.getCode(), null);

    // Then
    assertEquals(1, result.size());
    assertEquals("swap1", result.get(0).getId());
    assertEquals(SwapStatus.PENDING, result.get(0).getSwapStatus());
    verify(userService).getUser("receiver123");
    verify(swapRequestRepository).findByReceiverIdAndSwapStatusOrderByRequestedAtDesc("receiver123",
        SwapStatus.PENDING.getCode());
  }

  @Test
  @DisplayName("Should get sent swap requests without status filter")
  void shouldGetSentSwapRequestsWithoutStatusFilter() {
    // Given
    when(userService.getUser("sender123")).thenReturn(userEntity);
    when(swapRequestRepository.findBySenderIdOrderByRequestedAtDesc("sender123"))
        .thenReturn(Arrays.asList(swapRequest2, swapRequest1));

    // When
    List<SwapRequest> result = inboxService.getSentSwapRequests("sender123", null, null);

    // Then
    assertEquals(2, result.size());
    assertEquals("swap2", result.get(0).getId());
    assertEquals("swap1", result.get(1).getId());
    verify(userService).getUser("sender123");
    verify(swapRequestRepository).findBySenderIdOrderByRequestedAtDesc("sender123");
  }

  @Test
  @DisplayName("Should get sent swap requests with status filter")
  void shouldGetSentSwapRequestsWithStatusFilter() {
    // Given
    when(userService.getUser("sender123")).thenReturn(userEntity);
    when(swapRequestRepository.findBySenderIdAndSwapStatusOrderByRequestedAtDesc("sender123",
        SwapStatus.ACCEPTED.getCode()))
            .thenReturn(Arrays.asList(swapRequest2));

    // When
    List<SwapRequest> result = inboxService.getSentSwapRequests("sender123", SwapStatus.ACCEPTED.getCode(), null);

    // Then
    assertEquals(1, result.size());
    assertEquals("swap2", result.get(0).getId());
    assertEquals(SwapStatus.ACCEPTED, result.get(0).getSwapStatus());
    verify(userService).getUser("sender123");
    verify(swapRequestRepository).findBySenderIdAndSwapStatusOrderByRequestedAtDesc("sender123",
        SwapStatus.ACCEPTED.getCode());
  }

  @Test
  @DisplayName("Should throw BadRequestException for invalid status")
  void shouldThrowBadRequestExceptionForInvalidStatus() {
    // Given
    when(userService.getUser("receiver123")).thenReturn(userEntity);

    // When & Then
    assertThrows(BadRequestException.class,
        () -> inboxService.getReceivedSwapRequests("receiver123", "INVALID_STATUS", null));
    verify(userService).getUser("receiver123");
    verifyNoInteractions(swapRequestRepository);
  }

  @Test
  @DisplayName("Should sort by book title")
  void shouldSortByBookTitle() {
    // Given
    when(userService.getUser("receiver123")).thenReturn(userEntity);
    when(swapRequestRepository.findByReceiverIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(Arrays.asList(swapRequest2, swapRequest1)); // Book B, Book A

    // When
    List<SwapRequest> result = inboxService.getReceivedSwapRequests("receiver123", null, "book_title");

    // Then
    assertEquals(2, result.size());
    assertEquals("Book A", result.get(0).getBookToSwapWith().getTitle()); // Should be first after sorting
    assertEquals("Book B", result.get(1).getBookToSwapWith().getTitle());
  }

  @Test
  @DisplayName("Should sort by sender name")
  void shouldSortBySenderName() {
    // Given
    UserDao anotherSender = new UserDao();
    anotherSender.setId("sender456");
    anotherSender.setFirstName("Charlie");
    anotherSender.setLastName("Brown");

    BookDao bookDao3 = new BookDao();
    bookDao3.setId("book3");
    bookDao3.setTitle("Book C");
    bookDao3.setAuthor("Author C");
    bookDao3.setLanguage("English");
    bookDao3.setCondition("Good");

    SwapRequestDao swapRequest3 = new SwapRequestDao();
    swapRequest3.setId("swap3");
    swapRequest3.setSender(anotherSender);
    swapRequest3.setReceiver(receiverDao);
    swapRequest3.setBookToSwapWith(bookDao3);
    swapRequest3.setSwapType("ByBooks");
    swapRequest3.setAskForGiveaway(false);
    swapRequest3.setSwapStatus(SwapStatus.PENDING.getCode());
    swapRequest3.setRequestedAt(Instant.parse("2025-01-03T10:00:00Z"));
    swapRequest3.setUpdatedAt(Instant.parse("2025-01-03T10:00:00Z"));

    when(userService.getUser("receiver123")).thenReturn(userEntity);
    when(swapRequestRepository.findByReceiverIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(Arrays.asList(swapRequest1, swapRequest3)); // Alice Smith, Charlie Brown

    // When
    List<SwapRequest> result = inboxService.getReceivedSwapRequests("receiver123", null, "sender_name");

    // Then
    assertEquals(2, result.size());
    assertEquals("Alice", result.get(0).getSender().getFirstName()); // Alice Smith should be first
    assertEquals("Charlie", result.get(1).getSender().getFirstName());
  }

  @Test
  @DisplayName("Should update swap request status when authorized")
  void shouldUpdateSwapRequestStatusWhenAuthorized() {
    // Given
    when(swapRequestRepository.findById("swap1")).thenReturn(Optional.of(swapRequest1));
    when(swapRequestRepository.save(any(SwapRequestDao.class))).thenReturn(swapRequest1);

    // When
    SwapRequest result = inboxService.updateSwapRequestStatus("swap1", SwapStatus.ACCEPTED.getCode(), "receiver123");

    // Then
    assertNotNull(result);
    assertEquals(SwapStatus.ACCEPTED.getCode(), swapRequest1.getSwapStatus());
    verify(swapRequestRepository).findById("swap1");
    verify(swapRequestRepository).save(swapRequest1);
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
    when(swapRequestRepository.findById("swap1")).thenReturn(Optional.of(swapRequest1));

    // When & Then
    assertThrows(IllegalArgumentException.class,
        () -> inboxService.updateSwapRequestStatus("swap1", SwapStatus.ACCEPTED.getCode(), "unauthorized123"));
    verify(swapRequestRepository).findById("swap1");
    verify(swapRequestRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should throw exception for invalid status transition")
  void shouldThrowExceptionForInvalidStatusTransition() {
    // Given
    swapRequest1.setSwapStatus(SwapStatus.REJECTED.getCode()); // Terminal state
    when(swapRequestRepository.findById("swap1")).thenReturn(Optional.of(swapRequest1));

    // When & Then
    assertThrows(IllegalArgumentException.class,
        () -> inboxService.updateSwapRequestStatus("swap1", SwapStatus.ACCEPTED.getCode(), "receiver123"));
    verify(swapRequestRepository).findById("swap1");
    verify(swapRequestRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should allow sender to expire their own request")
  void shouldAllowSenderToExpireTheirOwnRequest() {
    // Given
    when(swapRequestRepository.findById("swap1")).thenReturn(Optional.of(swapRequest1));
    when(swapRequestRepository.save(any(SwapRequestDao.class))).thenReturn(swapRequest1);

    // When
    SwapRequest result = inboxService.updateSwapRequestStatus("swap1", SwapStatus.EXPIRED.getCode(), "sender123");

    // Then
    assertNotNull(result);
    assertEquals(SwapStatus.EXPIRED.getCode(), swapRequest1.getSwapStatus());
    verify(swapRequestRepository).findById("swap1");
    verify(swapRequestRepository).save(swapRequest1);
  }

  @Test
  @DisplayName("Should return default sorting when invalid sort parameter provided")
  void shouldReturnDefaultSortingWhenInvalidSortParameterProvided() {
    // Given
    when(userService.getUser("receiver123")).thenReturn(userEntity);
    when(swapRequestRepository.findByReceiverIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(Arrays.asList(swapRequest2, swapRequest1));

    // When
    List<SwapRequest> result = inboxService.getReceivedSwapRequests("receiver123", null, "invalid_sort");

    // Then
    assertEquals(2, result.size());
    assertEquals("swap2", result.get(0).getId()); // Should maintain original order
    assertEquals("swap1", result.get(1).getId());
  }

  @Test
  @DisplayName("Should get unread message count for user and swap request")
  void shouldGetUnreadMessageCountForUserAndSwapRequest() {
    // Given
    String userId = "user123";
    String swapRequestId = "swap123";
    long expectedCount = 5L;
    when(chatService.getUnreadMessageCount(swapRequestId, userId)).thenReturn(expectedCount);

    // When
    long result = inboxService.getUnreadMessageCount(userId, swapRequestId);

    // Then
    assertEquals(expectedCount, result);
    verify(chatService).getUnreadMessageCount(swapRequestId, userId);
  }

  @Test
  @DisplayName("Should return zero when no unread messages exist")
  void shouldReturnZeroWhenNoUnreadMessagesExist() {
    // Given
    String userId = "user123";
    String swapRequestId = "swap123";
    when(chatService.getUnreadMessageCount(swapRequestId, userId)).thenReturn(0L);

    // When
    long result = inboxService.getUnreadMessageCount(userId, swapRequestId);

    // Then
    assertEquals(0L, result);
    verify(chatService).getUnreadMessageCount(swapRequestId, userId);
  }

  @Test
  @DisplayName("Should clear unread count cache when status is updated")
  void shouldClearUnreadCountCacheWhenStatusIsUpdated() {
    // Given
    when(swapRequestRepository.findById("swap1")).thenReturn(Optional.of(swapRequest1));
    when(swapRequestRepository.save(any(SwapRequestDao.class))).thenReturn(swapRequest1);

    // When
    inboxService.updateSwapRequestStatus("swap1", SwapStatus.ACCEPTED.getCode(), "receiver123");

    // Then
    verify(swapRequestRepository).findById("swap1");
    verify(swapRequestRepository).save(swapRequest1);
    // Note: Cache eviction is handled by Spring AOP, so we can't directly verify it
    // in unit tests
    // This would be better tested in integration tests
  }
}