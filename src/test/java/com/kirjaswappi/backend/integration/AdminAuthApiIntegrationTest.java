/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.integration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirjaswappi.backend.common.http.controllers.AdminUserController;
import com.kirjaswappi.backend.common.http.controllers.AuthController;
import com.kirjaswappi.backend.common.http.controllers.mockMvc.config.CustomMockMvcConfiguration;
import com.kirjaswappi.backend.common.http.dtos.requests.AdminUserCreateRequest;
import com.kirjaswappi.backend.common.http.dtos.requests.AuthenticationRequest;
import com.kirjaswappi.backend.common.http.dtos.requests.RefreshAuthenticationRequest;
import com.kirjaswappi.backend.common.service.AdminUserService;
import com.kirjaswappi.backend.common.service.AuthService;
import com.kirjaswappi.backend.common.service.entities.AdminUser;
import com.kirjaswappi.backend.common.service.enums.Role;
import com.kirjaswappi.backend.common.service.exceptions.InvalidCredentials;
import com.kirjaswappi.backend.service.exceptions.ResourceNotFoundException;

/**
 * Unit tests for Admin User and Authentication API endpoints using WebMvcTest.
 * Tests admin user management and JWT token authentication flow.
 */
@WebMvcTest({ AdminUserController.class, AuthController.class })
@Import(CustomMockMvcConfiguration.class)
class AdminAuthApiIntegrationTest {

  private static final String ADMIN_USERS_API = "/api/v1/admin-users";
  private static final String AUTH_API = "/api/v1/authenticate";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AdminUserService adminUserService;

  @MockitoBean
  private AuthService authService;

  @Autowired
  private ObjectMapper objectMapper;

  private AdminUser createTestAdminUser(String username, Role role) {
    return AdminUser.builder()
        .username(username)
        .password("hashedpassword")
        .role(role)
        .build();
  }

  @Nested
  @DisplayName("Admin User Management Tests")
  class AdminUserManagementTests {

