/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kirjaswappi.backend.common.service.entities.AdminUser;
import com.kirjaswappi.backend.common.utils.JwtUtil;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

  private final JwtUtil jwtUtil;

  private final AdminUserService adminUserService;

  public AdminUser verifyLogin(AdminUser user) {
    // validate user credentials, if exists return user info
    return adminUserService.verifyUser(user);
  }

  public String generateJwtToken(AdminUser adminUser) {
    return jwtUtil.generateJwtToken(adminUser);
  }

  public String generateRefreshToken(AdminUser adminUser) {
    return jwtUtil.generateRefreshToken(adminUser);
  }

  public String verifyRefreshToken(String refreshToken) {
    String username = jwtUtil.extractUsername(refreshToken);
    AdminUser userDetails = adminUserService.getAdminUserInfo(username);
    if (jwtUtil.validateRefreshToken(refreshToken, userDetails)) {
      return jwtUtil.generateJwtToken(userDetails);
    }
    throw new BadRequestException("invalidRefreshToken", refreshToken);
  }
}
