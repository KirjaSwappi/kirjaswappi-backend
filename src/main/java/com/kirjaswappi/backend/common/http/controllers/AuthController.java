/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.http.controllers;

import static com.kirjaswappi.backend.common.utils.Constants.API_BASE;
import static com.kirjaswappi.backend.common.utils.Constants.AUTHENTICATE;
import static com.kirjaswappi.backend.common.utils.Constants.REFRESH;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kirjaswappi.backend.common.http.dtos.requests.AuthenticationRequest;
import com.kirjaswappi.backend.common.http.dtos.requests.RefreshAuthenticationRequest;
import com.kirjaswappi.backend.common.http.dtos.responses.AuthenticationResponse;
import com.kirjaswappi.backend.common.http.dtos.responses.RefreshAuthenticationResponse;
import com.kirjaswappi.backend.common.service.AuthService;
import com.kirjaswappi.backend.common.service.entities.AdminUser;

@RestController
@RequestMapping(API_BASE + AUTHENTICATE)
@Validated
@Tag(name = "Authentication", description = "APIs for admin user authentication and token management")
public class AuthController {
  @Autowired
  private AuthService authService;

  @PostMapping
  @Operation(summary = "Authenticate admin user", description = "Authenticates an admin user with username and password, returning JWT and refresh tokens.", responses = {
      @ApiResponse(responseCode = "200", description = "Authentication successful", content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request body"),
      @ApiResponse(responseCode = "401", description = "Invalid credentials"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public ResponseEntity<AuthenticationResponse> createAuthToken(@Valid @RequestBody AuthenticationRequest request) {
    AdminUser adminUser = authService.verifyLogin(request.toEntity());
    String jwtToken = authService.generateJwtToken(adminUser);
    String refreshToken = authService.generateRefreshToken(adminUser);
    return ResponseEntity.ok(new AuthenticationResponse(jwtToken, refreshToken));
  }

  @PostMapping(REFRESH)
  @Operation(summary = "Refresh authentication token", description = "Refreshes the JWT token using a valid refresh token.", responses = {
      @ApiResponse(responseCode = "200", description = "Token refreshed successfully", content = @Content(schema = @Schema(implementation = RefreshAuthenticationResponse.class))),
      @ApiResponse(responseCode = "400", description = "Invalid refresh token"),
      @ApiResponse(responseCode = "401", description = "Expired or invalid refresh token"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public ResponseEntity<RefreshAuthenticationResponse> refreshAuthToken(
      @Valid @RequestBody RefreshAuthenticationRequest request) {
    String jwtToken = authService.verifyRefreshToken(request.getRefreshToken());
    return ResponseEntity.ok(new RefreshAuthenticationResponse(jwtToken));
  }
}
