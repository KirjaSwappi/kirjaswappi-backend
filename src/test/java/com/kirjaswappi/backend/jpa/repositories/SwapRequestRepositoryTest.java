/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.jpa.repositories;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.kirjaswappi.backend.jpa.daos.BookDao;
import com.kirjaswappi.backend.jpa.daos.SwapRequestDao;
import com.kirjaswappi.backend.jpa.daos.UserDao;
import com.kirjaswappi.backend.service.enums.SwapStatus;

class SwapRequestRepositoryTest {

  @Mock
  private SwapRequestRepository swapRequestRepository;

  private UserDao senderDao;
  private UserDao receiverDao;
  private BookDao bookDao;
  private SwapRequestDao swapRequest1;
  private SwapRequestDao swapRequest2;
  private SwapRequestDao swapRequest3;

  @BeforeEach
  @DisplayName("Setup mocks for SwapRequestRepository tests")
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // Create test users
    senderDao = UserDao.builder()
        .id("sender123")
        .firstName("John")
        .lastName("Sender")
        .email("sender@example.com")
        .password("hashedPassword")
        .salt("salt")
        .isEmailVerified(true)
        .build();

    receiverDao = UserDao.builder()
        .id("receiver123")
        .firstName("Jane")
        .lastName("Receiver")
        .email("receiver@example.com")
        .password("hashedPassword")
        .salt("salt")
        .isEmailVerified(true)
        .build();

    // Create test book
    bookDao = BookDao.builder()
        .id("book123")
        .title("Test Book")
        .author("Test Author")
        .build();

    // Create test swap requests with different timestamps and statuses
    swapRequest1 = SwapRequestDao.builder()
        .id("swap1")
        .sender(senderDao)
        .receiver(receiverDao)
        .bookToSwapWith(bookDao)
        .swapType("ByBooks")
        .askForGiveaway(false)
        .swapStatus(SwapStatus.PENDING.getCode())
        .requestedAt(Instant.parse("2025-01-01T10:00:00Z"))
        .updatedAt(Instant.parse("2025-01-01T10:00:00Z"))
        .build();

    swapRequest2 = SwapRequestDao.builder()
        .id("swap2")
        .sender(senderDao)
        .receiver(receiverDao)
        .bookToSwapWith(bookDao)
        .swapType("ByBooks")
        .askForGiveaway(false)
        .swapStatus(SwapStatus.ACCEPTED.getCode())
        .requestedAt(Instant.parse("2025-01-02T10:00:00Z"))
        .updatedAt(Instant.parse("2025-01-02T10:00:00Z"))
        .build();

