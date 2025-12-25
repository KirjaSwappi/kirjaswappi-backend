/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.jpa.daos;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.Accessors;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.mongodb.lang.Nullable;

@Document(collection = "notification_outbox")
@Getter
@Setter
@Builder
@Accessors(fluent = true)
@NoArgsConstructor
@AllArgsConstructor
public class NotificationOutboxDao {
  @Id
  private String id;

  @NotNull
  private String userId;

  @NotNull
  private String title;

  @NotNull
  private String message;

  @NotNull
  private String status; // PENDING, SENT, FAILED

  private int retryCount;

  @NotNull
  private Instant createdAt;

  @Nullable
  private Instant sentAt;

  @Nullable
  private String errorMessage;
}
