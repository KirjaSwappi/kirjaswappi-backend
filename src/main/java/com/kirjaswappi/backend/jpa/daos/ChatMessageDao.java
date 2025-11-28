/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.jpa.daos;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.Accessors;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "chat_messages")
@Getter
@Setter
@Builder
@Accessors(fluent = true)
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDao {
  @Id
  private String id;

  @NotNull
  private String swapRequestId;

  @NotNull
  @DBRef
  private UserDao sender;

  private String message; // Make nullable since message can be empty if only images

  private List<String> imageIds; // Store unique IDs, not URLs

  @NotNull
  private Instant sentAt;

  @NotNull
  private boolean readByReceiver;
}
