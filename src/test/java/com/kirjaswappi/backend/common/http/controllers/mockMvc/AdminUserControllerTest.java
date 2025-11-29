/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.http.controllers.mockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirjaswappi.backend.common.http.controllers.AdminUserController;
import com.kirjaswappi.backend.common.http.controllers.mockMvc.config.CustomMockMvcConfiguration;
import com.kirjaswappi.backend.common.http.dtos.requests.AdminUserCreateRequest;
import com.kirjaswappi.backend.common.service.AdminUserService;
import com.kirjaswappi.backend.common.service.entities.AdminUser;
import com.kirjaswappi.backend.common.service.enums.Role;

@WebMvcTest(AdminUserController.class)
@Import(CustomMockMvcConfiguration.class)
class AdminUserControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AdminUserService adminUserService;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @DisplayName("Should create an admin user successfully")
  void shouldCreateAdminUserSuccessfully() throws Exception {
    var request = new AdminUserCreateRequest();
    request.setUsername("admin123");
    request.setPassword("securePass");
    request.setRole("Admin");

    var entity = new AdminUser();
    entity.setUsername("admin123");
    entity.setRole(Role.ADMIN);

    when(adminUserService.addUser(any(AdminUser.class))).thenReturn(entity);

    mockMvc.perform(post("/api/v1/admin-users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value("admin123"));
  }

  @Test
  @DisplayName("Should return list of admin users")
  void shouldReturnListOfAdminUsers() throws Exception {
    var admin1 = new AdminUser();
    admin1.setUsername("admin1");
    admin1.setRole(Role.ADMIN);

    var admin2 = new AdminUser();
    admin2.setUsername("admin2");
    admin2.setRole(Role.USER);

    when(adminUserService.getAdminUsers()).thenReturn(List.of(admin1, admin2));

    mockMvc.perform(get("/api/v1/admin-users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].username").value("admin1"))
        .andExpect(jsonPath("$[0].role").value("Admin"))
        .andExpect(jsonPath("$[1].username").value("admin2"))
        .andExpect(jsonPath("$[1].role").value("User"));
  }

  @Test
  @DisplayName("Should delete an admin user")
  void shouldDeleteAdminUser() throws Exception {
    doNothing().when(adminUserService).deleteUser("admin123");
    mockMvc.perform(delete("/api/v1/admin-users/admin123"))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("Should return 400 for invalid admin create request")
  void shouldReturn400ForInvalidAdminCreateRequest() throws Exception {
    var request = new AdminUserCreateRequest(); // No username or password
    mockMvc.perform(post("/api/v1/admin-users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }
}