    @Test
    @DisplayName("Should create admin user successfully")
    void shouldCreateAdminUserSuccessfully() throws Exception {
      AdminUser adminUser = createTestAdminUser("newadmin", Role.ADMIN);
      when(adminUserService.addUser(any())).thenReturn(adminUser);

      AdminUserCreateRequest request = new AdminUserCreateRequest();
      request.setUsername("newadmin");
      request.setPassword("SecureP@ss123");
      request.setRole("Admin");

      mockMvc.perform(post(ADMIN_USERS_API)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.username").value("newadmin"))
          .andExpect(jsonPath("$.role").value("Admin"));
    }

    @Test
    @DisplayName("Should create admin user with User role")
    void shouldCreateAdminUserWithUserRole() throws Exception {
      AdminUser adminUser = createTestAdminUser("regularuser", Role.USER);
      when(adminUserService.addUser(any())).thenReturn(adminUser);

      AdminUserCreateRequest request = new AdminUserCreateRequest();
      request.setUsername("regularuser");
      request.setPassword("SecureP@ss123");
      request.setRole("User");

      mockMvc.perform(post(ADMIN_USERS_API)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.username").value("regularuser"))
          .andExpect(jsonPath("$.role").value("User"));
    }

    @Test
    @DisplayName("Should return 400 when username is missing")
    void shouldReturn400WhenUsernameMissing() throws Exception {
      AdminUserCreateRequest request = new AdminUserCreateRequest();
      request.setPassword("SecureP@ss123");
      request.setRole("Admin");

      mockMvc.perform(post(ADMIN_USERS_API)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when password is missing")
    void shouldReturn400WhenPasswordMissing() throws Exception {
      AdminUserCreateRequest request = new AdminUserCreateRequest();
      request.setUsername("admin");
      request.setRole("Admin");

      mockMvc.perform(post(ADMIN_USERS_API)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when role is missing")
    void shouldReturn400WhenRoleMissing() throws Exception {
      AdminUserCreateRequest request = new AdminUserCreateRequest();
      request.setUsername("admin");
      request.setPassword("SecureP@ss123");

      mockMvc.perform(post(ADMIN_USERS_API)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when all fields are missing")
    void shouldReturn400WhenAllFieldsMissing() throws Exception {
      AdminUserCreateRequest request = new AdminUserCreateRequest();

      mockMvc.perform(post(ADMIN_USERS_API)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return all admin users")
    void shouldReturnAllAdminUsers() throws Exception {
      List<AdminUser> users = List.of(
          createTestAdminUser("admin1", Role.ADMIN),
          createTestAdminUser("admin2", Role.USER),
          createTestAdminUser("admin3", Role.ADMIN));
      when(adminUserService.getAdminUsers()).thenReturn(users);

      mockMvc.perform(get(ADMIN_USERS_API))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("Should return empty list when no admin users exist")
    void shouldReturnEmptyListWhenNoAdminUsers() throws Exception {
      when(adminUserService.getAdminUsers()).thenReturn(List.of());

      mockMvc.perform(get(ADMIN_USERS_API))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Should delete admin user successfully")
    void shouldDeleteAdminUserSuccessfully() throws Exception {
      doNothing().when(adminUserService).deleteUser("deleteadmin");

      mockMvc.perform(delete(ADMIN_USERS_API + "/deleteadmin"))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent admin user")
    void shouldReturn404WhenDeletingNonExistentUser() throws Exception {
      doThrow(new ResourceNotFoundException("adminUserNotFound", "nonexistent"))
          .when(adminUserService).deleteUser("nonexistent");

      mockMvc.perform(delete(ADMIN_USERS_API + "/nonexistent"))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("Authentication Tests")
  class AuthenticationTests {

    @Test
    @DisplayName("Should authenticate admin user and return tokens")
    void shouldAuthenticateAndReturnTokens() throws Exception {
      AdminUser adminUser = createTestAdminUser("authuser", Role.ADMIN);
      when(authService.verifyLogin(any())).thenReturn(adminUser);
      when(authService.generateJwtToken(any())).thenReturn("jwt-token");
      when(authService.generateRefreshToken(any())).thenReturn("refresh-token");

      AuthenticationRequest authRequest = new AuthenticationRequest();
      authRequest.setUsername("authuser");
      authRequest.setPassword("SecureP@ss123");

      mockMvc.perform(post(AUTH_API)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(authRequest)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.jwtToken").exists())
          .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    @DisplayName("Should return 401 when password is incorrect")
    void shouldReturn401WhenPasswordIncorrect() throws Exception {
      when(authService.verifyLogin(any())).thenThrow(new InvalidCredentials());

      AuthenticationRequest authRequest = new AuthenticationRequest();
      authRequest.setUsername("wrongpassuser");
      authRequest.setPassword("WrongPassword123");

      mockMvc.perform(post(AUTH_API)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(authRequest)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 404 when username not found")
    void shouldReturn404WhenUsernameNotFound() throws Exception {
      when(authService.verifyLogin(any()))
          .thenThrow(new ResourceNotFoundException("adminUserNotFound", "nonexistent"));

      AuthenticationRequest authRequest = new AuthenticationRequest();
      authRequest.setUsername("nonexistent");
      authRequest.setPassword("SomePassword123");

      mockMvc.perform(post(AUTH_API)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(authRequest)))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 when username is missing")
    void shouldReturn400WhenUsernameMissingInAuth() throws Exception {
      AuthenticationRequest authRequest = new AuthenticationRequest();
      authRequest.setPassword("SomePassword123");

      mockMvc.perform(post(AUTH_API)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(authRequest)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when password is missing in auth")
    void shouldReturn400WhenPasswordMissingInAuth() throws Exception {
      AuthenticationRequest authRequest = new AuthenticationRequest();
      authRequest.setUsername("someuser");

      mockMvc.perform(post(AUTH_API)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(authRequest)))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("Token Refresh Tests")
  class TokenRefreshTests {

    @Test
    @DisplayName("Should refresh token successfully")
    void shouldRefreshTokenSuccessfully() throws Exception {
      when(authService.verifyRefreshToken("valid-refresh-token")).thenReturn("new-jwt-token");

      RefreshAuthenticationRequest refreshRequest = new RefreshAuthenticationRequest();
      refreshRequest.setRefreshToken("valid-refresh-token");

      mockMvc.perform(post(AUTH_API + "/refresh")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(refreshRequest)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.jwtToken").value("new-jwt-token"));
    }

    @Test
    @DisplayName("Should return 401 when refresh token is invalid")
    void shouldReturn401WhenRefreshTokenInvalid() throws Exception {
      when(authService.verifyRefreshToken("invalid-refresh-token"))
          .thenThrow(new InvalidCredentials());

      RefreshAuthenticationRequest refreshRequest = new RefreshAuthenticationRequest();
      refreshRequest.setRefreshToken("invalid-refresh-token");

      mockMvc.perform(post(AUTH_API + "/refresh")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(refreshRequest)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should accept request when refresh token is empty string")
    void shouldReturn400WhenRefreshTokenMissing() throws Exception {
      // Empty refresh token will still be sent to the service
      when(authService.verifyRefreshToken("")).thenThrow(new InvalidCredentials());

      RefreshAuthenticationRequest refreshRequest = new RefreshAuthenticationRequest();
      refreshRequest.setRefreshToken("");

      mockMvc.perform(post(AUTH_API + "/refresh")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(refreshRequest)))
          .andExpect(status().isUnauthorized());
    }
  }
}
