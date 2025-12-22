/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.http.controllers;

import static com.kirjaswappi.backend.common.utils.Constants.ADMIN_USERS;
import static com.kirjaswappi.backend.common.utils.Constants.API_BASE;
import static com.kirjaswappi.backend.common.utils.Constants.USERNAME;

import java.util.List;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kirjaswappi.backend.common.http.ErrorResponse;
import com.kirjaswappi.backend.common.http.dtos.requests.AdminUserCreateRequest;
import com.kirjaswappi.backend.common.http.dtos.responses.AdminUserResponse;
import com.kirjaswappi.backend.common.service.AdminUserService;
import com.kirjaswappi.backend.common.service.entities.AdminUser;

@RestController
@RequestMapping(API_BASE + ADMIN_USERS)
@Validated
@Tag(name = "Admin User Management", description = "APIs for managing admin users in the system")
public class AdminUserController {
  @Autowired
  private AdminUserService adminUserService;

  @PostMapping
  @Operation(summary = "Create a new admin user", description = "Creates a new admin user with the specified username, password, and role. The username must be unique.", responses = {
      @ApiResponse(responseCode = "201", description = "Admin user created successfully", content = @Content(schema = @Schema(implementation = AdminUserResponse.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request data or admin user already exists", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public ResponseEntity<AdminUserResponse> createAdminUser(@Valid @RequestBody AdminUserCreateRequest request) {
    AdminUser savedUser = adminUserService.addUser(request.toEntity());
    return ResponseEntity.status(HttpStatus.CREATED).body(new AdminUserResponse(savedUser));
  }

  @GetMapping
  @Operation(summary = "Get all admin users", description = "Retrieves a list of all admin users in the system.", responses = {
      @ApiResponse(responseCode = "200", description = "List of admin users retrieved successfully", content = @Content(schema = @Schema(implementation = AdminUserResponse[].class))),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public ResponseEntity<List<AdminUserResponse>> findAdminUsers() {
    List<AdminUserResponse> userListResponses = adminUserService.getAdminUsers()
        .stream().map(AdminUserResponse::new).toList();
    return ResponseEntity.status(HttpStatus.OK).body(userListResponses);
  }

  @DeleteMapping(USERNAME)
  @Operation(summary = "Delete an admin user", description = "Deletes the admin user with the specified username.", responses = {
      @ApiResponse(responseCode = "204", description = "Admin user deleted successfully"),
      @ApiResponse(responseCode = "404", description = "Admin user not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public ResponseEntity<Void> deleteAdminUser(
      @Parameter(description = "Username of the admin user to delete", required = true, example = "admin") @PathVariable String username) {
    adminUserService.deleteUser(username);
    return ResponseEntity.noContent().build();
  }
}
