/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.controllers.mockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirjaswappi.backend.common.http.controllers.mockMvc.config.CustomMockMvcConfiguration;
import com.kirjaswappi.backend.http.controllers.ChatController;
import com.kirjaswappi.backend.http.dtos.requests.SendMessageRequest;
import com.kirjaswappi.backend.service.ChatService;
import com.kirjaswappi.backend.service.entities.Book;
import com.kirjaswappi.backend.service.entities.ChatMessage;
import com.kirjaswappi.backend.service.entities.SwapRequest;
import com.kirjaswappi.backend.service.entities.User;
import com.kirjaswappi.backend.service.enums.SwapStatus;
import com.kirjaswappi.backend.service.enums.SwapType;
import com.kirjaswappi.backend.service.exceptions.ChatAccessDeniedException;
import com.kirjaswappi.backend.service.exceptions.SwapRequestNotFoundException;

@WebMvcTest(ChatController.class)
@Import(CustomMockMvcConfiguration.class)
class ChatControllerTest {
  private static final String API_PATH = "/api/v1/swap-requests";

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ChatService chatService;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @DisplayName("Should return chat messages when user has access")
  void shouldReturnChatMessagesWhenUserHasAccess() throws Exception {
    // Given
    User sender = new User();
    sender.setId("sender123");
    sender.setFirstName("John");
    sender.setLastName("Doe");

    Book requestedBook = new Book();
    requestedBook.setId("book123");
    requestedBook.setTitle("Test Book");
    requestedBook.setAuthor("Test Author");

    SwapRequest swapRequest = new SwapRequest();
    swapRequest.setId("swap123");
    swapRequest.setSender(sender);
    swapRequest.setBookToSwapWith(requestedBook);
    swapRequest.setSwapType(SwapType.BY_BOOKS);
    swapRequest.setSwapStatus(SwapStatus.PENDING);
    swapRequest.setAskForGiveaway(false);

    ChatMessage message1 = new ChatMessage();
    message1.setId("msg1");
    message1.setSwapRequestId("swap123");
    message1.setSender(sender);
    message1.setMessage("Hello, is this book available?");
    message1.setSentAt(Instant.parse("2025-01-01T10:00:00Z"));
    message1.setReadByReceiver(false);

    ChatMessage message2 = new ChatMessage();
    message2.setId("msg2");
    message2.setSwapRequestId("swap123");
    message2.setSender(sender);
    message2.setMessage("Yes, it's still available!");
    message2.setSentAt(Instant.parse("2025-01-01T10:05:00Z"));
    message2.setReadByReceiver(true);

    List<ChatMessage> messages = Arrays.asList(message1, message2);
    when(chatService.getChatMessages("swap123", "user123")).thenReturn(messages);
    when(chatService.getSwapRequestForChat("swap123", "user123")).thenReturn(swapRequest);

    // When & Then
    mockMvc.perform(get(API_PATH + "/swap123/chat")
        .param("userId", "user123"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value("msg1"))
        .andExpect(jsonPath("$[0].message").value("Hello, is this book available?"))
        .andExpect(jsonPath("$[0].ownMessage").value(false))
        .andExpect(jsonPath("$[0].swapContext").exists())
        .andExpect(jsonPath("$[0].swapContext.swapType").value("ByBooks"))
        .andExpect(jsonPath("$[0].swapContext.swapStatus").value("Pending"))
        .andExpect(jsonPath("$[0].swapContext.askForGiveaway").value(false))
        .andExpect(jsonPath("$[0].swapContext.requestedBook.id").value("book123"))
        .andExpect(jsonPath("$[0].swapContext.requestedBook.title").value("Test Book"))
        .andExpect(jsonPath("$[0].swapContext.requestedBook.author").value("Test Author"))
        .andExpect(jsonPath("$[0].swapContext.offeredBook").doesNotExist())
        .andExpect(jsonPath("$[0].swapContext.offeredGenre").doesNotExist())
        .andExpect(jsonPath("$[1].id").value("msg2"))
        .andExpect(jsonPath("$[1].message").value("Yes, it's still available!"))
        .andExpect(jsonPath("$[1].readByReceiver").value(true));

    verify(chatService).getChatMessages("swap123", "user123");
    verify(chatService).getSwapRequestForChat("swap123", "user123");
  }

  @Test
  @DisplayName("Should return 403 when user has no access to chat")
  void shouldReturn403WhenUserHasNoAccessToChat() throws Exception {
    // Given
    when(chatService.getChatMessages("swap123", "unauthorized123"))
        .thenThrow(new ChatAccessDeniedException());

    // When & Then
    mockMvc.perform(get(API_PATH + "/swap123/chat")
        .param("userId", "unauthorized123"))
        .andExpect(status().isForbidden());

    verify(chatService).getChatMessages("swap123", "unauthorized123");
  }

  @Test
  @DisplayName("Should return 404 when swap request not found")
  void shouldReturn404WhenSwapRequestNotFound() throws Exception {
    // Given
    when(chatService.getChatMessages("nonexistent", "user123"))
        .thenThrow(new SwapRequestNotFoundException());

    // When & Then
    mockMvc.perform(get(API_PATH + "/nonexistent/chat")
        .param("userId", "user123"))
        .andExpect(status().isNotFound());

    verify(chatService).getChatMessages("nonexistent", "user123");
  }

  @Test
  @DisplayName("Should send message successfully")
  void shouldSendMessageSuccessfully() throws Exception {
    // Given
    User sender = new User();
    sender.setId("user123");
    sender.setFirstName("John");
    sender.setLastName("Doe");

    Book requestedBook = new Book();
    requestedBook.setId("book123");
    requestedBook.setTitle("Test Book");
    requestedBook.setAuthor("Test Author");

    SwapRequest swapRequest = new SwapRequest();
    swapRequest.setId("swap123");
    swapRequest.setSender(sender);
    swapRequest.setBookToSwapWith(requestedBook);
    swapRequest.setSwapType(SwapType.BY_BOOKS);
    swapRequest.setSwapStatus(SwapStatus.PENDING);
    swapRequest.setAskForGiveaway(false);

    ChatMessage sentMessage = new ChatMessage();
    sentMessage.setId("msg123");
    sentMessage.setSwapRequestId("swap123");
    sentMessage.setSender(sender);
    sentMessage.setMessage("Hello, is this book still available?");
    sentMessage.setSentAt(Instant.parse("2025-01-01T10:00:00Z"));
    sentMessage.setReadByReceiver(false);

    when(chatService.sendMessage("swap123", "user123", "Hello, is this book still available?", null))
        .thenReturn(sentMessage);
    when(chatService.getSwapRequestForChat("swap123", "user123")).thenReturn(swapRequest);

    // When & Then
    mockMvc.perform(multipart(API_PATH + "/swap123/chat")
        .param("userId", "user123")
        .param("message", "Hello, is this book still available?"))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value("msg123"))
        .andExpect(jsonPath("$.message").value("Hello, is this book still available?"))
        .andExpect(jsonPath("$.readByReceiver").value(false))
        .andExpect(jsonPath("$.sender.id").value("user123"))
        .andExpect(jsonPath("$.sender.name").value("John Doe"));

    verify(chatService).sendMessage("swap123", "user123", "Hello, is this book still available?", null);
    verify(chatService).getSwapRequestForChat("swap123", "user123");
  }

