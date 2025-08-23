/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service.entities;

import java.time.Instant;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {
  private String id;
  private String swapRequestId;
  private User sender;
  private String message;
  private List<String> imageIds; // Store unique IDs, not URLs
  private Instant sentAt;
  private boolean readByReceiver;
}
