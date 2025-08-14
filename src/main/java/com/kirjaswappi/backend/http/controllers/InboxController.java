/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.controllers;

import static com.kirjaswappi.backend.common.utils.Constants.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.kirjaswappi.backend.http.dtos.responses.InboxItemResponse;
import com.kirjaswappi.backend.service.InboxService;
import com.kirjaswappi.backend.service.entities.SwapRequest;

@RestController
@RequestMapping(API_BASE + "/inbox")
@Validated
public class InboxController {
  @Autowired
  private InboxService inboxService;

  @GetMapping("/received")
  @Operation(summary = "Get received swap requests", description = "Retrieve all swap requests received by the user with optional filtering and sorting", responses = {
      @ApiResponse(responseCode = "200", description = "Received swap requests retrieved successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid status or sort parameter"),
      @ApiResponse(responseCode = "404", description = "User not found")
  })
  public ResponseEntity<List<InboxItemResponse>> getReceivedSwapRequests(
      @Parameter(description = "User ID", required = true) @RequestParam String userId,
      @Parameter(description = "Filter by status", required = false) @RequestParam(required = false) String status,
      @Parameter(description = "Sort by field (date, book_title, sender_name, status)", required = false) @RequestParam(required = false) String sortBy) {

    List<SwapRequest> swapRequests = inboxService.getReceivedSwapRequests(userId, status, sortBy);
    List<InboxItemResponse> response = swapRequests.stream()
        .map(swapRequest -> {
          InboxItemResponse item = new InboxItemResponse(swapRequest);
          // Add unread message count using cached version
          long unreadCount = inboxService.getUnreadMessageCount(userId, swapRequest.getId());
          item.setUnreadMessageCount(unreadCount);
          // Set notification indicators
          item.setUnread(inboxService.isInboxItemUnread(swapRequest, userId));
          item.setHasNewMessages(unreadCount > 0);
          // Mark inbox item as read when viewed
          inboxService.markInboxItemAsRead(swapRequest.getId(), userId);
          return item;
        })
        .toList();

    return ResponseEntity.ok(response);
  }

  @GetMapping("/sent")
  @Operation(summary = "Get sent swap requests", description = "Retrieve all swap requests sent by the user with optional filtering and sorting", responses = {
      @ApiResponse(responseCode = "200", description = "Sent swap requests retrieved successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid status or sort parameter"),
      @ApiResponse(responseCode = "404", description = "User not found")
  })
  public ResponseEntity<List<InboxItemResponse>> getSentSwapRequests(
      @Parameter(description = "User ID", required = true) @RequestParam String userId,
      @Parameter(description = "Filter by status", required = false) @RequestParam(required = false) String status,
      @Parameter(description = "Sort by field (date, book_title, sender_name, status)", required = false) @RequestParam(required = false) String sortBy) {

    List<SwapRequest> swapRequests = inboxService.getSentSwapRequests(userId, status, sortBy);
    List<InboxItemResponse> response = swapRequests.stream()
        .map(swapRequest -> {
          InboxItemResponse item = new InboxItemResponse(swapRequest);
          // Add unread message count using cached version
          long unreadCount = inboxService.getUnreadMessageCount(userId, swapRequest.getId());
          item.setUnreadMessageCount(unreadCount);
          // Set notification indicators
          item.setUnread(inboxService.isInboxItemUnread(swapRequest, userId));
          item.setHasNewMessages(unreadCount > 0);
          // Mark inbox item as read when viewed
          inboxService.markInboxItemAsRead(swapRequest.getId(), userId);
          return item;
        })
        .toList();

    return ResponseEntity.ok(response);
  }
}