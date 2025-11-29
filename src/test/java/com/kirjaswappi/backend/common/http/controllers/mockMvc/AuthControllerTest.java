/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.http.controllers.mockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirjaswappi.backend.common.http.controllers.AuthController;
import com.kirjaswappi.backend.common.http.controllers.mockMvc.config.CustomMockMvcConfiguration;
import com.kirjaswappi.backend.common.http.dtos.requests.AuthenticationRequest;
import com.kirjaswappi.backend.common.http.dtos.requests.RefreshAuthenticationRequest;
import com.kirjaswappi.backend.common.service.AuthService;
import com.kirjaswappi.backend.common.service.entities.AdminUser;

@WebMvcTest(AuthController.class)
@Import(CustomMockMvcConfiguration.class)
class AuthControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AuthService authService;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @DisplayName("Should authenticate and return JWT and refresh token")
  void shouldAuthenticateAndReturnTokens() throws Exception {
    AuthenticationRequest request = new AuthenticationRequest();
    request.setUsername("admin");
    request.setPassword("password");

    AdminUser adminUser = new AdminUser();
    adminUser.setUsername("admin");

    when(authService.verifyLogin(any())).thenReturn(adminUser);
    when(authService.generateJwtToken(adminUser)).thenReturn("jwt-token");
    when(authService.generateRefreshToken(adminUser)).thenReturn("refresh-token");

    mockMvc.perform(post("/api/v1/authenticate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.jwtToken").value("jwt-token"))
        .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
  }

  @Test
  @DisplayName("Should refresh token and return new JWT")
  void shouldRefreshJwtToken() throws Exception {
    RefreshAuthenticationRequest request = new RefreshAuthenticationRequest();
    request.setRefreshToken("refresh-token");

    when(authService.verifyRefreshToken("refresh-token")).thenReturn("new-jwt-token");

    mockMvc.perform(post("/api/v1/authenticate/refresh")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.jwtToken").value("new-jwt-token"));
  }
}
