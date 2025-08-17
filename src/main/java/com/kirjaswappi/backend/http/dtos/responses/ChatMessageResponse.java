/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.responses;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

import com.kirjaswappi.backend.service.entities.ChatMessage;
import com.kirjaswappi.backend.service.entities.User;

@Getter
@Setter
public class ChatMessageResponse {
  private String id;
  private String swapRequestId;
  private SenderResponse sender;
  private String message;
  private Instant sentAt;
  private boolean readByReceiver;
  private boolean isOwnMessage;

  public ChatMessageResponse(ChatMessage entity) {
    this.id = entity.getId();
    this.swapRequestId = entity.getSwapRequestId();
    this.sender = new SenderResponse(entity.getSender());
    this.message = entity.getMessage();
    this.sentAt = entity.getSentAt();
    this.readByReceiver = entity.isReadByReceiver();
    this.isOwnMessage = false; // Will be set separately
  }

  public ChatMessageResponse(ChatMessage entity, String currentUserId) {
    this.id = entity.getId();
    this.swapRequestId = entity.getSwapRequestId();
    this.sender = new SenderResponse(entity.getSender());
    this.message = entity.getMessage();
    this.sentAt = entity.getSentAt();
    this.readByReceiver = entity.isReadByReceiver();
    this.isOwnMessage = entity.getSender().getId().equals(currentUserId);
  }

  @Getter
  @Setter
  static class SenderResponse {
    private String id;
    private String name;

    public SenderResponse(User entity) {
      this.id = entity.getId();
      this.name = entity.getFirstName() + " " + entity.getLastName();
    }
  }
}