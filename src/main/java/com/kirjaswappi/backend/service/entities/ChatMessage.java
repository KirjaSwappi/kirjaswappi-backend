/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service.entities;

import java.time.Instant;
import java.util.List;

import lombok.*;
import lombok.experimental.Accessors;

@With
@Builder
@Accessors(fluent = true)
public record ChatMessage(
    String id,
    String swapRequestId,
    User sender,
    String message,
    List<String> imageIds, // Store unique IDs, not URLs
    Instant sentAt,
    boolean readByReceiver
) {

}
