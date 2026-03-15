/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.protobuf.Timestamp;
import com.kirjaswappi.backend.jpa.daos.NotificationOutboxDao;
import com.kirjaswappi.backend.jpa.repositories.NotificationOutboxRepository;
import com.kirjaswappi.backend.proto.notification.NotificationRequest;
import com.kirjaswappi.backend.proto.notification.NotificationResponse;
import com.kirjaswappi.backend.proto.notification.NotificationServiceGrpc;

@Service
public class NotificationService implements NotificationClient {
  private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

  private static final String STATUS_PENDING = "PENDING";
  private static final String STATUS_SENT = "SENT";
  private static final String STATUS_FAILED = "FAILED";
  private static final int MAX_RETRIES = 3;
  private static final Duration FAILED_RETENTION = Duration.ofDays(7);
  private static final long CLEANUP_INTERVAL_MS = 86_400_000L; // 1 day

  @Autowired
  private NotificationOutboxRepository notificationOutboxRepository;

  private final ManagedChannel channel;
  private final NotificationServiceGrpc.NotificationServiceBlockingStub stub;
  private final boolean enabled;

  public NotificationService(
      @Value("${notification.service.host}") String host,
      @Value("${notification.service.port}") int port,
      @Value("${notification.service.enabled:true}") boolean enabled) {

    this.enabled = enabled;

    if (enabled) {
      this.channel = ManagedChannelBuilder.forAddress(host, port)
          .usePlaintext()
          .keepAliveTime(30, TimeUnit.SECONDS)
          .keepAliveTimeout(5, TimeUnit.SECONDS)
          .keepAliveWithoutCalls(true)
          .build();

      this.stub = NotificationServiceGrpc.newBlockingStub(channel);
      logger.info("Notification service client initialized for {}:{}", host, port);
    } else {
      this.channel = null;
      this.stub = null;
      logger.info("Notification service is disabled");
    }
  }

  @Override
  public void sendNotification(String userId, String title, String message) {
    if (!enabled) {
      logger.debug("Notification service is disabled, skipping notification for user: {}", userId);
      return;
    }

    try {
      NotificationOutboxDao outbox = NotificationOutboxDao.builder()
          .userId(userId)
          .title(title)
          .message(message)
          .status(STATUS_PENDING)
          .createdAt(Instant.now())
          .retryCount(0)
          .build();

      notificationOutboxRepository.save(outbox);
      logger.debug("Notification queued in outbox for user: {}", userId);
    } catch (Exception e) {
      logger.error("Failed to queue notification for user: {}", userId, e);
    }
  }

  @Scheduled(fixedDelay = 5000) // Run every 5 seconds
  public void processOutbox() {
    if (!enabled || stub == null) {
      return;
    }

    List<NotificationOutboxDao> pendingNotifications = notificationOutboxRepository
        .findByStatusOrderByCreatedAtAsc(STATUS_PENDING);
    if (pendingNotifications.isEmpty()) {
      return;
    }

    logger.debug("Processing {} pending notifications", pendingNotifications.size());

    for (NotificationOutboxDao notification : pendingNotifications) {
      processNotification(notification);
    }
  }

  private void processNotification(NotificationOutboxDao notification) {
    try {
      Instant now = Instant.now();
      NotificationRequest request = NotificationRequest.newBuilder()
          .setUserId(notification.userId())
          .setTitle(notification.title())
          .setMessage(notification.message())
          .setTime(Timestamp.newBuilder()
              .setSeconds(now.getEpochSecond())
              .setNanos(now.getNano())
              .build())
          .build();

      NotificationResponse response = stub.sendNotification(request);

      if (response.getSuccess()) {
        notification.status(STATUS_SENT)
            .sentAt(Instant.now());
        logger.debug("Notification sent successfully to user: {}", notification.userId());
      } else {
        handleHelper(notification, "Service returned failure");
      }
    } catch (StatusRuntimeException e) {
      handleHelper(notification, "gRPC Status: " + e.getStatus());
    } catch (Exception e) {
      handleHelper(notification, "Exception: " + e.getMessage());
    } finally {
      notificationOutboxRepository.save(notification);
    }
  }

  private void handleHelper(NotificationOutboxDao notification, String error) {
    if (notification.retryCount() >= MAX_RETRIES) {
      notification.status(STATUS_FAILED);
      logger.error("Notification failed permanently for user: {}. Error: {}", notification.userId(), error);
    } else {
      notification.retryCount(notification.retryCount() + 1);
      // Keep status PENDING to try again
      logger.warn("Notification failed for user: {}. Retrying ({}/{}). Error: {}",
          notification.userId(), notification.retryCount(), MAX_RETRIES, error);
    }
    notification.errorMessage(error);
  }

  @Scheduled(fixedDelay = CLEANUP_INTERVAL_MS)
  public void cleanupFailedNotifications() {
    if (!enabled) {
      return;
    }

    Instant cutoff = Instant.now().minus(FAILED_RETENTION);
    long deleted = notificationOutboxRepository.deleteByStatusAndCreatedAtBefore(STATUS_FAILED, cutoff);
    logger.debug("Cleaned up {} FAILED notifications older than {} days", deleted, FAILED_RETENTION.toDays());
  }

  @Override
  public void shutdown() {
    if (!enabled || channel == null) {
      return;
    }

    try {
      channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      logger.warn("Interrupted while shutting down notification service client");
      Thread.currentThread().interrupt();
    }
  }
}
