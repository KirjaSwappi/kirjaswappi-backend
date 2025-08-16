/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.controllers;

import static com.kirjaswappi.backend.common.utils.Constants.*;

import java.util.List;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.kirjaswappi.backend.http.dtos.requests.SendMessageRequest;
import com.kirjaswappi.backend.http.dtos.responses.ChatMessageResponse;
import com.kirjaswappi.backend.service.ChatService;
import com.kirjaswappi.backend.service.entities.ChatMessage;

@RestController
@RequestMapping(API_BASE + SWAP_REQUESTS)
@Validated
public class ChatController {
  @Autowired
  private ChatService chatService;

  @GetMapping(ID + CHAT)
  @Operation(summary = "Get chat messages for a swap request", description = "Retrieve all chat messages for a specific swap request. User must be sender or receiver. Automatically marks messages as read.", responses = {
      @ApiResponse(responseCode = "200", description = "Chat messages retrieved successfully"),
      @ApiResponse(responseCode = "403", description = "Access denied - user not authorized to view this chat"),
      @ApiResponse(responseCode = "404", description = "Swap request not found")
  })
  public ResponseEntity<List<ChatMessageResponse>> getChatMessages(
      @Parameter(description = "Swap request ID", required = true) @PathVariable String id,
      @Parameter(description = "User ID", required = true) @RequestParam String userId) {

    // Mark messages as read BEFORE getting them to ensure cache is cleared
    chatService.markMessagesAsRead(id, userId);

    List<ChatMessage> messages = chatService.getChatMessages(id, userId);

    List<ChatMessageResponse> response = messages.stream()
        .map(message -> new ChatMessageResponse(message, userId))
        .toList();

    return ResponseEntity.ok(response);
  }

  @PostMapping(ID + CHAT)
  @Operation(summary = "Send a chat message", description = "Send a new message in the chat for a specific swap request. User must be sender or receiver.", responses = {
      @ApiResponse(responseCode = "201", description = "Message sent successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid message content"),
      @ApiResponse(responseCode = "403", description = "Access denied - user not authorized to send messages in this chat"),
      @ApiResponse(responseCode = "404", description = "Swap request not found")
  })
  public ResponseEntity<ChatMessageResponse> sendMessage(
      @Parameter(description = "Swap request ID", required = true) @PathVariable String id,
      @Parameter(description = "User ID", required = true) @RequestParam String userId,
      @Valid @RequestBody SendMessageRequest request) {

    ChatMessage message = chatService.sendMessage(id, userId, request.getMessage());
    ChatMessageResponse response = new ChatMessageResponse(message, userId);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PutMapping(ID + CHAT + "/mark-read")
  @Operation(summary = "Mark messages as read", description = "Mark all unread messages in the chat as read for the current user.", responses = {
      @ApiResponse(responseCode = "204", description = "Messages marked as read successfully"),
      @ApiResponse(responseCode = "403", description = "Access denied - user not authorized to access this chat"),
      @ApiResponse(responseCode = "404", description = "Swap request not found")
  })
  public ResponseEntity<Void> markMessagesAsRead(
      @Parameter(description = "Swap request ID", required = true) @PathVariable String id,
      @Parameter(description = "User ID", required = true) @RequestParam String userId) {

    chatService.markMessagesAsRead(id, userId);

    return ResponseEntity.noContent().build();
  }
}