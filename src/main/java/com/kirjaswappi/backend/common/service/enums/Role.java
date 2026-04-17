/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service.enums;

import java.util.Arrays;
import java.util.Objects;

import lombok.Getter;

import com.kirjaswappi.backend.service.exceptions.BadRequestException;

@Getter
public enum Role {
  MASTER_ADMIN("MasterAdmin"),
  ADMIN("Admin"),
  USER("User");

  private final String code;

  Role(String code) {
    this.code = code;
  }

  public static Role fromCode(String code) {
    Objects.requireNonNull(code);
    return Arrays.stream(Role.values())
        .filter(c -> c.getCode().equalsIgnoreCase(code))
        .findFirst()
        .orElseThrow(() -> new BadRequestException("invalidRole", code));
  }
}
