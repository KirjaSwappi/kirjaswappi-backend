/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.responses;

import java.time.Instant;

import javax.swing.*;

import lombok.Getter;
import lombok.Setter;

import com.kirjaswappi.backend.service.entities.SwapOffer;
import com.kirjaswappi.backend.service.entities.SwapRequest;
import com.kirjaswappi.backend.service.entities.SwappableBook;

@Getter
@Setter
public class SwapRequestResponse {
  private String id;
  private String senderId;
  private String receiverId;
  private BookResponse bookToSwapWith;
  private String swapType;
  private SwapOfferResponse swapOffer;
  private boolean askForGiveaway;
  private String swapStatus;
  private String note;
  private Instant requestedAt;
  private Instant updatedAt;

  public SwapRequestResponse(SwapRequest entity) {
    this.id = entity.id();
    this.senderId = entity.sender().id();
    this.receiverId = entity.receiver().id();
    this.bookToSwapWith = new BookResponse(entity.bookToSwapWith());
    this.swapType = entity.swapType().getCode();
    this.swapOffer = entity.swapOffer() == null ? null : new SwapOfferResponse(entity.swapOffer());
    this.askForGiveaway = entity.askForGiveaway();
    this.swapStatus = entity.swapStatus().getCode();
    this.note = entity.note();
    this.requestedAt = entity.requestedAt();
    this.updatedAt = entity.updatedAt();
  }

  @Setter
  @Getter
  static class SwapOfferResponse {
    private OfferedBookResponse offeredBook;
    private GenreResponse offeredGenre;

    public SwapOfferResponse(SwapOffer entity) {
      this.offeredBook = entity.offeredBook() == null ? null : new OfferedBookResponse(entity.offeredBook());
      this.offeredGenre = entity.offeredGenre() == null ? null : new GenreResponse(entity.offeredGenre());
    }

    @Setter
    @Getter
    static class OfferedBookResponse {
      private String id;
      private String title;
      private String author;
      private String coverPhotoUrl;

      public OfferedBookResponse(SwappableBook entity) {
        this.id = entity.getId();
        this.title = entity.getTitle();
        this.author = entity.getAuthor();
        this.coverPhotoUrl = entity.getCoverPhoto();
      }
    }
  }
}
