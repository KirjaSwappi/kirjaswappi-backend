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
import com.kirjaswappi.backend.service.entities.SwapRequest;

@RestController
@RequestMapping(API_BASE + SWAP_REQUESTS)
@Validated
public class ChatController {
  @Autowired
  private ChatService chatService;

  @GetMapping(ID + CHAT)
  @Operation(summary = "Get chat messages for a swap request", description = "Retrieve all chat messages for a specific swap request with book swap context. User must be sender or receiver. Automatically marks messages as read.", responses = {
      @ApiResponse(responseCode = "200", description = "Chat messages retrieved successfully"),
      @ApiResponse(responseCode = "403", description = "Access denied - user not authorized to view this chat"),
      @ApiResponse(responseCode = "404", description = "Swap request not found")
  })
  public ResponseEntity<List<ChatMessageResponse>> getChatMessages(
      @Parameter(description = "Swap request ID", required = true) @PathVariable String id,
      @Parameter(description = "User ID", required = true) @RequestParam String userId) {

    List<ChatMessage> messages = chatService.getChatMessages(id, userId);

    // Get swap request details for context
    SwapRequest swapRequest = chatService.getSwapRequestForChat(id, userId);

    // Create swap context
    ChatMessageResponse.SwapContextResponse swapContext = createSwapContext(swapRequest);

    List<ChatMessageResponse> response = messages.stream()
        .map(message -> {
          ChatMessageResponse chatResponse = new ChatMessageResponse(message, userId);
          // Include swap context only in the first message for efficiency
          if (messages.indexOf(message) == 0) {
            chatResponse.setSwapContext(swapContext);
          }
          return chatResponse;
        })
        .toList();

    return ResponseEntity.ok(response);
  }

  @PostMapping(ID + CHAT)
  @Operation(summary = "Send a chat message", description = "Send a new message in the chat for a specific swap request with book swap context. User must be sender or receiver. Supports text message, images, or both.", responses = {
      @ApiResponse(responseCode = "201", description = "Message sent successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid message content or images"),
      @ApiResponse(responseCode = "403", description = "Access denied - user not authorized to send messages in this chat"),
      @ApiResponse(responseCode = "404", description = "Swap request not found")
  })
  public ResponseEntity<ChatMessageResponse> sendMessage(
      @Parameter(description = "Swap request ID", required = true) @PathVariable String id,
      @Parameter(description = "User ID", required = true) @RequestParam String userId,
      @Valid @ModelAttribute SendMessageRequest request) {

    // Validate that either message or images are provided
    if (!request.isValid()) {
      return ResponseEntity.badRequest().build();
    }

    ChatMessage message = chatService.sendMessage(id, userId, request.getMessage(), request.getImages());

    // Get swap request details for context
    SwapRequest swapRequest = chatService.getSwapRequestForChat(id, userId);

    ChatMessageResponse response = new ChatMessageResponse(message, userId);
    response.setSwapContext(createSwapContext(swapRequest));

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  private ChatMessageResponse.SwapContextResponse createSwapContext(SwapRequest swapRequest) {
    ChatMessageResponse.SwapContextResponse context = new ChatMessageResponse.SwapContextResponse();

    // Set basic swap information
    context.setSwapType(swapRequest.getSwapType().getCode());
    context.setSwapStatus(swapRequest.getSwapStatus().getCode());
    context.setAskForGiveaway(swapRequest.isAskForGiveaway());

    // Set requested book information
    context.setRequestedBook(
        new ChatMessageResponse.SwapContextResponse.BookInfoResponse(swapRequest.getBookToSwapWith()));

    // Set offered book/genre information if available
    if (swapRequest.getSwapOffer() != null) {
      if (swapRequest.getSwapOffer().getOfferedBook() != null) {
        context.setOfferedBook(
            new ChatMessageResponse.SwapContextResponse.BookInfoResponse(swapRequest.getSwapOffer().getOfferedBook()));
      }
      if (swapRequest.getSwapOffer().getOfferedGenre() != null) {
        context.setOfferedGenreName(swapRequest.getSwapOffer().getOfferedGenre().getName());
      }
    }

    return context;
  }
}
