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

  public InboxItemResponse(SwapRequest entity) {
    this.id = entity.getId();
    this.swapType = entity.getSwapType().getCode();
    this.swapStatus = entity.getSwapStatus().getCode();
    this.note = entity.getNote();
    this.requestedAt = entity.getRequestedAt();
    this.updatedAt = entity.getUpdatedAt();
    this.sender = new UserSummaryResponse(entity.getSender());
    this.receiver = new UserSummaryResponse(entity.getReceiver());
    this.bookToSwapWith = new BookSummaryResponse(entity.getBookToSwapWith());
    this.swapOffer = entity.getSwapOffer() != null ? new SwapOfferSummaryResponse(entity.getSwapOffer()) : null;
    this.askForGiveaway = entity.isAskForGiveaway();
    this.unreadMessageCount = 0; // Will be set separately by service
    this.isUnread = false; // Will be set separately by service
    this.hasNewMessages = false; // Will be set separately by service
    this.conversationType = null; // Will be set separately by service
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
      this.id = entity.getId();
      this.name = entity.getFirstName() + " " + entity.getLastName();
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
      this.id = entity.getId();
      this.title = entity.getTitle();
      this.author = entity.getAuthor();
      this.condition = entity.getCondition() != null ? entity.getCondition().getCode() : null;
    }
  }

  @Getter
  @Setter
  static class SwapOfferSummaryResponse {
    private String offeredBookTitle;
    private String offeredGenreName;

    public SwapOfferSummaryResponse(com.kirjaswappi.backend.service.entities.SwapOffer entity) {
      if (entity.getOfferedBook() != null) {
        this.offeredBookTitle = entity.getOfferedBook().getTitle();
      }
      if (entity.getOfferedGenre() != null) {
        this.offeredGenreName = entity.getOfferedGenre().getName();
      }
    }
  }
}