    swapRequest3 = SwapRequestDao.builder()
        .id("swap3")
        .sender(receiverDao)
        .receiver(senderDao)
        .bookToSwapWith(bookDao)
        .swapType("ByBooks")
        .askForGiveaway(false)
        .swapStatus(SwapStatus.REJECTED.getCode())
        .requestedAt(Instant.parse("2025-01-03T10:00:00Z"))
        .updatedAt(Instant.parse("2025-01-03T10:00:00Z"))
        .build();
  }

  @Test
  @DisplayName("Should find swap requests by receiver ID ordered by requested date descending")
  void shouldFindByReceiverIdOrderByRequestedAtDesc() {
    // Given
    List<SwapRequestDao> expectedResult = Arrays.asList(swapRequest2, swapRequest1);
    when(swapRequestRepository.findByReceiverIdOrderByRequestedAtDesc("receiver123"))
        .thenReturn(expectedResult);

    // When
    List<SwapRequestDao> result = swapRequestRepository.findByReceiverIdOrderByRequestedAtDesc("receiver123");

    // Then
    assertEquals(2, result.size());
    assertEquals("swap2", result.getFirst().id()); // Most recent first
    assertEquals("swap1", result.getFirst().id());
    verify(swapRequestRepository).findByReceiverIdOrderByRequestedAtDesc("receiver123");
  }

  @Test
  @DisplayName("Should find swap requests by sender ID ordered by requested date descending")
  void shouldFindBySenderIdOrderByRequestedAtDesc() {
    // Given
    List<SwapRequestDao> expectedResult = Arrays.asList(swapRequest2, swapRequest1);
    when(swapRequestRepository.findBySenderIdOrderByRequestedAtDesc("sender123"))
        .thenReturn(expectedResult);

    // When
    List<SwapRequestDao> result = swapRequestRepository.findBySenderIdOrderByRequestedAtDesc("sender123");

    // Then
    assertEquals(2, result.size());
    assertEquals("swap2", result.getFirst().id()); // Most recent first
    assertEquals("swap1", result.getFirst().id());
    verify(swapRequestRepository).findBySenderIdOrderByRequestedAtDesc("sender123");
  }

  @Test
  @DisplayName("Should find swap requests by receiver ID and status ordered by requested date descending")
  void shouldFindByReceiverIdAndSwapStatusOrderByRequestedAtDesc() {
    // Given
    when(swapRequestRepository.findByReceiverIdAndSwapStatusOrderByRequestedAtDesc("receiver123",
        SwapStatus.PENDING.getCode()))
            .thenReturn(List.of(swapRequest1));
    when(swapRequestRepository.findByReceiverIdAndSwapStatusOrderByRequestedAtDesc("receiver123",
        SwapStatus.ACCEPTED.getCode()))
            .thenReturn(List.of(swapRequest2));

    // When
    List<SwapRequestDao> pendingResult = swapRequestRepository
        .findByReceiverIdAndSwapStatusOrderByRequestedAtDesc("receiver123", SwapStatus.PENDING.getCode());
    List<SwapRequestDao> acceptedResult = swapRequestRepository
        .findByReceiverIdAndSwapStatusOrderByRequestedAtDesc("receiver123", SwapStatus.ACCEPTED.getCode());

    // Then
    assertEquals(1, pendingResult.size());
    assertEquals("swap1", pendingResult.getFirst().id());
    assertEquals(SwapStatus.PENDING.getCode(), pendingResult.getFirst().swapStatus());

    assertEquals(1, acceptedResult.size());
    assertEquals("swap2", acceptedResult.getFirst().id());
    assertEquals(SwapStatus.ACCEPTED.getCode(), acceptedResult.getFirst().swapStatus());

    verify(swapRequestRepository).findByReceiverIdAndSwapStatusOrderByRequestedAtDesc("receiver123",
        SwapStatus.PENDING.getCode());
    verify(swapRequestRepository).findByReceiverIdAndSwapStatusOrderByRequestedAtDesc("receiver123",
        SwapStatus.ACCEPTED.getCode());
  }

  @Test
  @DisplayName("Should find swap requests by sender ID and status ordered by requested date descending")
  void shouldFindBySenderIdAndSwapStatusOrderByRequestedAtDesc() {
    // Given
    when(swapRequestRepository.findBySenderIdAndSwapStatusOrderByRequestedAtDesc("sender123",
        SwapStatus.PENDING.getCode()))
            .thenReturn(List.of(swapRequest1));
    when(swapRequestRepository.findBySenderIdAndSwapStatusOrderByRequestedAtDesc("receiver123",
        SwapStatus.REJECTED.getCode()))
            .thenReturn(List.of(swapRequest3));

    // When
    List<SwapRequestDao> pendingResult = swapRequestRepository
        .findBySenderIdAndSwapStatusOrderByRequestedAtDesc("sender123", SwapStatus.PENDING.getCode());
    List<SwapRequestDao> rejectedResult = swapRequestRepository
        .findBySenderIdAndSwapStatusOrderByRequestedAtDesc("receiver123", SwapStatus.REJECTED.getCode());

    // Then
    assertEquals(1, pendingResult.size());
    assertEquals("swap1", pendingResult.getFirst().id());
    assertEquals(SwapStatus.PENDING.getCode(), pendingResult.getFirst().swapStatus());

    assertEquals(1, rejectedResult.size());
    assertEquals("swap3", rejectedResult.getFirst().id());
    assertEquals(SwapStatus.REJECTED.getCode(), rejectedResult.getFirst().swapStatus());

    verify(swapRequestRepository).findBySenderIdAndSwapStatusOrderByRequestedAtDesc("sender123",
        SwapStatus.PENDING.getCode());
    verify(swapRequestRepository).findBySenderIdAndSwapStatusOrderByRequestedAtDesc("receiver123",
        SwapStatus.REJECTED.getCode());
  }

  @Test
  @DisplayName("Should return empty list when no swap requests found for receiver")
  void shouldReturnEmptyListWhenNoSwapRequestsFoundForReceiver() {
    // Given
    when(swapRequestRepository.findByReceiverIdOrderByRequestedAtDesc("nonexistent"))
        .thenReturn(List.of());

    // When
    List<SwapRequestDao> result = swapRequestRepository.findByReceiverIdOrderByRequestedAtDesc("nonexistent");

    // Then
    assertTrue(result.isEmpty());
    verify(swapRequestRepository).findByReceiverIdOrderByRequestedAtDesc("nonexistent");
  }

  @Test
  @DisplayName("Should return empty list when no swap requests found for sender")
  void shouldReturnEmptyListWhenNoSwapRequestsFoundForSender() {
    // Given
    when(swapRequestRepository.findBySenderIdOrderByRequestedAtDesc("nonexistent"))
        .thenReturn(List.of());

    // When
    List<SwapRequestDao> result = swapRequestRepository.findBySenderIdOrderByRequestedAtDesc("nonexistent");

    // Then
    assertTrue(result.isEmpty());
    verify(swapRequestRepository).findBySenderIdOrderByRequestedAtDesc("nonexistent");
  }

  @Test
  @DisplayName("Should return empty list when no swap requests found for receiver and status")
  void shouldReturnEmptyListWhenNoSwapRequestsFoundForReceiverAndStatus() {
    // Given
    when(swapRequestRepository.findByReceiverIdAndSwapStatusOrderByRequestedAtDesc("receiver123",
        SwapStatus.EXPIRED.getCode()))
            .thenReturn(List.of());

    // When
    List<SwapRequestDao> result = swapRequestRepository
        .findByReceiverIdAndSwapStatusOrderByRequestedAtDesc("receiver123", SwapStatus.EXPIRED.getCode());

    // Then
    assertTrue(result.isEmpty());
    verify(swapRequestRepository).findByReceiverIdAndSwapStatusOrderByRequestedAtDesc("receiver123",
        SwapStatus.EXPIRED.getCode());
  }

  @Test
  @DisplayName("Should return empty list when no swap requests found for sender and status")
  void shouldReturnEmptyListWhenNoSwapRequestsFoundForSenderAndStatus() {
    // Given
    when(swapRequestRepository.findBySenderIdAndSwapStatusOrderByRequestedAtDesc("sender123",
        SwapStatus.EXPIRED.getCode()))
            .thenReturn(List.of());

    // When
    List<SwapRequestDao> result = swapRequestRepository
        .findBySenderIdAndSwapStatusOrderByRequestedAtDesc("sender123", SwapStatus.EXPIRED.getCode());

    // Then
    assertTrue(result.isEmpty());
    verify(swapRequestRepository).findBySenderIdAndSwapStatusOrderByRequestedAtDesc("sender123",
        SwapStatus.EXPIRED.getCode());
  }
}
