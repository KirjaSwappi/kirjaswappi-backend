/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service.entities;

import java.time.Instant;

import lombok.*;

import com.kirjaswappi.backend.service.enums.SwapStatus;
import com.kirjaswappi.backend.service.enums.SwapType;

@With
@Builder
public record SwapRequest(
    String id,
    User sender,
    User receiver,
    Book bookToSwapWith,
    SwapType swapType,
    SwapOffer swapOffer,
    boolean askForGiveaway,
    SwapStatus swapStatus,
    String note,
    Instant requestedAt,
    Instant updatedAt,
    Instant readByReceiverAt,
    Instant readBySenderAt
) {
}
