/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.integration;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirjaswappi.backend.common.http.controllers.mockMvc.config.CustomMockMvcConfiguration;
import com.kirjaswappi.backend.http.controllers.UserInteractionController;
import com.kirjaswappi.backend.http.dtos.requests.CreateReportRequest;
import com.kirjaswappi.backend.service.ReportService;
import com.kirjaswappi.backend.service.UserService;
import com.kirjaswappi.backend.service.entities.Report;
import com.kirjaswappi.backend.service.exceptions.UserNotFoundException;

@WebMvcTest(UserInteractionController.class)
@Import(CustomMockMvcConfiguration.class)
class UserInteractionApiIntegrationTest {

  private static final String USERS_PATH = "/api/v1/users";
  private static final String REPORTS_PATH = "/api/v1/reports";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private ReportService reportService;

  @Nested
  @DisplayName("Block/Unblock Tests")
  class BlockTests {

    @Test
    @DisplayName("Should block user successfully")
    void shouldBlockUserSuccessfully() throws Exception {
      doNothing().when(userService).blockUser("user-1", "user-2");

      mockMvc.perform(post(USERS_PATH + "/user-2/block")
          .with(withUser("user-1")))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 401 when blocking without auth")
    void shouldReturn401WhenBlockingWithoutAuth() throws Exception {
      mockMvc.perform(post(USERS_PATH + "/user-2/block"))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 404 when blocking non-existent user")
    void shouldReturn404WhenBlockingNonExistentUser() throws Exception {
      org.mockito.Mockito.doThrow(new UserNotFoundException())
          .when(userService).blockUser("user-1", "nonexistent");

      mockMvc.perform(post(USERS_PATH + "/nonexistent/block")
          .with(withUser("user-1")))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should unblock user successfully")
    void shouldUnblockUserSuccessfully() throws Exception {
      doNothing().when(userService).unblockUser("user-1", "user-2");

      mockMvc.perform(delete(USERS_PATH + "/user-2/block")
          .with(withUser("user-1")))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 401 when unblocking without auth")
    void shouldReturn401WhenUnblockingWithoutAuth() throws Exception {
      mockMvc.perform(delete(USERS_PATH + "/user-2/block"))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("Mute/Unmute Tests")
  class MuteTests {

    @Test
    @DisplayName("Should mute user successfully")
    void shouldMuteUserSuccessfully() throws Exception {
      doNothing().when(userService).muteUser("user-1", "user-2");

      mockMvc.perform(post(USERS_PATH + "/user-2/mute")
          .with(withUser("user-1")))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 401 when muting without auth")
    void shouldReturn401WhenMutingWithoutAuth() throws Exception {
      mockMvc.perform(post(USERS_PATH + "/user-2/mute"))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 404 when muting non-existent user")
    void shouldReturn404WhenMutingNonExistentUser() throws Exception {
      org.mockito.Mockito.doThrow(new UserNotFoundException())
          .when(userService).muteUser("user-1", "nonexistent");

      mockMvc.perform(post(USERS_PATH + "/nonexistent/mute")
          .with(withUser("user-1")))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should unmute user successfully")
    void shouldUnmuteUserSuccessfully() throws Exception {
      doNothing().when(userService).unmuteUser("user-1", "user-2");

      mockMvc.perform(delete(USERS_PATH + "/user-2/mute")
          .with(withUser("user-1")))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 401 when unmuting without auth")
    void shouldReturn401WhenUnmutingWithoutAuth() throws Exception {
      mockMvc.perform(delete(USERS_PATH + "/user-2/mute"))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("Report Tests")
  class ReportTests {

    @Test
    @DisplayName("Should create report successfully")
    void shouldCreateReportSuccessfully() throws Exception {
      Report report = Report.builder()
          .id("report-1")
          .reporterUserId("user-1")
          .reportedUserId("user-2")
          .reason("Spam")
          .createdAt(Instant.parse("2025-06-01T10:00:00Z"))
          .build();

      when(reportService.createReport("user-1", "user-2", "Spam")).thenReturn(report);

      CreateReportRequest request = new CreateReportRequest();
      request.setReportedUserId("user-2");
      request.setReason("Spam");

      mockMvc.perform(post(REPORTS_PATH)
          .with(withUser("user-1"))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").value("report-1"))
          .andExpect(jsonPath("$.reportedUserId").value("user-2"))
          .andExpect(jsonPath("$.reason").value("Spam"));
    }

    @Test
    @DisplayName("Should return 401 when reporting without auth")
    void shouldReturn401WhenReportingWithoutAuth() throws Exception {
      CreateReportRequest request = new CreateReportRequest();
      request.setReportedUserId("user-2");
      request.setReason("Spam");

      mockMvc.perform(post(REPORTS_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 400 when reported user ID is missing")
    void shouldReturn400WhenReportedUserIdMissing() throws Exception {
      CreateReportRequest request = new CreateReportRequest();
      request.setReason("Spam");

      mockMvc.perform(post(REPORTS_PATH)
          .with(withUser("user-1"))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when reason is missing")
    void shouldReturn400WhenReasonMissing() throws Exception {
      CreateReportRequest request = new CreateReportRequest();
      request.setReportedUserId("user-2");

      mockMvc.perform(post(REPORTS_PATH)
          .with(withUser("user-1"))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }

  private static RequestPostProcessor withUser(String userId) {
    return request -> {
      request.setUserPrincipal(() -> userId);
      return request;
    };
  }
}
