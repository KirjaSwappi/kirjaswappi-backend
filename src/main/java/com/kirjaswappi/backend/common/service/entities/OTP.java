/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service.entities;

import java.time.Instant;

import lombok.*;

@With
@Builder
public record OTP(
    String email,
    String otp,
    Instant createdAt
) {
}
