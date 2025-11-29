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
    senderDao = UserDao.builder()
        .id("sender123")
        .firstName("Alice")
        .lastName("Smith")
        .build();

    receiverDao = UserDao.builder()
        .id("receiver123")
        .firstName("Bob")
        .lastName("Johnson")
        .build();

    userEntity = User.builder()
        .id("receiver123")
        .firstName("Bob")
        .lastName("Johnson")
        .build();

    // Create test books
    bookDao1 = BookDao.builder()
        .id("book1")
        .title("Book A")
        .author("Author A")
        .language("English")
        .condition("Good")
        .build();

    bookDao2 = BookDao.builder()
        .id("book2")
        .title("Book B")
        .author("Author B")
        .language("English")
        .condition("Good")
        .build();

    // Create received swap request
    receivedSwapRequest = SwapRequestDao.builder()
        .id("received1")
        .sender(senderDao)
        .receiver(receiverDao)
        .bookToSwapWith(bookDao1)
        .swapType("ByBooks")
        .askForGiveaway(false)
        .swapStatus(SwapStatus.PENDING.getCode())
        .requestedAt(Instant.parse("2025-01-01T10:00:00Z"))
        .updatedAt(Instant.parse("2025-01-01T10:00:00Z"))
        .build();

    // Create sent swap request
    sentSwapRequest = SwapRequestDao.builder()
        .id("sent1")
        .sender(receiverDao)
        .receiver(senderDao)
        .bookToSwapWith(bookDao2)
        .swapType("GiveAway")
        .askForGiveaway(true)
        .swapStatus(SwapStatus.ACCEPTED.getCode())
        .requestedAt(Instant.parse("2025-01-02T10:00:00Z"))
        .updatedAt(Instant.parse("2025-01-02T10:00:00Z"))
        .build();
  }

  @Test
  @DisplayName("Should get unified inbox without status filter")
  void shouldGetUnifiedInboxWithoutStatusFilter() {
    // Given
    when(userService.getUser("receiver123")).thenReturn(userEntity);
    when(swapRequestRepository.findByReceiverIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(List.of(receivedSwapRequest));
    when(swapRequestRepository.findBySenderIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(List.of(sentSwapRequest));
    when(chatService.getLatestMessageTimestamp("received1"))
        .thenReturn(Optional.of(Instant.parse("2025-01-03T10:00:00Z")));
    when(chatService.getLatestMessageTimestamp("sent1")).thenReturn(Optional.of(Instant.parse("2025-01-04T10:00:00Z")));

    // When
    List<SwapRequest> result = inboxService.getUnifiedInbox("receiver123", null, null);

    // Then
    assertEquals(2, result.size());
    // Should be sorted by latest message timestamp (sent1 has newer message)
    assertEquals("sent1", result.get(0).id());
    assertEquals("received1", result.get(1).id());
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
            .thenReturn(List.of(receivedSwapRequest));
    when(swapRequestRepository.findBySenderIdAndSwapStatusOrderByRequestedAtDesc("receiver123",
        SwapStatus.PENDING.getCode()))
            .thenReturn(List.of());

    // When
    List<SwapRequest> result = inboxService.getUnifiedInbox("receiver123", SwapStatus.PENDING.getCode(), null);

    // Then
    assertEquals(1, result.size());
    assertEquals("received1", result.getFirst().id());
    assertEquals(SwapStatus.PENDING, result.getFirst().swapStatus());
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
        .thenReturn(List.of(receivedSwapRequest)); // Book A
    when(swapRequestRepository.findBySenderIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(List.of(sentSwapRequest)); // Book B

    // When
    List<SwapRequest> result = inboxService.getUnifiedInbox("receiver123", null, "book_title");

    // Then
    assertEquals(2, result.size());
    assertEquals("Book A", result.get(0).bookToSwapWith().title()); // Should be first after sorting
    assertEquals("Book B", result.get(1).bookToSwapWith().title());
  }

  @Test
  @DisplayName("Should sort unified inbox by latest message timestamp")
  void shouldSortUnifiedInboxByLatestMessageTimestamp() {
    // Given
    when(userService.getUser("receiver123")).thenReturn(userEntity);
    when(swapRequestRepository.findByReceiverIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(List.of(receivedSwapRequest));
    when(swapRequestRepository.findBySenderIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(List.of(sentSwapRequest));
    // received1 has older message, sent1 has newer message
    when(chatService.getLatestMessageTimestamp("received1"))
        .thenReturn(Optional.of(Instant.parse("2025-01-03T10:00:00Z")));
    when(chatService.getLatestMessageTimestamp("sent1")).thenReturn(Optional.of(Instant.parse("2025-01-04T10:00:00Z")));

    // When
    List<SwapRequest> result = inboxService.getUnifiedInbox("receiver123", null, "latest_message");

    // Then
    assertEquals(2, result.size());
    assertEquals("sent1", result.getFirst().id()); // Should be first (newer message)
    assertEquals("received1", result.get(1).id());
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
    assertEquals("received1", result.getFirst().id()); // Should be first (has messages)
    assertEquals("sent1", result.get(1).id());
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
    assertEquals(SwapStatus.ACCEPTED.getCode(), receivedSwapRequest.swapStatus());
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

  @Test
  @DisplayName("Should throw exception for invalid status transition")
  void shouldThrowExceptionForInvalidStatusTransition() {
    // Given - trying to transition from REJECTED (terminal state) to ACCEPTED
    receivedSwapRequest.swapStatus(SwapStatus.REJECTED.getCode());
    when(swapRequestRepository.findById("received1")).thenReturn(Optional.of(receivedSwapRequest));

    // When & Then
    assertThrows(IllegalArgumentException.class,
        () -> inboxService.updateSwapRequestStatus("received1", SwapStatus.ACCEPTED.getCode(), "receiver123"));
    verify(swapRequestRepository).findById("received1");
    verify(swapRequestRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should allow sender to mark swap as expired")
  void shouldAllowSenderToMarkSwapAsExpired() {
    // Given
    when(swapRequestRepository.findById("received1")).thenReturn(Optional.of(receivedSwapRequest));
    when(swapRequestRepository.save(any(SwapRequestDao.class))).thenReturn(receivedSwapRequest);

    // When - sender marks their own request as expired
    SwapRequest result = inboxService.updateSwapRequestStatus("received1", SwapStatus.EXPIRED.getCode(), "sender123");

    // Then
    assertNotNull(result);
    assertEquals(SwapStatus.EXPIRED.getCode(), receivedSwapRequest.swapStatus());
    verify(swapRequestRepository).save(receivedSwapRequest);
    verify(eventPublisher, times(2)).publishEvent(any(InboxUpdateEvent.class));
  }

  @Test
  @DisplayName("Should allow receiver to mark swap as reserved")
  void shouldAllowReceiverToMarkSwapAsReserved() {
    // Given - swap must be in ACCEPTED state to transition to RESERVED
    sentSwapRequest.swapStatus(SwapStatus.ACCEPTED.getCode());
    when(swapRequestRepository.findById("sent1")).thenReturn(Optional.of(sentSwapRequest));
    when(swapRequestRepository.save(any(SwapRequestDao.class))).thenReturn(sentSwapRequest);

    // When - receiver marks as reserved
    SwapRequest result = inboxService.updateSwapRequestStatus("sent1", SwapStatus.RESERVED.getCode(), "sender123");

    // Then
    assertNotNull(result);
    assertEquals(SwapStatus.RESERVED.getCode(), sentSwapRequest.swapStatus());
    verify(swapRequestRepository).save(sentSwapRequest);
  }

  @Test
  @DisplayName("Should sort unified inbox by date")
  void shouldSortUnifiedInboxByDate() {
    // Given
    when(userService.getUser("receiver123")).thenReturn(userEntity);
    when(swapRequestRepository.findByReceiverIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(Arrays.asList(receivedSwapRequest));
    when(swapRequestRepository.findBySenderIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(Arrays.asList(sentSwapRequest));

    // When
    List<SwapRequest> result = inboxService.getUnifiedInbox("receiver123", null, "date");

    // Then
    assertEquals(2, result.size());
    // Should be sorted by requestedAt descending (sent1 is newer)
    assertEquals("sent1", result.getFirst().id());
    assertEquals("received1", result.get(1).id());
  }

  @Test
  @DisplayName("Should sort unified inbox by sender name")
  void shouldSortUnifiedInboxBySenderName() {
    // Given
    when(userService.getUser("receiver123")).thenReturn(userEntity);
    when(swapRequestRepository.findByReceiverIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(List.of(receivedSwapRequest)); // Sender: Alice Smith
    when(swapRequestRepository.findBySenderIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(List.of(sentSwapRequest)); // Sender: Bob Johnson

    // When
    List<SwapRequest> result = inboxService.getUnifiedInbox("receiver123", null, "sender_name");

    // Then
    assertEquals(2, result.size());
    // Should be sorted alphabetically by sender name (Alice < Bob)
    assertEquals("received1", result.getFirst().id());
    assertEquals("sent1", result.get(1).id());
  }

  @Test
  @DisplayName("Should sort unified inbox by status")
  void shouldSortUnifiedInboxByStatus() {
    // Given
    when(userService.getUser("receiver123")).thenReturn(userEntity);
    when(swapRequestRepository.findByReceiverIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(List.of(receivedSwapRequest)); // Status: PENDING
    when(swapRequestRepository.findBySenderIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(List.of(sentSwapRequest)); // Status: ACCEPTED

    // When
    List<SwapRequest> result = inboxService.getUnifiedInbox("receiver123", null, "status");

    // Then
    assertEquals(2, result.size());
    // Should be sorted alphabetically by status code (ACCEPTED < PENDING)
    assertEquals("sent1", result.getFirst().id());
    assertEquals("received1", result.get(1).id());
  }

  @Test
  @DisplayName("Should handle unknown sort option by defaulting to latest message")
  void shouldHandleUnknownSortOptionByDefaultingToLatestMessage() {
    // Given
    when(userService.getUser("receiver123")).thenReturn(userEntity);
    when(swapRequestRepository.findByReceiverIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(List.of(receivedSwapRequest));
    when(swapRequestRepository.findBySenderIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(List.of(sentSwapRequest));
    when(chatService.getLatestMessageTimestamp("received1"))
        .thenReturn(Optional.of(Instant.parse("2025-01-03T10:00:00Z")));
    when(chatService.getLatestMessageTimestamp("sent1")).thenReturn(Optional.of(Instant.parse("2025-01-04T10:00:00Z")));

    // When
    List<SwapRequest> result = inboxService.getUnifiedInbox("receiver123", null, "unknown_sort");

    // Then
    assertEquals(2, result.size());
    // Should default to latest message sorting
    assertEquals("sent1", result.getFirst().id());
    verify(chatService).getLatestMessageTimestamp("received1");
    verify(chatService).getLatestMessageTimestamp("sent1");
  }

  @Test
  @DisplayName("Should mark inbox item as read by receiver")
  void shouldMarkInboxItemAsReadByReceiver() {
    // Given
    receivedSwapRequest.readByReceiverAt(null);
    when(swapRequestRepository.findById("received1")).thenReturn(Optional.of(receivedSwapRequest));
    when(swapRequestRepository.save(any(SwapRequestDao.class))).thenReturn(receivedSwapRequest);

    // When
    inboxService.markInboxItemAsRead("received1", "receiver123");

    // Then
    assertNotNull(receivedSwapRequest.readByReceiverAt());
    verify(swapRequestRepository).findById("received1");
    verify(swapRequestRepository).save(receivedSwapRequest);
  }

  @Test
  @DisplayName("Should mark inbox item as read by sender")
  void shouldMarkInboxItemAsReadBySender() {
    // Given
    receivedSwapRequest.readBySenderAt(null);
    when(swapRequestRepository.findById("received1")).thenReturn(Optional.of(receivedSwapRequest));
    when(swapRequestRepository.save(any(SwapRequestDao.class))).thenReturn(receivedSwapRequest);

    // When
    inboxService.markInboxItemAsRead("received1", "sender123");

    // Then
    assertNotNull(receivedSwapRequest.readBySenderAt());
    verify(swapRequestRepository).findById("received1");
    verify(swapRequestRepository).save(receivedSwapRequest);
  }

  @Test
  @DisplayName("Should not update inbox item if already read by receiver")
  void shouldNotUpdateInboxItemIfAlreadyReadByReceiver() {
    // Given
    Instant readTime = Instant.parse("2025-01-01T12:00:00Z");
    receivedSwapRequest.readByReceiverAt(readTime);
    when(swapRequestRepository.findById("received1")).thenReturn(Optional.of(receivedSwapRequest));

    // When
    inboxService.markInboxItemAsRead("received1", "receiver123");

    // Then
    assertEquals(readTime, receivedSwapRequest.readByReceiverAt()); // Should remain unchanged
    verify(swapRequestRepository).findById("received1");
    verify(swapRequestRepository, never()).save(any()); // Should not save if already read
  }

  @Test
  @DisplayName("Should not update inbox item if already read by sender")
  void shouldNotUpdateInboxItemIfAlreadyReadBySender() {
    // Given
    Instant readTime = Instant.parse("2025-01-01T12:00:00Z");
    receivedSwapRequest.readBySenderAt(readTime);
    when(swapRequestRepository.findById("received1")).thenReturn(Optional.of(receivedSwapRequest));

    // When
    inboxService.markInboxItemAsRead("received1", "sender123");

    // Then
    assertEquals(readTime, receivedSwapRequest.readBySenderAt()); // Should remain unchanged
    verify(swapRequestRepository, never()).save(any()); // Should not save if already read
  }

  @Test
  @DisplayName("Should throw SwapRequestNotFoundException when marking nonexistent inbox item as read")
  void shouldThrowSwapRequestNotFoundExceptionWhenMarkingNonexistentInboxItemAsRead() {
    // Given
    when(swapRequestRepository.findById("nonexistent")).thenReturn(Optional.empty());

    // When & Then
    assertThrows(SwapRequestNotFoundException.class,
        () -> inboxService.markInboxItemAsRead("nonexistent", "receiver123"));
    verify(swapRequestRepository).findById("nonexistent");
    verify(swapRequestRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should not update inbox item when user is neither sender nor receiver")
  void shouldNotUpdateInboxItemWhenUserIsNeitherSenderNorReceiver() {
    // Given
    when(swapRequestRepository.findById("received1")).thenReturn(Optional.of(receivedSwapRequest));

    // When
    inboxService.markInboxItemAsRead("received1", "unauthorized123");

    // Then
    verify(swapRequestRepository).findById("received1");
    verify(swapRequestRepository, never()).save(any()); // Should not save for unauthorized user
  }

  @Test
  @DisplayName("Should return true when inbox item is unread by receiver")
  void shouldReturnTrueWhenInboxItemIsUnreadByReceiver() {
    // Given
    com.kirjaswappi.backend.service.entities.User receiver = new com.kirjaswappi.backend.service.entities.User()
        .id("receiver123");
    com.kirjaswappi.backend.service.entities.User sender = new com.kirjaswappi.backend.service.entities.User()
        .id("sender123");

    SwapRequest swapRequest = SwapRequest.builder()
        .id("swap1")
        .receiver(receiver)
        .sender(sender)
        .readByReceiverAt(null)
        .build();

    // When
    boolean result = inboxService.isInboxItemUnread(swapRequest, "receiver123");

    // Then
    assertTrue(result);
  }

  @Test
  @DisplayName("Should return false when inbox item is read by receiver")
  void shouldReturnFalseWhenInboxItemIsReadByReceiver() {
    // Given

    com.kirjaswappi.backend.service.entities.User receiver = new com.kirjaswappi.backend.service.entities.User()
        .id("receiver123");
    com.kirjaswappi.backend.service.entities.User sender = new com.kirjaswappi.backend.service.entities.User()
        .id("sender123");

    SwapRequest swapRequest = SwapRequest.builder()
        .id("swap1")
        .receiver(receiver)
        .sender(sender)
        .readByReceiverAt(Instant.now())
        .build();

    // When
    boolean result = inboxService.isInboxItemUnread(swapRequest, "receiver123");

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("Should return true when inbox item is unread by sender")
  void shouldReturnTrueWhenInboxItemIsUnreadBySender() {
    // Given
    com.kirjaswappi.backend.service.entities.User receiver = new com.kirjaswappi.backend.service.entities.User()
        .id("receiver123");
    com.kirjaswappi.backend.service.entities.User sender = new com.kirjaswappi.backend.service.entities.User()
        .id("sender123");

    SwapRequest swapRequest = SwapRequest.builder()
        .id("swap1")
        .receiver(receiver)
        .sender(sender)
        .readBySenderAt(null)
        .build();

    // When
    boolean result = inboxService.isInboxItemUnread(swapRequest, "sender123");

    // Then
    assertTrue(result);
  }

  @Test
  @DisplayName("Should return false when inbox item is read by sender")
  void shouldReturnFalseWhenInboxItemIsReadBySender() {
    // Given
    com.kirjaswappi.backend.service.entities.User receiver = new com.kirjaswappi.backend.service.entities.User()
        .id("receiver123");
    com.kirjaswappi.backend.service.entities.User sender = new com.kirjaswappi.backend.service.entities.User()
        .id("sender123");

    SwapRequest swapRequest = SwapRequest.builder()
        .id("swap1")
        .receiver(receiver)
        .sender(sender)
        .readBySenderAt(Instant.now())
        .build();

    // When
    boolean result = inboxService.isInboxItemUnread(swapRequest, "sender123");

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("Should return false when user is neither sender nor receiver")
  void shouldReturnFalseWhenUserIsNeitherSenderNorReceiver() {
    // Given
    com.kirjaswappi.backend.service.entities.User receiver = new com.kirjaswappi.backend.service.entities.User()
        .id("receiver123");
    com.kirjaswappi.backend.service.entities.User sender = new com.kirjaswappi.backend.service.entities.User()
        .id("sender123");

    SwapRequest swapRequest = SwapRequest.builder()
        .id("swap1")
        .receiver(receiver)
        .sender(sender)
        .build();

    // When
    boolean result = inboxService.isInboxItemUnread(swapRequest, "unauthorized123");

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("Should get unread message count from chat service")
  void shouldGetUnreadMessageCountFromChatService() {
    // Given
    when(chatService.getUnreadMessageCount("swap123", "receiver123")).thenReturn(5L);

    // When
    long count = inboxService.getUnreadMessageCount("receiver123", "swap123");

    // Then
    assertEquals(5L, count);
    verify(chatService).getUnreadMessageCount("swap123", "receiver123");
  }

  @Test
  @DisplayName("Should return zero when no unread messages")
  void shouldReturnZeroWhenNoUnreadMessages() {
    // Given
    when(chatService.getUnreadMessageCount("swap123", "receiver123")).thenReturn(0L);

    // When
    long count = inboxService.getUnreadMessageCount("receiver123", "swap123");

    // Then
    assertEquals(0L, count);
    verify(chatService).getUnreadMessageCount("swap123", "receiver123");
  }

  @Test
  @DisplayName("Should clear unread count cache")
  void shouldClearUnreadCountCache() {
    // When
    inboxService.clearUnreadCountCache("receiver123", "swap123");

    // Then - method should complete without error
    // The @CacheEvict annotation handles the cache clearing
    // No assertions needed as this is a void method with annotation-based behavior
  }

  @Test
  @DisplayName("Should handle empty status filter as null")
  void shouldHandleEmptyStatusFilterAsNull() {
    // Given
    when(userService.getUser("receiver123")).thenReturn(userEntity);
    when(swapRequestRepository.findByReceiverIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(Arrays.asList(receivedSwapRequest));
    when(swapRequestRepository.findBySenderIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(Arrays.asList(sentSwapRequest));
    when(chatService.getLatestMessageTimestamp(anyString())).thenReturn(Optional.empty());

    // When
    List<SwapRequest> result = inboxService.getUnifiedInbox("receiver123", "   ", null);

    // Then
    assertEquals(2, result.size());
    verify(swapRequestRepository).findByReceiverIdOrderByRequestedAtDesc("receiver123");
    verify(swapRequestRepository).findBySenderIdOrderByRequestedAtDesc("receiver123");
    // Should not call status-filtered methods
    verify(swapRequestRepository, never()).findByReceiverIdAndSwapStatusOrderByRequestedAtDesc(anyString(),
        anyString());
  }

  @Test
  @DisplayName("Should handle empty sort option as null")
  void shouldHandleEmptySortOptionAsNull() {
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
    List<SwapRequest> result = inboxService.getUnifiedInbox("receiver123", null, "   ");

    // Then
    assertEquals(2, result.size());
    // Should default to latest message sorting
    verify(chatService).getLatestMessageTimestamp("received1");
    verify(chatService).getLatestMessageTimestamp("sent1");
  }

  @Test
  @DisplayName("Should return empty list when user has no swap requests")
  void shouldReturnEmptyListWhenUserHasNoSwapRequests() {
    // Given
    when(userService.getUser("receiver123")).thenReturn(userEntity);
    when(swapRequestRepository.findByReceiverIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(Arrays.asList());
    when(swapRequestRepository.findBySenderIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(Arrays.asList());

    // When
    List<SwapRequest> result = inboxService.getUnifiedInbox("receiver123", null, null);

    // Then
    assertEquals(0, result.size());
    verify(userService).getUser("receiver123");
  }

  @Test
  @DisplayName("Should throw exception when receiver tries to mark sender request as expired")
  void shouldThrowExceptionWhenReceiverTriesToMarkSenderRequestAsExpired() {
    // Given
    when(swapRequestRepository.findById("received1")).thenReturn(Optional.of(receivedSwapRequest));

    // When & Then
    assertThrows(IllegalArgumentException.class,
        () -> inboxService.updateSwapRequestStatus("received1", SwapStatus.EXPIRED.getCode(), "receiver123"));
    verify(swapRequestRepository).findById("received1");
    verify(swapRequestRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should throw exception when sender tries to accept their own request")
  void shouldThrowExceptionWhenSenderTriesToAcceptTheirOwnRequest() {
    // Given
    when(swapRequestRepository.findById("received1")).thenReturn(Optional.of(receivedSwapRequest));

    // When & Then
    assertThrows(IllegalArgumentException.class,
        () -> inboxService.updateSwapRequestStatus("received1", SwapStatus.ACCEPTED.getCode(), "sender123"));
    verify(swapRequestRepository).findById("received1");
    verify(swapRequestRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should allow receiver to reject swap request")
  void shouldAllowReceiverToRejectSwapRequest() {
    // Given
    when(swapRequestRepository.findById("received1")).thenReturn(Optional.of(receivedSwapRequest));
    when(swapRequestRepository.save(any(SwapRequestDao.class))).thenReturn(receivedSwapRequest);

    // When
    SwapRequest result = inboxService.updateSwapRequestStatus("received1", SwapStatus.REJECTED.getCode(),
        "receiver123");

    // Then
    assertNotNull(result);
    assertEquals(SwapStatus.REJECTED.getCode(), receivedSwapRequest.swapStatus());
    verify(swapRequestRepository).save(receivedSwapRequest);
    verify(eventPublisher, times(2)).publishEvent(any(InboxUpdateEvent.class));
  }

  @Test
  @DisplayName("Should throw BadRequestException for invalid status code")
  void shouldThrowBadRequestExceptionForInvalidStatusCode() {
    // Given - validation happens before repository call, so no need to mock

    // When & Then
    assertThrows(BadRequestException.class,
        () -> inboxService.updateSwapRequestStatus("received1", "INVALID_STATUS", "receiver123"));
    // Validation happens first, so repository should not be called
    verifyNoInteractions(swapRequestRepository);
  }
}