  @Test
  @DisplayName("Should return 400 when sending empty message")
  void shouldReturn400WhenSendingEmptyMessage() throws Exception {
    // Given
    SendMessageRequest request = new SendMessageRequest();
    request.setMessage("");

    // When & Then
    mockMvc.perform(post(API_PATH + "/swap123/chat")
        .param("userId", "user123")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(chatService);
  }

  @Test
  @DisplayName("Should return 400 when sending null message")
  void shouldReturn400WhenSendingNullMessage() throws Exception {
    // Given
    SendMessageRequest request = new SendMessageRequest();
    request.setMessage(null);

    // When & Then
    mockMvc.perform(post(API_PATH + "/swap123/chat")
        .param("userId", "user123")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(chatService);
  }

  @Test
  @DisplayName("Should return 403 when unauthorized user tries to send message")
  void shouldReturn403WhenUnauthorizedUserTriesToSendMessage() throws Exception {
    // Given
    when(chatService.sendMessage("swap123", "unauthorized123", "Hello there!", null))
        .thenThrow(new ChatAccessDeniedException());

    // When & Then
    mockMvc.perform(multipart(API_PATH + "/swap123/chat")
        .param("userId", "unauthorized123")
        .param("message", "Hello there!"))
        .andExpect(status().isForbidden());

    verify(chatService).sendMessage("swap123", "unauthorized123", "Hello there!", null);
  }

  @Test
  @DisplayName("Should return chat messages without swap context for subsequent messages")
  void shouldReturnChatMessagesWithoutSwapContextForSubsequentMessages() throws Exception {
    // Given
    User sender = new User();
    sender.setId("sender123");
    sender.setFirstName("John");
    sender.setLastName("Doe");

    Book requestedBook = new Book();
    requestedBook.setId("book123");
    requestedBook.setTitle("Pride and Prejudice");
    requestedBook.setAuthor("Jane Austen");

    SwapRequest swapRequest = new SwapRequest();
    swapRequest.setId("swap123");
    swapRequest.setSender(sender);
    swapRequest.setBookToSwapWith(requestedBook);
    swapRequest.setSwapType(SwapType.BY_GENRES);
    swapRequest.setSwapStatus(SwapStatus.PENDING);

    ChatMessage message1 = new ChatMessage();
    message1.setId("msg1");
    message1.setSwapRequestId("swap123");
    message1.setSender(sender);
    message1.setMessage("First message");
    message1.setSentAt(Instant.parse("2025-01-01T10:00:00Z"));

    ChatMessage message2 = new ChatMessage();
    message2.setId("msg2");
    message2.setSwapRequestId("swap123");
    message2.setSender(sender);
    message2.setMessage("Second message");
    message2.setSentAt(Instant.parse("2025-01-01T10:05:00Z"));

    List<ChatMessage> messages = Arrays.asList(message1, message2);
    when(chatService.getChatMessages("swap123", "user123")).thenReturn(messages);
    when(chatService.getSwapRequestForChat("swap123", "user123")).thenReturn(swapRequest);

    // When & Then
    mockMvc.perform(get(API_PATH + "/swap123/chat")
        .param("userId", "user123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value("msg1"))
        .andExpect(jsonPath("$[0].swapContext").exists()) // First message has context
        .andExpect(jsonPath("$[1].id").value("msg2"))
        .andExpect(jsonPath("$[1].swapContext").doesNotExist()); // Subsequent messages don't have context

    verify(chatService).getChatMessages("swap123", "user123");
    verify(chatService).getSwapRequestForChat("swap123", "user123");
  }
}
