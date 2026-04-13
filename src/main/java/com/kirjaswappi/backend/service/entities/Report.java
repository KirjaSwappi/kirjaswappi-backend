/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service.entities;

import java.time.Instant;

import lombok.Builder;
import lombok.With;

@With
@Builder
public record Report(
    String id,
    String reporterUserId,
    String reportedUserId,
    String reason,
    Instant createdAt
) {
}
