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
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import com.mongodb.lang.Nullable;

@Document(collection = "swap_requests")
@CompoundIndexes({
    @CompoundIndex(name = "receiver_status_requested_idx", def = "{'receiver': 1, 'swapStatus': 1, 'requestedAt': -1}"),
    @CompoundIndex(name = "sender_status_requested_idx", def = "{'sender': 1, 'swapStatus': 1, 'requestedAt': -1}")
})
@Getter
@Setter
@Builder
@Accessors(fluent = true)
@NoArgsConstructor
@AllArgsConstructor
public class SwapRequestDao {
  @Id
  private String id;

  @NotNull
  @DBRef
  private UserDao sender;

  @NotNull
  @DBRef
  private UserDao receiver;

  @NotNull
  @DBRef
  private BookDao bookToSwapWith;

  @NotNull
  private String swapType;

  @Nullable // can be null for: GiveAway/OpenForOffers
  private SwapOfferDao swapOfferDao;

  @NotNull
  private boolean askForGiveaway;

  @NotNull
  private String swapStatus;

  @Nullable
  private String note;

  @NotNull
  private Instant requestedAt;

  @NotNull
  private Instant updatedAt;

  @Nullable
  private Instant readByReceiverAt;

  @Nullable
  private Instant readBySenderAt;
}
