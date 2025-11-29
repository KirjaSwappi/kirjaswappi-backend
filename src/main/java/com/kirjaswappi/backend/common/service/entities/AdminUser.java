/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service.entities;

import lombok.*;

import com.kirjaswappi.backend.common.service.enums.Role;

@With
@Builder
public record AdminUser(
    String username,
    String password,
    Role role
) {
}
