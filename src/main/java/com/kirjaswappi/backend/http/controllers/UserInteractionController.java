/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.controllers;

import static com.kirjaswappi.backend.common.utils.Constants.*;

import java.security.Principal;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.kirjaswappi.backend.http.dtos.requests.CreateReportRequest;
import com.kirjaswappi.backend.http.dtos.responses.ReportResponse;
import com.kirjaswappi.backend.service.ReportService;
import com.kirjaswappi.backend.service.UserService;
import com.kirjaswappi.backend.service.entities.Report;

@RestController
@Validated
@Tag(name = "User Interactions", description = "API for block, mute, and report functionality")
public class UserInteractionController {
  @Autowired
  private UserService userService;

  @Autowired
  private ReportService reportService;

  @PostMapping(API_BASE + USERS + ID + BLOCK)
  @Operation(summary = "Block a user.", responses = {
      @ApiResponse(responseCode = "204", description = "User blocked."),
      @ApiResponse(responseCode = "401", description = "Unauthorized.") })
  public ResponseEntity<Void> blockUser(
      @Parameter(description = "Target user ID to block.") @PathVariable String id,
      Principal principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    userService.blockUser(principal.getName(), id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping(API_BASE + USERS + ID + BLOCK)
  @Operation(summary = "Unblock a user.", responses = {
      @ApiResponse(responseCode = "204", description = "User unblocked."),
      @ApiResponse(responseCode = "401", description = "Unauthorized.") })
  public ResponseEntity<Void> unblockUser(
      @Parameter(description = "Target user ID to unblock.") @PathVariable String id,
      Principal principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    userService.unblockUser(principal.getName(), id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping(API_BASE + USERS + ID + MUTE)
  @Operation(summary = "Mute a user.", responses = {
      @ApiResponse(responseCode = "204", description = "User muted."),
      @ApiResponse(responseCode = "401", description = "Unauthorized.") })
  public ResponseEntity<Void> muteUser(
      @Parameter(description = "Target user ID to mute.") @PathVariable String id,
      Principal principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    userService.muteUser(principal.getName(), id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping(API_BASE + USERS + ID + MUTE)
  @Operation(summary = "Unmute a user.", responses = {
      @ApiResponse(responseCode = "204", description = "User unmuted."),
      @ApiResponse(responseCode = "401", description = "Unauthorized.") })
  public ResponseEntity<Void> unmuteUser(
      @Parameter(description = "Target user ID to unmute.") @PathVariable String id,
      Principal principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    userService.unmuteUser(principal.getName(), id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping(API_BASE + REPORTS)
  @Operation(summary = "Report a user.", responses = {
      @ApiResponse(responseCode = "201", description = "Report created."),
      @ApiResponse(responseCode = "401", description = "Unauthorized.") })
  public ResponseEntity<ReportResponse> createReport(
      @Valid @RequestBody CreateReportRequest request,
      Principal principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    request.validate();
    Report report = reportService.createReport(principal.getName(), request.getReportedUserId(), request.getReason());
    return ResponseEntity.status(HttpStatus.CREATED).body(new ReportResponse(report));
  }
}
