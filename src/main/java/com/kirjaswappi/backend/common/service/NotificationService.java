/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import notification.Notification.NotificationRequest;
import notification.Notification.NotificationResponse;
import notification.NotificationServiceGrpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.protobuf.Timestamp;

@Service
public class NotificationService {
  private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

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

  public void sendNotification(String userId, String title, String message) {
    if (!enabled) {
      logger.debug("Notification service is disabled, skipping notification for user: {}", userId);
      return;
    }

    try {
      Instant now = Instant.now();
      Timestamp timestamp = Timestamp.newBuilder()
          .setSeconds(now.getEpochSecond())
          .setNanos(now.getNano())
          .build();

      NotificationRequest request = NotificationRequest.newBuilder()
          .setUserId(userId)
          .setTitle(title)
          .setMessage(message)
          .setTime(timestamp)
          .build();

      NotificationResponse response = stub.sendNotification(request);

      if (response.getSuccess()) {
        logger.debug("Notification sent successfully to user: {}", userId);
      } else {
        logger.warn("Failed to send notification to user: {}", userId);
      }
    } catch (StatusRuntimeException e) {
      logger.error("Failed to send notification to user: {} - {}", userId, e.getStatus());
    } catch (Exception e) {
      logger.error("Unexpected error sending notification to user: {}", userId, e);
    }
  }

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
