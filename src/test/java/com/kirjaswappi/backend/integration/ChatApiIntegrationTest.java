/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.integration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kirjaswappi.backend.common.http.controllers.mockMvc.config.CustomMockMvcConfiguration;
import com.kirjaswappi.backend.http.controllers.ChatController;
import com.kirjaswappi.backend.service.ChatService;
import com.kirjaswappi.backend.service.entities.*;
import com.kirjaswappi.backend.service.enums.*;
import com.kirjaswappi.backend.service.exceptions.ResourceNotFoundException;

/**
 * Integration tests for Chat API endpoints. Tests chat message retrieval and
 * sending with swap context.
 */
@WebMvcTest(ChatController.class)
@Import(CustomMockMvcConfiguration.class)
class ChatApiIntegrationTest {

  private static final String API_BASE = "/api/v1/swap-requests";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ChatService chatService;

  private User createTestUser(String id, String firstName, String lastName, String email) {
    return User.builder()
        .id(id)
        .firstName(firstName)
        .lastName(lastName)
        .email(email)
        .build();
  }

  private Book createTestBook(String id, String title, User owner) {
    return Book.builder()
        .id(id)
        .title(title)
        .author("Test Author")
        .language(Language.ENGLISH)
        .condition(Condition.GOOD)
        .owner(owner)
        .coverPhotos(List.of())
        .genres(List.of())
        .build();
  }

  private SwapRequest createTestSwapRequest(String id, User sender, User receiver, Book book) {
    return SwapRequest.builder()
        .id(id)
        .sender(sender)
        .receiver(receiver)
        .bookToSwapWith(book)
        .swapType(SwapType.GIVE_AWAY)
        .swapStatus(SwapStatus.PENDING)
        .askForGiveaway(false)
        .note("Test note")
        .requestedAt(Instant.now())
        .build();
  }

  private ChatMessage createTestChatMessage(String id, User sender, String message) {
    return ChatMessage.builder()
        .id(id)
        .sender(sender)
        .message(message)
        .sentAt(Instant.now())
        .readByReceiver(false)
        .build();
  }

  @Nested
  @DisplayName("Get Chat Messages Tests")
  class GetChatMessagesTests {

