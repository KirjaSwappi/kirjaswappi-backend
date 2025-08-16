/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service.entities;

import java.time.Instant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.kirjaswappi.backend.service.enums.SwapStatus;
import com.kirjaswappi.backend.service.enums.SwapType;

@Getter
@Setter
@NoArgsConstructor
public class SwapRequest {
  private String id;
  private User sender;
  private User receiver;
  private Book bookToSwapWith;
  private SwapType swapType;
  private SwapOffer swapOffer;
  private boolean askForGiveaway;
  private SwapStatus swapStatus;
  private String note;
  private Instant requestedAt;
  private Instant updatedAt;
  private Instant readByReceiverAt;
  private Instant readBySenderAt;
}
