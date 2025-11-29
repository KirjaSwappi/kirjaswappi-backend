/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.responses;

import java.time.Instant;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.jspecify.annotations.NonNull;

import com.kirjaswappi.backend.service.entities.Book;
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

  public ChatMessageResponse(@NonNull ChatMessage entity) {
    this.id = entity.id();
    this.swapRequestId = entity.swapRequestId();
    this.sender = new SenderResponse(entity.sender());
    this.message = entity.message();
    this.imageUrls = entity.imageIds(); // This will contain URLs when converted by service
    this.sentAt = entity.sentAt();
    this.readByReceiver = entity.readByReceiver();
    this.ownMessage = false; // Will be set separately
    this.swapContext = null; // Will be set separately
  }

  public ChatMessageResponse(ChatMessage entity, String currentUserId) {
    this.id = entity.id();
    this.swapRequestId = entity.swapRequestId();
    this.sender = new SenderResponse(entity.sender());
    this.message = entity.message();
    this.imageUrls = entity.imageIds(); // This will contain URLs when converted by service
    this.sentAt = entity.sentAt();
    this.readByReceiver = entity.readByReceiver();
    this.ownMessage = entity.sender().id().equals(currentUserId);
    this.swapContext = null; // Will be set separately
  }

  @Getter
  @Setter
  static class SenderResponse {
    private String id;
    private String name;

    public SenderResponse(User entity) {
      this.id = entity.id();
      this.name = entity.firstName() + " " + entity.lastName();
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

      public BookInfoResponse(@NonNull Book entity) {
        this.id = entity.id();
        this.title = entity.title();
        this.author = entity.author();
        this.condition = entity.condition() != null ? entity.condition().code() : null;
        this.coverPhotoUrl = entity.coverPhotos() != null && !entity.coverPhotos().isEmpty()
            ? entity.coverPhotos().getFirst()
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
