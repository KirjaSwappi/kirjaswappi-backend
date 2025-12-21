/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service;

/**
 * Interface for notification service operations. This interface allows for easy
 * mocking in tests.
 */
public interface NotificationClient {

  /**
   * Sends a notification to a user.
   *
   * @param userId  the user ID to send the notification to
   * @param title   the notification title
   * @param message the notification message
   */
  void sendNotification(String userId, String title, String message);

  /**
   * Shuts down the notification service.
   */
  void shutdown();
}
