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
@RequestMapping(API_BASE + INBOX)
@Validated
public class InboxController {
  @Autowired
  private InboxService inboxService;

  @GetMapping
  @Operation(summary = "Get unified inbox", description = "Retrieve all swap requests for the user (both sent and received) in a unified inbox sorted by latest messages with optional filtering and sorting", responses = {
      @ApiResponse(responseCode = "200", description = "Unified inbox retrieved successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid status or sort parameter"),
      @ApiResponse(responseCode = "404", description = "User not found")
  })
  public ResponseEntity<List<InboxItemResponse>> getUnifiedInbox(
      @Parameter(description = "User ID", required = true) @RequestParam String userId,
      @Parameter(description = "Filter by status") @RequestParam(required = false) String status,
      @Parameter(description = "Sort by field (latest_message, date, book_title, sender_name, status)") @RequestParam(required = false) String sortBy) {

    List<SwapRequest> swapRequests = inboxService.getUnifiedInbox(userId, status, sortBy);
    List<InboxItemResponse> response = swapRequests.stream()
        .map(swapRequest -> {
          InboxItemResponse item = new InboxItemResponse(swapRequest);
          // Add unread message count using cached version
          long unreadCount = inboxService.getUnreadMessageCount(userId, swapRequest.id());
          item.setUnreadMessageCount(unreadCount);
          // Set notification indicators
          item.setUnread(inboxService.isInboxItemUnread(swapRequest, userId));
          item.setHasNewMessages(unreadCount > 0);
          // Set conversation type for UI display
          item.setConversationType(userId.equals(swapRequest.sender().id()) ? "sent" : "received");
          return item;
        })
        .toList();

    return ResponseEntity.ok(response);
  }
}
