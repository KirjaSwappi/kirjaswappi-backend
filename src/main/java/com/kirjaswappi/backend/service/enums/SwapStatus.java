/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import lombok.Getter;

import com.kirjaswappi.backend.service.exceptions.BadRequestException;

@Getter
public enum SwapStatus {
  PENDING("Pending"),
  ACCEPTED("Accepted"),
  RESERVED("Reserved"),
  REJECTED("Rejected"),
  EXPIRED("Expired");

  private final String code;

  SwapStatus(String code) {
    this.code = code;
  }

  public boolean canTransitionTo(SwapStatus target) {
    return getAllowedTransitions().contains(target);
  }

  private Set<SwapStatus> getAllowedTransitions() {
    return switch (this) {
    case PENDING -> Set.of(ACCEPTED, REJECTED, EXPIRED);
    case ACCEPTED -> Set.of(RESERVED, EXPIRED);
    case RESERVED -> Set.of(EXPIRED);
    case REJECTED, EXPIRED -> Set.of();
    };
  }

  public static SwapStatus fromCode(String code) {
    Objects.requireNonNull(code);
    return Arrays.stream(SwapStatus.values())
        .filter(c -> c.getCode().equalsIgnoreCase(code))
        .findFirst()
        .orElseThrow(() -> new BadRequestException("invalidSwapStatus", code));
  }

  public static List<String> getSupportedSwapStatusTypes() {
    return Stream.of(SwapStatus.values()).map(SwapStatus::getCode).toList();
  }
}
