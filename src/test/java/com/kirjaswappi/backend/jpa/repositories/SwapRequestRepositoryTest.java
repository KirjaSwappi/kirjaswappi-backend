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
    senderDao = new UserDao();
    senderDao.setId("sender123");
    senderDao.setFirstName("John");
    senderDao.setLastName("Sender");
    senderDao.setEmail("sender@example.com");
    senderDao.setPassword("hashedPassword");
    senderDao.setSalt("salt");
    senderDao.setEmailVerified(true);

    receiverDao = new UserDao();
    receiverDao.setId("receiver123");
    receiverDao.setFirstName("Jane");
    receiverDao.setLastName("Receiver");
    receiverDao.setEmail("receiver@example.com");
    receiverDao.setPassword("hashedPassword");
    receiverDao.setSalt("salt");
    receiverDao.setEmailVerified(true);

    // Create test book
    bookDao = new BookDao();
    bookDao.setId("book123");
    bookDao.setTitle("Test Book");
    bookDao.setAuthor("Test Author");

    // Create test swap requests with different timestamps and statuses
    swapRequest1 = new SwapRequestDao();
    swapRequest1.setId("swap1");
    swapRequest1.setSender(senderDao);
    swapRequest1.setReceiver(receiverDao);
    swapRequest1.setBookToSwapWith(bookDao);
    swapRequest1.setSwapType("ByBooks");
    swapRequest1.setAskForGiveaway(false);
    swapRequest1.setSwapStatus(SwapStatus.PENDING.getCode());
    swapRequest1.setRequestedAt(Instant.parse("2025-01-01T10:00:00Z"));
    swapRequest1.setUpdatedAt(Instant.parse("2025-01-01T10:00:00Z"));

    swapRequest2 = new SwapRequestDao();
    swapRequest2.setId("swap2");
    swapRequest2.setSender(senderDao);
    swapRequest2.setReceiver(receiverDao);
    swapRequest2.setBookToSwapWith(bookDao);
    swapRequest2.setSwapType("ByBooks");
    swapRequest2.setAskForGiveaway(false);
    swapRequest2.setSwapStatus(SwapStatus.ACCEPTED.getCode());
    swapRequest2.setRequestedAt(Instant.parse("2025-01-02T10:00:00Z"));
    swapRequest2.setUpdatedAt(Instant.parse("2025-01-02T10:00:00Z"));

    swapRequest3 = new SwapRequestDao();
    swapRequest3.setId("swap3");
    swapRequest3.setSender(receiverDao); // Different sender
    swapRequest3.setReceiver(senderDao); // Different receiver
    swapRequest3.setBookToSwapWith(bookDao);
    swapRequest3.setSwapType("ByBooks");
    swapRequest3.setAskForGiveaway(false);
    swapRequest3.setSwapStatus(SwapStatus.REJECTED.getCode());
    swapRequest3.setRequestedAt(Instant.parse("2025-01-03T10:00:00Z"));
    swapRequest3.setUpdatedAt(Instant.parse("2025-01-03T10:00:00Z"));
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
    assertEquals("swap2", result.get(0).getId()); // Most recent first
    assertEquals("swap1", result.get(1).getId());
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
    assertEquals("swap2", result.get(0).getId()); // Most recent first
    assertEquals("swap1", result.get(1).getId());
    verify(swapRequestRepository).findBySenderIdOrderByRequestedAtDesc("sender123");
  }

  @Test
  @DisplayName("Should find swap requests by receiver ID and status ordered by requested date descending")
  void shouldFindByReceiverIdAndSwapStatusOrderByRequestedAtDesc() {
    // Given
    when(swapRequestRepository.findByReceiverIdAndSwapStatusOrderByRequestedAtDesc("receiver123",
        SwapStatus.PENDING.getCode()))
            .thenReturn(Arrays.asList(swapRequest1));
    when(swapRequestRepository.findByReceiverIdAndSwapStatusOrderByRequestedAtDesc("receiver123",
        SwapStatus.ACCEPTED.getCode()))
            .thenReturn(Arrays.asList(swapRequest2));

    // When
    List<SwapRequestDao> pendingResult = swapRequestRepository
        .findByReceiverIdAndSwapStatusOrderByRequestedAtDesc("receiver123", SwapStatus.PENDING.getCode());
    List<SwapRequestDao> acceptedResult = swapRequestRepository
        .findByReceiverIdAndSwapStatusOrderByRequestedAtDesc("receiver123", SwapStatus.ACCEPTED.getCode());

    // Then
    assertEquals(1, pendingResult.size());
    assertEquals("swap1", pendingResult.get(0).getId());
    assertEquals(SwapStatus.PENDING.getCode(), pendingResult.get(0).getSwapStatus());

    assertEquals(1, acceptedResult.size());
    assertEquals("swap2", acceptedResult.get(0).getId());
    assertEquals(SwapStatus.ACCEPTED.getCode(), acceptedResult.get(0).getSwapStatus());

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
            .thenReturn(Arrays.asList(swapRequest1));
    when(swapRequestRepository.findBySenderIdAndSwapStatusOrderByRequestedAtDesc("receiver123",
        SwapStatus.REJECTED.getCode()))
            .thenReturn(Arrays.asList(swapRequest3));

    // When
    List<SwapRequestDao> pendingResult = swapRequestRepository
        .findBySenderIdAndSwapStatusOrderByRequestedAtDesc("sender123", SwapStatus.PENDING.getCode());
    List<SwapRequestDao> rejectedResult = swapRequestRepository
        .findBySenderIdAndSwapStatusOrderByRequestedAtDesc("receiver123", SwapStatus.REJECTED.getCode());

    // Then
    assertEquals(1, pendingResult.size());
    assertEquals("swap1", pendingResult.get(0).getId());
    assertEquals(SwapStatus.PENDING.getCode(), pendingResult.get(0).getSwapStatus());

    assertEquals(1, rejectedResult.size());
    assertEquals("swap3", rejectedResult.get(0).getId());
    assertEquals(SwapStatus.REJECTED.getCode(), rejectedResult.get(0).getSwapStatus());

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
        .thenReturn(Arrays.asList());

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
        .thenReturn(Arrays.asList());

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
            .thenReturn(Arrays.asList());

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
            .thenReturn(Arrays.asList());

    // When
    List<SwapRequestDao> result = swapRequestRepository
        .findBySenderIdAndSwapStatusOrderByRequestedAtDesc("sender123", SwapStatus.EXPIRED.getCode());

    // Then
    assertTrue(result.isEmpty());
    verify(swapRequestRepository).findBySenderIdAndSwapStatusOrderByRequestedAtDesc("sender123",
        SwapStatus.EXPIRED.getCode());
  }
}