    @Test
    @DisplayName("Should return chat messages successfully")
    void shouldReturnChatMessagesSuccessfully() throws Exception {
      String swapRequestId = "swap-1";
      String userId = "user-1";

      User sender = createTestUser(userId, "John", "Doe", "john@test.com");
      User receiver = createTestUser("user-2", "Jane", "Doe", "jane@test.com");
      Book book = createTestBook("book-1", "Test Book", receiver);
      SwapRequest swapRequest = createTestSwapRequest(swapRequestId, sender, receiver, book);

      List<ChatMessage> messages = List.of(
          createTestChatMessage("msg-1", sender, "Hello!"),
          createTestChatMessage("msg-2", receiver, "Hi there!"));

      when(chatService.getChatMessages(swapRequestId, userId)).thenReturn(messages);
      when(chatService.getSwapRequestForChat(swapRequestId, userId)).thenReturn(swapRequest);

      mockMvc.perform(get(API_BASE + "/" + swapRequestId + "/chat")
          .param("userId", userId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].message").value("Hello!"))
          .andExpect(jsonPath("$[1].message").value("Hi there!"));
    }

    @Test
    @DisplayName("Should return empty list when no messages")
    void shouldReturnEmptyListWhenNoMessages() throws Exception {
      String swapRequestId = "swap-1";
      String userId = "user-1";

      User sender = createTestUser(userId, "John", "Doe", "john@test.com");
      User receiver = createTestUser("user-2", "Jane", "Doe", "jane@test.com");
      Book book = createTestBook("book-1", "Test Book", receiver);
      SwapRequest swapRequest = createTestSwapRequest(swapRequestId, sender, receiver, book);

      when(chatService.getChatMessages(swapRequestId, userId)).thenReturn(List.of());
      when(chatService.getSwapRequestForChat(swapRequestId, userId)).thenReturn(swapRequest);

      mockMvc.perform(get(API_BASE + "/" + swapRequestId + "/chat")
          .param("userId", userId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Should return 404 when swap request not found")
    void shouldReturn404WhenSwapRequestNotFound() throws Exception {
      String swapRequestId = "nonexistent";
      String userId = "user-1";

      when(chatService.getChatMessages(swapRequestId, userId))
          .thenThrow(new ResourceNotFoundException("swapRequestNotFound", swapRequestId));

      mockMvc.perform(get(API_BASE + "/" + swapRequestId + "/chat")
          .param("userId", userId))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 when userId is missing")
    void shouldReturn400WhenUserIdMissing() throws Exception {
      mockMvc.perform(get(API_BASE + "/swap-1/chat"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should include swap context in first message response")
    void shouldIncludeSwapContextInResponse() throws Exception {
      String swapRequestId = "swap-1";
      String userId = "user-1";

      User sender = createTestUser(userId, "John", "Doe", "john@test.com");
      User receiver = createTestUser("user-2", "Jane", "Doe", "jane@test.com");
      Book book = createTestBook("book-1", "Test Book", receiver);
      SwapRequest swapRequest = createTestSwapRequest(swapRequestId, sender, receiver, book);

      List<ChatMessage> messages = List.of(createTestChatMessage("msg-1", sender, "Hello!"));

      when(chatService.getChatMessages(swapRequestId, userId)).thenReturn(messages);
      when(chatService.getSwapRequestForChat(swapRequestId, userId)).thenReturn(swapRequest);

      mockMvc.perform(get(API_BASE + "/" + swapRequestId + "/chat")
          .param("userId", userId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].swapContext").exists())
          .andExpect(jsonPath("$[0].swapContext.swapType").value("GiveAway"))
          .andExpect(jsonPath("$[0].swapContext.swapStatus").value("Pending"));
    }

    @Test
    @DisplayName("Should set isOwnMessage correctly")
    void shouldSetIsOwnMessageCorrectly() throws Exception {
      String swapRequestId = "swap-1";
      String userId = "user-1";

      User sender = createTestUser(userId, "John", "Doe", "john@test.com");
      User receiver = createTestUser("user-2", "Jane", "Doe", "jane@test.com");
      Book book = createTestBook("book-1", "Test Book", receiver);
      SwapRequest swapRequest = createTestSwapRequest(swapRequestId, sender, receiver, book);

      List<ChatMessage> messages = List.of(
          createTestChatMessage("msg-1", sender, "My message"),
          createTestChatMessage("msg-2", receiver, "Their message"));

      when(chatService.getChatMessages(swapRequestId, userId)).thenReturn(messages);
      when(chatService.getSwapRequestForChat(swapRequestId, userId)).thenReturn(swapRequest);

      mockMvc.perform(get(API_BASE + "/" + swapRequestId + "/chat")
          .param("userId", userId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].ownMessage").value(true))
          .andExpect(jsonPath("$[1].ownMessage").value(false));
    }
  }

  @Nested
  @DisplayName("Send Chat Message Tests")
  class SendChatMessageTests {

    @Test
    @DisplayName("Should send text message successfully")
    void shouldSendTextMessageSuccessfully() throws Exception {
      String swapRequestId = "swap-1";
      String userId = "user-1";
      String messageText = "Hello, I'm interested!";

      User sender = createTestUser(userId, "John", "Doe", "john@test.com");
      User receiver = createTestUser("user-2", "Jane", "Doe", "jane@test.com");
      Book book = createTestBook("book-1", "Test Book", receiver);
      SwapRequest swapRequest = createTestSwapRequest(swapRequestId, sender, receiver, book);
      ChatMessage sentMessage = createTestChatMessage("msg-1", sender, messageText);

      when(chatService.sendMessage(eq(swapRequestId), eq(userId), eq(messageText), any())).thenReturn(sentMessage);
      when(chatService.getSwapRequestForChat(swapRequestId, userId)).thenReturn(swapRequest);

      mockMvc.perform(multipart(API_BASE + "/" + swapRequestId + "/chat")
          .param("userId", userId)
          .param("message", messageText))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.message").value(messageText));
    }

    @Test
    @DisplayName("Should send message with image successfully")
    void shouldSendMessageWithImageSuccessfully() throws Exception {
      String swapRequestId = "swap-1";
      String userId = "user-1";

      User sender = createTestUser(userId, "John", "Doe", "john@test.com");
      User receiver = createTestUser("user-2", "Jane", "Doe", "jane@test.com");
      Book book = createTestBook("book-1", "Test Book", receiver);
      SwapRequest swapRequest = createTestSwapRequest(swapRequestId, sender, receiver, book);
      ChatMessage sentMessage = createTestChatMessage("msg-1", sender, null);

      MockMultipartFile image = new MockMultipartFile(
          "images", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "test image content".getBytes());

      when(chatService.sendMessage(eq(swapRequestId), eq(userId), isNull(), any())).thenReturn(sentMessage);
      when(chatService.getSwapRequestForChat(swapRequestId, userId)).thenReturn(swapRequest);

      mockMvc.perform(multipart(API_BASE + "/" + swapRequestId + "/chat")
          .file(image)
          .param("userId", userId))
          .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should return 400 when neither message nor images provided")
    void shouldReturn400WhenNoContent() throws Exception {
      mockMvc.perform(multipart(API_BASE + "/swap-1/chat")
          .param("userId", "user-1"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 404 when swap request not found")
    void shouldReturn404WhenSwapRequestNotFoundOnSend() throws Exception {
      String swapRequestId = "nonexistent";
      String userId = "user-1";
      String messageText = "Hello!";

      when(chatService.sendMessage(eq(swapRequestId), eq(userId), eq(messageText), any()))
          .thenThrow(new ResourceNotFoundException("swapRequestNotFound", swapRequestId));

      mockMvc.perform(multipart(API_BASE + "/" + swapRequestId + "/chat")
          .param("userId", userId)
          .param("message", messageText))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 when userId is missing")
    void shouldReturn400WhenUserIdMissingOnSend() throws Exception {
      mockMvc.perform(multipart(API_BASE + "/swap-1/chat")
          .param("message", "Hello!"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should include swap context in response")
    void shouldIncludeSwapContextInSendResponse() throws Exception {
      String swapRequestId = "swap-1";
      String userId = "user-1";
      String messageText = "Hello!";

      User sender = createTestUser(userId, "John", "Doe", "john@test.com");
      User receiver = createTestUser("user-2", "Jane", "Doe", "jane@test.com");
      Book book = createTestBook("book-1", "Test Book", receiver);
      SwapRequest swapRequest = createTestSwapRequest(swapRequestId, sender, receiver, book);
      ChatMessage sentMessage = createTestChatMessage("msg-1", sender, messageText);

      when(chatService.sendMessage(eq(swapRequestId), eq(userId), eq(messageText), any())).thenReturn(sentMessage);
      when(chatService.getSwapRequestForChat(swapRequestId, userId)).thenReturn(swapRequest);

      mockMvc.perform(multipart(API_BASE + "/" + swapRequestId + "/chat")
          .param("userId", userId)
          .param("message", messageText))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.swapContext").exists())
          .andExpect(jsonPath("$.swapContext.requestedBook").exists());
    }
  }
}
