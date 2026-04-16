/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.components;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.kirjaswappi.backend.common.http.ErrorUtils;
import com.kirjaswappi.backend.common.service.AdminUserService;
import com.kirjaswappi.backend.common.service.entities.AdminUser;
import com.kirjaswappi.backend.common.service.enums.Role;
import com.kirjaswappi.backend.common.utils.JwtUtil;

@ExtendWith(MockitoExtension.class)
class FilterApiRequestTest {

  @Mock
  private AdminUserService adminUserService;
  @Mock
  private ErrorUtils errorUtils;
  @Mock
  private JwtUtil jwtUtil;
  @Mock
  private FilterChain filterChain;

  @InjectMocks
  private FilterApiRequest filterApiRequest;

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Should continue filter chain when no JWT token is present")
  void shouldContinueWhenNoToken() throws Exception {
    filterApiRequest.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  @DisplayName("Should set authentication when user token is valid")
  void shouldSetAuthWhenUserTokenIsValid() throws Exception {
    request.addHeader("Authorization", "Bearer valid-user-token");

    when(jwtUtil.isUserToken("valid-user-token")).thenReturn(true);
    when(jwtUtil.validateUserToken("valid-user-token")).thenReturn(true);
    when(jwtUtil.extractUserId("valid-user-token")).thenReturn("user-123");
    when(jwtUtil.extractRole("valid-user-token")).thenReturn("USER");

    filterApiRequest.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    var auth = SecurityContextHolder.getContext().getAuthentication();
    assertNotNull(auth);
    assertEquals("user-123", auth.getName());
    assertEquals("USER", auth.getAuthorities().iterator().next().getAuthority());
  }

  @Test
  @DisplayName("Should return 401 when user token is invalid")
  void shouldReturn401WhenUserTokenIsInvalid() throws Exception {
    request.addHeader("Authorization", "Bearer expired-user-token");

    when(jwtUtil.isUserToken("expired-user-token")).thenReturn(true);
    when(jwtUtil.validateUserToken("expired-user-token")).thenReturn(false);
    when(errorUtils.buildErrorResponse(any()))
        .thenReturn(new com.kirjaswappi.backend.common.http.ErrorResponse(
            new com.kirjaswappi.backend.common.http.ErrorResponse.Error("invalidJwtToken", "Invalid token")));

    filterApiRequest.doFilterInternal(request, response, filterChain);

    verify(filterChain, never()).doFilter(request, response);
    assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  @DisplayName("Should return 401 when admin token is invalid")
  void shouldReturn401WhenAdminTokenIsInvalid() throws Exception {
    request.addHeader("Authorization", "Bearer bad-admin-token");

    when(jwtUtil.isUserToken("bad-admin-token")).thenReturn(false);
    when(jwtUtil.extractUsername("bad-admin-token")).thenReturn("admin");
    when(adminUserService.getAdminUserInfo("admin"))
        .thenReturn(new AdminUser("admin", "pass", Role.ADMIN));
    when(jwtUtil.validateJwtToken(eq("bad-admin-token"), any())).thenReturn(false);
    when(errorUtils.buildErrorResponse(any()))
        .thenReturn(new com.kirjaswappi.backend.common.http.ErrorResponse(
            new com.kirjaswappi.backend.common.http.ErrorResponse.Error("invalidJwtToken", "Invalid token")));

    filterApiRequest.doFilterInternal(request, response, filterChain);

    verify(filterChain, never()).doFilter(request, response);
    assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
  }
}
