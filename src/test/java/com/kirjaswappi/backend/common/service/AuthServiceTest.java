/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.kirjaswappi.backend.common.service.entities.AdminUser;
import com.kirjaswappi.backend.common.utils.JwtUtil;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;

class AuthServiceTest {
  @Mock
  private AdminUserService adminUserService;
  @Mock
  private JwtUtil jwtUtil;
  @InjectMocks
  private AuthService authService;

  private AdminUser adminUser;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    adminUser = AdminUser.builder().username("admin").password("pass").build();
  }

  @Test
  @DisplayName("Should throw exception when authentication fails")
  void authenticateThrowsOnFailure() {
    when(adminUserService.verifyUser(any())).thenThrow(new RuntimeException());
    assertThrows(RuntimeException.class, () -> authService.verifyLogin(adminUser));
  }

  @Test
  @DisplayName("Should authenticate successfully")
  void authenticateSuccess() {
    when(adminUserService.verifyUser(any())).thenReturn(adminUser);
    assertEquals(adminUser, authService.verifyLogin(adminUser));
  }

  @Test
  @DisplayName("Should generate JWT token")
  void generateJwtToken() {
    when(jwtUtil.generateJwtToken(adminUser)).thenReturn("jwt");
    assertEquals("jwt", authService.generateJwtToken(adminUser));
  }

  @Test
  @DisplayName("Should generate refresh token")
  void generateRefreshToken() {
    when(jwtUtil.generateRefreshToken(adminUser)).thenReturn("refresh");
    assertEquals("refresh", authService.generateRefreshToken(adminUser));
  }

  @Test
  @DisplayName("Should throw exception for invalid refresh token")
  void verifyRefreshTokenThrows() {
    when(jwtUtil.extractUsername("bad")).thenReturn("admin");
    when(adminUserService.getAdminUserInfo("admin")).thenReturn(adminUser);
    when(jwtUtil.validateRefreshToken("bad", adminUser)).thenReturn(false);
    assertThrows(BadRequestException.class, () -> authService.verifyRefreshToken("bad"));
  }

  @Test
  @DisplayName("Should verify refresh token and return new JWT")
  void verifyRefreshTokenSuccess() {
    when(jwtUtil.extractUsername("good")).thenReturn("admin");
    when(adminUserService.getAdminUserInfo("admin")).thenReturn(adminUser);
    when(jwtUtil.validateRefreshToken("good", adminUser)).thenReturn(true);
    when(jwtUtil.generateJwtToken(adminUser)).thenReturn("jwt");
    assertEquals("jwt", authService.verifyRefreshToken("good"));
  }
}
