/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;

import io.grpc.ManagedChannel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.kirjaswappi.backend.jpa.daos.NotificationOutboxDao;
import com.kirjaswappi.backend.jpa.repositories.NotificationOutboxRepository;
import com.kirjaswappi.backend.proto.notification.NotificationResponse;
import com.kirjaswappi.backend.proto.notification.NotificationServiceGrpc;

class NotificationServiceTest {

  @Mock
  private NotificationOutboxRepository notificationOutboxRepository;

  @Mock
  private ManagedChannel channel;

  @Mock
  private NotificationServiceGrpc.NotificationServiceBlockingStub stub;

  private NotificationService notificationService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    // Initialize NotificationService manually to control @Value fields
    notificationService = new NotificationService("localhost", 9090, true);

    // Inject mocks using ReflectionTestUtils since they are not injected by
    // constructor
    ReflectionTestUtils.setField(notificationService, "notificationOutboxRepository", notificationOutboxRepository);
    ReflectionTestUtils.setField(notificationService, "channel", channel);
    ReflectionTestUtils.setField(notificationService, "stub", stub);
  }

  @Test
  @DisplayName("Should save notification to outbox when sendNotification is called")
  void shouldSaveNotificationToOutbox() {
    // When
    notificationService.sendNotification("user1", "Test Title", "Test Message");

    // Then
    verify(notificationOutboxRepository).save(any(NotificationOutboxDao.class));
    verifyNoInteractions(stub); // Should not call gRPC directly
  }

  @Test
  @DisplayName("Should process pending notifications from outbox")
  void shouldProcessPendingNotifications() {
    // Given
    NotificationOutboxDao pendingNotification = NotificationOutboxDao.builder()
        .id("notif1")
        .userId("user1")
        .title("Title")
        .message("Message")
        .status("PENDING")
        .retryCount(0)
        .createdAt(Instant.now())
        .build();

    when(notificationOutboxRepository.findByStatusOrderByCreatedAtAsc("PENDING"))
        .thenReturn(List.of(pendingNotification));

    NotificationResponse successResponse = NotificationResponse.newBuilder()
        .setSuccess(true)
        .build();
    when(stub.sendNotification(any())).thenReturn(successResponse);

    // When
    notificationService.processOutbox();

    // Then
    verify(stub).sendNotification(any());
    verify(notificationOutboxRepository)
        .save(argThat(notification -> notification.status().equals("SENT") && notification.sentAt() != null));
  }

  @Test
  @DisplayName("Should increment retry count on failure")
  void shouldIncrementRetryCountOnFailure() {
    // Given
    NotificationOutboxDao pendingNotification = NotificationOutboxDao.builder()
        .id("notif1")
        .userId("user1")
        .title("Title")
        .message("Message")
        .status("PENDING")
        .retryCount(0)
        .createdAt(Instant.now())
        .build();

    when(notificationOutboxRepository.findByStatusOrderByCreatedAtAsc("PENDING"))
        .thenReturn(List.of(pendingNotification));

    when(stub.sendNotification(any())).thenThrow(new RuntimeException("gRPC Error"));

    // When
    notificationService.processOutbox();

    // Then
    verify(stub).sendNotification(any());
    verify(notificationOutboxRepository).save(argThat(notification -> notification.status().equals("PENDING") &&
        notification.retryCount() == 1 &&
        notification.errorMessage().contains("gRPC Error")));
  }

  @Test
  @DisplayName("Should mark as FAILED after max retries")
  void shouldMarkAsFailedAfterMaxRetries() {
    // Given
    NotificationOutboxDao pendingNotification = NotificationOutboxDao.builder()
        .id("notif1")
        .userId("user1")
        .title("Title")
        .message("Message")
        .status("PENDING")
        .retryCount(3) // Already at max retries (assuming max=3 check is >=)
        .createdAt(Instant.now())
        .build();

    when(notificationOutboxRepository.findByStatusOrderByCreatedAtAsc("PENDING"))
        .thenReturn(List.of(pendingNotification));

    when(stub.sendNotification(any())).thenThrow(new RuntimeException("gRPC Error"));

    // When
    notificationService.processOutbox();

    // Then
    verify(stub).sendNotification(any());
    verify(notificationOutboxRepository).save(argThat(notification -> notification.status().equals("FAILED")));
  }
}
