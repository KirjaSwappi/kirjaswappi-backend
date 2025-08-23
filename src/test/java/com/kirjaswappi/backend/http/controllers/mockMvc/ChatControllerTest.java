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
import com.kirjaswappi.backend.service.entities.Genre;
import com.kirjaswappi.backend.service.entities.SwapOffer;
import com.kirjaswappi.backend.service.entities.SwapRequest;
import com.kirjaswappi.backend.service.entities.SwappableBook;
import com.kirjaswappi.backend.service.entities.User;
import com.kirjaswappi.backend.service.enums.Condition;
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
    SendMessageRequest request = new SendMessageRequest();
    request.setMessage("Hello, is this book still available?");

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

    when(chatService.sendMessage("swap123", "user123", "Hello, is this book still available?"))
        .thenReturn(sentMessage);
    when(chatService.getSwapRequestForChat("swap123", "user123")).thenReturn(swapRequest);

    // When & Then
    mockMvc.perform(post(API_PATH + "/swap123/chat")
        .param("userId", "user123")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value("msg123"))
        .andExpect(jsonPath("$.message").value("Hello, is this book still available?"))
        .andExpect(jsonPath("$.readByReceiver").value(false))
        .andExpect(jsonPath("$.sender.id").value("user123"))
        .andExpect(jsonPath("$.sender.name").value("John Doe"));

    verify(chatService).sendMessage("swap123", "user123", "Hello, is this book still available?");
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
    SendMessageRequest request = new SendMessageRequest();
    request.setMessage("Hello there!");

    when(chatService.sendMessage("swap123", "unauthorized123", "Hello there!"))
        .thenThrow(new ChatAccessDeniedException());

    // When & Then
    mockMvc.perform(post(API_PATH + "/swap123/chat")
        .param("userId", "unauthorized123")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());

    verify(chatService).sendMessage("swap123", "unauthorized123", "Hello there!");
  }

  @Test
  @DisplayName("Should mark messages as read successfully")
  void shouldMarkMessagesAsReadSuccessfully() throws Exception {
    // Given
    doNothing().when(chatService).markMessagesAsRead("swap123", "user123");

    // When & Then
    mockMvc.perform(put(API_PATH + "/swap123/chat/mark-read")
        .param("userId", "user123"))
        .andExpect(status().isNoContent());

    verify(chatService).markMessagesAsRead("swap123", "user123");
  }

  @Test
  @DisplayName("Should return 403 when unauthorized user tries to mark messages as read")
  void shouldReturn403WhenUnauthorizedUserTriesToMarkMessagesAsRead() throws Exception {
    // Given
    doThrow(new ChatAccessDeniedException()).when(chatService).markMessagesAsRead("swap123", "unauthorized123");

    // When & Then
    mockMvc.perform(put(API_PATH + "/swap123/chat/mark-read")
        .param("userId", "unauthorized123"))
        .andExpect(status().isForbidden());

    verify(chatService).markMessagesAsRead("swap123", "unauthorized123");
  }

  @Test
  @DisplayName("Should return 404 when trying to mark messages as read for nonexistent swap request")
  void shouldReturn404WhenTryingToMarkMessagesAsReadForNonexistentSwapRequest() throws Exception {
    // Given
    doThrow(new SwapRequestNotFoundException()).when(chatService).markMessagesAsRead("nonexistent", "user123");

    // When & Then
    mockMvc.perform(put(API_PATH + "/nonexistent/chat/mark-read")
        .param("userId", "user123"))
        .andExpect(status().isNotFound());

    verify(chatService).markMessagesAsRead("nonexistent", "user123");
  }

  @Test
  @DisplayName("Should return chat messages with swap context when user has access")
  void shouldReturnChatMessagesWithSwapContextWhenUserHasAccess() throws Exception {
    // Given
    User sender = new User();
    sender.setId("sender123");
    sender.setFirstName("John");
    sender.setLastName("Doe");

    User receiver = new User();
    receiver.setId("receiver123");
    receiver.setFirstName("Jane");
    receiver.setLastName("Smith");

    Book requestedBook = new Book();
    requestedBook.setId("book123");
    requestedBook.setTitle("The Great Gatsby");
    requestedBook.setAuthor("F. Scott Fitzgerald");
    requestedBook.setCondition(Condition.GOOD);
    requestedBook.setCoverPhotos(Arrays.asList("cover1.jpg", "cover2.jpg"));

    SwappableBook offeredBook = new SwappableBook();
    offeredBook.setId("offered123");
    offeredBook.setTitle("To Kill a Mockingbird");
    offeredBook.setAuthor("Harper Lee");
    offeredBook.setCoverPhoto("offered_cover.jpg");

    Genre offeredGenre = new Genre();
    offeredGenre.setId("genre123");
    offeredGenre.setName("Fiction");

    SwapOffer swapOffer = new SwapOffer();
    swapOffer.setOfferedBook(offeredBook);
    swapOffer.setOfferedGenre(offeredGenre);

    SwapRequest swapRequest = new SwapRequest();
    swapRequest.setId("swap123");
    swapRequest.setSender(sender);
    swapRequest.setReceiver(receiver);
    swapRequest.setBookToSwapWith(requestedBook);
    swapRequest.setSwapOffer(swapOffer);
    swapRequest.setSwapType(SwapType.BY_BOOKS);
    swapRequest.setSwapStatus(SwapStatus.PENDING);
    swapRequest.setAskForGiveaway(false);

    ChatMessage message = new ChatMessage();
    message.setId("msg1");
    message.setSwapRequestId("swap123");
    message.setSender(sender);
    message.setMessage("Hello, is this book available?");
    message.setSentAt(Instant.parse("2025-01-01T10:00:00Z"));
    message.setReadByReceiver(false);

    List<ChatMessage> messages = Arrays.asList(message);
    when(chatService.getChatMessages("swap123", "receiver123")).thenReturn(messages);
    when(chatService.getSwapRequestForChat("swap123", "receiver123")).thenReturn(swapRequest);

    // When & Then
    mockMvc.perform(get(API_PATH + "/swap123/chat")
        .param("userId", "receiver123"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value("msg1"))
        .andExpect(jsonPath("$[0].message").value("Hello, is this book available?"))
        .andExpect(jsonPath("$[0].ownMessage").value(false))
        .andExpect(jsonPath("$[0].swapContext").exists())
        .andExpect(jsonPath("$[0].swapContext.swapType").value("ByBooks"))
        .andExpect(jsonPath("$[0].swapContext.swapStatus").value("Pending"))
        .andExpect(jsonPath("$[0].swapContext.askForGiveaway").value(false))
        .andExpect(jsonPath("$[0].swapContext.requestedBook.id").value("book123"))
        .andExpect(jsonPath("$[0].swapContext.requestedBook.title").value("The Great Gatsby"))
        .andExpect(jsonPath("$[0].swapContext.requestedBook.author").value("F. Scott Fitzgerald"))
        .andExpect(jsonPath("$[0].swapContext.requestedBook.condition").value("Good"))
        .andExpect(jsonPath("$[0].swapContext.requestedBook.coverPhotoUrl").value("cover1.jpg"))
        .andExpect(jsonPath("$[0].swapContext.offeredBook.id").value("offered123"))
        .andExpect(jsonPath("$[0].swapContext.offeredBook.title").value("To Kill a Mockingbird"))
        .andExpect(jsonPath("$[0].swapContext.offeredBook.author").value("Harper Lee"))
        .andExpect(jsonPath("$[0].swapContext.offeredBook.coverPhotoUrl").value("offered_cover.jpg"))
        .andExpect(jsonPath("$[0].swapContext.offeredGenreName").value("Fiction"));

    verify(chatService).getChatMessages("swap123", "receiver123");
    verify(chatService).getSwapRequestForChat("swap123", "receiver123");
  }

  @Test
  @DisplayName("Should send message with swap context successfully")
  void shouldSendMessageWithSwapContextSuccessfully() throws Exception {
    // Given
    User sender = new User();
    sender.setId("sender123");
    sender.setFirstName("John");
    sender.setLastName("Doe");

    User receiver = new User();
    receiver.setId("receiver123");
    receiver.setFirstName("Jane");
    receiver.setLastName("Smith");

    Book requestedBook = new Book();
    requestedBook.setId("book123");
    requestedBook.setTitle("1984");
    requestedBook.setAuthor("George Orwell");
    requestedBook.setCondition(Condition.LIKE_NEW);
    requestedBook.setCoverPhotos(Arrays.asList("1984_cover.jpg"));

    SwapRequest swapRequest = new SwapRequest();
    swapRequest.setId("swap123");
    swapRequest.setSender(sender);
    swapRequest.setReceiver(receiver);
    swapRequest.setBookToSwapWith(requestedBook);
    swapRequest.setSwapType(SwapType.GIVE_AWAY);
    swapRequest.setSwapStatus(SwapStatus.ACCEPTED);
    swapRequest.setAskForGiveaway(true);

    ChatMessage sentMessage = new ChatMessage();
    sentMessage.setId("msg123");
    sentMessage.setSwapRequestId("swap123");
    sentMessage.setSender(sender);
    sentMessage.setMessage("Thank you for accepting!");
    sentMessage.setSentAt(Instant.parse("2025-01-01T12:00:00Z"));
    sentMessage.setReadByReceiver(false);

    SendMessageRequest request = new SendMessageRequest();
    request.setMessage("Thank you for accepting!");

    when(chatService.sendMessage("swap123", "sender123", "Thank you for accepting!")).thenReturn(sentMessage);
    when(chatService.getSwapRequestForChat("swap123", "sender123")).thenReturn(swapRequest);

    // When & Then
    mockMvc.perform(post(API_PATH + "/swap123/chat")
        .param("userId", "sender123")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value("msg123"))
        .andExpect(jsonPath("$.message").value("Thank you for accepting!"))
        .andExpect(jsonPath("$.ownMessage").value(true))
        .andExpect(jsonPath("$.swapContext").exists())
        .andExpect(jsonPath("$.swapContext.swapType").value("GiveAway"))
        .andExpect(jsonPath("$.swapContext.swapStatus").value("Accepted"))
        .andExpect(jsonPath("$.swapContext.askForGiveaway").value(true))
        .andExpect(jsonPath("$.swapContext.requestedBook.title").value("1984"))
        .andExpect(jsonPath("$.swapContext.requestedBook.author").value("George Orwell"))
        .andExpect(jsonPath("$.swapContext.requestedBook.condition").value("Like New"));

    verify(chatService).sendMessage("swap123", "sender123", "Thank you for accepting!");
    verify(chatService).getSwapRequestForChat("swap123", "sender123");
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
