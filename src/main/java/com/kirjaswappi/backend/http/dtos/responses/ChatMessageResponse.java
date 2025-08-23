/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.responses;

import java.time.Instant;
import java.util.List;

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
  private List<String> imageUrls; // New field for image URLs
  private Instant sentAt;
  private boolean readByReceiver;
  private boolean ownMessage;

  private SwapContextResponse swapContext; // Book information for the swap

  public ChatMessageResponse(ChatMessage entity) {
    this.id = entity.getId();
    this.swapRequestId = entity.getSwapRequestId();
    this.sender = new SenderResponse(entity.getSender());
    this.message = entity.getMessage();
    this.imageUrls = entity.getImageIds(); // This will contain URLs when converted by service
    this.sentAt = entity.getSentAt();
    this.readByReceiver = entity.isReadByReceiver();
    this.ownMessage = false; // Will be set separately
    this.swapContext = null; // Will be set separately
  }

  public ChatMessageResponse(ChatMessage entity, String currentUserId) {
    this.id = entity.getId();
    this.swapRequestId = entity.getSwapRequestId();
    this.sender = new SenderResponse(entity.getSender());
    this.message = entity.getMessage();
    this.imageUrls = entity.getImageIds(); // This will contain URLs when converted by service
    this.sentAt = entity.getSentAt();
    this.readByReceiver = entity.isReadByReceiver();
    this.ownMessage = entity.getSender().getId().equals(currentUserId);
    this.swapContext = null; // Will be set separately
  }

  public boolean isOwnMessage() {
    return ownMessage;
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

  @Getter
  @Setter
  public static class SwapContextResponse {
    private BookInfoResponse requestedBook; // Book that was requested for swap
    private BookInfoResponse offeredBook; // Book that was offered in return (if any)
    private String offeredGenreName; // Genre offered (if no specific book)
    private String swapType;
    private String swapStatus;
    private boolean askForGiveaway;

    @Getter
    @Setter
    public static class BookInfoResponse {
      private String id;
      private String title;
      private String author;
      private String condition;
      private String coverPhotoUrl;

      public BookInfoResponse(com.kirjaswappi.backend.service.entities.Book entity) {
        this.id = entity.getId();
        this.title = entity.getTitle();
        this.author = entity.getAuthor();
        this.condition = entity.getCondition() != null ? entity.getCondition().getCode() : null;
        this.coverPhotoUrl = entity.getCoverPhotos() != null && !entity.getCoverPhotos().isEmpty()
            ? entity.getCoverPhotos().get(0)
            : null;
      }

      public BookInfoResponse(com.kirjaswappi.backend.service.entities.SwappableBook entity) {
        this.id = entity.getId();
        this.title = entity.getTitle();
        this.author = entity.getAuthor();
        this.condition = null; // SwappableBook doesn't have condition
        this.coverPhotoUrl = entity.getCoverPhoto();
      }
    }
  }
}
