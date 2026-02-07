/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.responses;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

import com.kirjaswappi.backend.service.entities.SwapRequest;
import com.kirjaswappi.backend.service.entities.User;

@Getter
@Setter
public class InboxItemResponse {
  private String id;
  private String swapType;
  private String swapStatus;
  private String note;
  private Instant requestedAt;
  private Instant updatedAt;
  private UserSummaryResponse sender;
  private UserSummaryResponse receiver;
  private BookSummaryResponse bookToSwapWith;
  private SwapOfferSummaryResponse swapOffer;
  private boolean askForGiveaway;
  private long unreadMessageCount;
  private boolean isUnread;
  private boolean hasNewMessages;
  private String conversationType; // "sent" or "received"
  private String lastMessageContent;
  private String lastMessageSenderId;
  private Instant lastMessageSentAt;
  private boolean lastMessageIsImage;

  public InboxItemResponse(SwapRequest entity) {
    this.id = entity.id();
    this.swapType = entity.swapType().getCode();
    this.swapStatus = entity.swapStatus().getCode();
    this.note = entity.note();
    this.requestedAt = entity.requestedAt();
    this.updatedAt = entity.updatedAt();
    this.sender = new UserSummaryResponse(entity.sender());
    this.receiver = new UserSummaryResponse(entity.receiver());
    this.bookToSwapWith = new BookSummaryResponse(entity.bookToSwapWith());
    this.swapOffer = entity.swapOffer() != null ? new SwapOfferSummaryResponse(entity.swapOffer()) : null;
    this.askForGiveaway = entity.askForGiveaway();
    this.unreadMessageCount = 0; // Will be set separately by service
    this.isUnread = false; // Will be set separately by service
    this.hasNewMessages = false; // Will be set separately by service
    this.conversationType = null; // Will be set separately by service
    this.lastMessageContent = null; // Will be set separately by service
    this.lastMessageSenderId = null; // Will be set separately by service
    this.lastMessageSentAt = null; // Will be set separately by service
    this.lastMessageIsImage = false; // Will be set separately by service
  }

  public void setConversationType(String conversationType) {
    this.conversationType = conversationType;
  }

  public String getConversationType() {
    return conversationType;
  }

  @Getter
  @Setter
  static class UserSummaryResponse {
    private String id;
    private String name;

    public UserSummaryResponse(User entity) {
      this.id = entity.id();
      this.name = entity.firstName() + " " + entity.lastName();
    }
  }

  @Getter
  @Setter
  static class BookSummaryResponse {
    private String id;
    private String title;
    private String author;
    private String condition;

    public BookSummaryResponse(com.kirjaswappi.backend.service.entities.Book entity) {
      this.id = entity.id();
      this.title = entity.title();
      this.author = entity.author();
      this.condition = entity.condition() != null ? entity.condition().code() : null;
    }
  }

  @Getter
  @Setter
  static class SwapOfferSummaryResponse {
    private String offeredBookTitle;
    private String offeredGenreName;

    public SwapOfferSummaryResponse(com.kirjaswappi.backend.service.entities.SwapOffer entity) {
      if (entity.offeredBook() != null) {
        this.offeredBookTitle = entity.offeredBook().getTitle();
      }
      if (entity.offeredGenre() != null) {
        this.offeredGenreName = entity.offeredGenre().getName();
      }
    }
  }
}
