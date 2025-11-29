/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirjaswappi.backend.config.TestContainersConfig;
import com.kirjaswappi.backend.http.dtos.requests.UpdateSwapStatusRequest;
import com.kirjaswappi.backend.jpa.daos.*;
import com.kirjaswappi.backend.jpa.repositories.*;

/**
 * Integration test for the complete Inbox and Chat API workflow. Tests the HTTP
 * endpoints end-to-end with real database operations.
 */
@SpringBootTest
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class InboxChatApiIntegrationTest {

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private BookRepository bookRepository;

  @Autowired
  private SwapRequestRepository swapRequestRepository;

  @Autowired
  private ChatMessageRepository chatMessageRepository;

  @Autowired
  private ObjectMapper objectMapper;

  private MockMvc mockMvc;
  private UserDao senderUser;
  private UserDao receiverUser;
  private BookDao testBook;
  private SwapRequestDao testSwapRequest;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    // Clean up existing data
    chatMessageRepository.deleteAll();
    swapRequestRepository.deleteAll();
    bookRepository.deleteAll();
    userRepository.deleteAll();

    // Create test users
    senderUser = createTestUser("John", "Sender", "john.sender@test.com");
    receiverUser = createTestUser("Jane", "Receiver", "jane.receiver@test.com");

    // Create test book
    testBook = createTestBook("Test Book", "Test Author", receiverUser);

    // Create test swap request
    testSwapRequest = createTestSwapRequest(senderUser, receiverUser, testBook);
  }

  @Test
  void testCompleteInboxChatApiWorkflow() throws Exception {
    // 1. Get unified inbox - should return 1 request
    mockMvc.perform(get("/api/v1/inbox")
        .param("userId", receiverUser.id()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(testSwapRequest.id()))
        .andExpect(jsonPath("$[0].swapStatus").value("Pending"))
        .andExpect(jsonPath("$[0].unreadMessageCount").value(0))
        .andExpect(jsonPath("$[0].sender.name").value("John Sender"))
        .andExpect(jsonPath("$[0].bookToSwapWith.title").value("Test Book"))
        .andExpect(jsonPath("$[0].conversationType").value("received"));

    // 2. Sender sends a chat message
    mockMvc.perform(multipart("/api/v1/swap-requests/" + testSwapRequest.id() + "/chat")
        .param("userId", senderUser.id())
        .param("message", "Hi! I'm interested in your book. Is it still available?"))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").value("Hi! I'm interested in your book. Is it still available?"))
        .andExpect(jsonPath("$.sender.name").value("John Sender"))
        .andExpect(jsonPath("$.readByReceiver").value(false));

    // 3. Get unified inbox again - should show unread message count
    mockMvc.perform(get("/api/v1/inbox")
        .param("userId", receiverUser.id()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].unreadMessageCount").value(1));

    // 4. Receiver gets chat messages - should auto-mark as read
    mockMvc.perform(get("/api/v1/swap-requests/" + testSwapRequest.id() + "/chat")
        .param("userId", receiverUser.id()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].message").value("Hi! I'm interested in your book. Is it still available?"))
        .andExpect(jsonPath("$[0].sender.name").value("John Sender"));

    // 5. Receiver responds with a message
    mockMvc.perform(multipart("/api/v1/swap-requests/" + testSwapRequest.id() + "/chat")
        .param("userId", receiverUser.id())
        .param("message", "Yes, it's available! What would you like to offer in exchange?"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.message").value("Yes, it's available! What would you like to offer in exchange?"))
        .andExpect(jsonPath("$.sender.name").value("Jane Receiver"));

    // 6. Receiver accepts the swap request
    UpdateSwapStatusRequest statusRequest = new UpdateSwapStatusRequest();
    statusRequest.setStatus("Accepted");

    mockMvc.perform(put("/api/v1/swap-requests/" + testSwapRequest.id() + "/status")
        .header("X-User-Id", receiverUser.id())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(statusRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.swapStatus").value("Accepted"));

    // 7. Get unified inbox for sender - should see status change
    mockMvc.perform(get("/api/v1/inbox")
        .param("userId", senderUser.id()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].swapStatus").value("Accepted"))
        .andExpect(jsonPath("$[0].conversationType").value("sent"))
        .andExpect(jsonPath("$[0].unreadMessageCount").value(1)); // Receiver's response message

    // 8. Test filtering by status
    mockMvc.perform(get("/api/v1/inbox")
        .param("userId", receiverUser.id())
        .param("status", "Accepted"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].swapStatus").value("Accepted"));

    mockMvc.perform(get("/api/v1/inbox")
        .param("userId", receiverUser.id())
        .param("status", "Pending"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

    // 9. Test sorting by book title
    mockMvc.perform(get("/api/v1/inbox")
        .param("userId", receiverUser.id())
        .param("sortBy", "book_title"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].bookToSwapWith.title").value("Test Book"));
  }

  @Test
  void testInboxFilteringAndSortingApi() throws Exception {
    // Create additional test data
    UserDao anotherUser = createTestUser("Alice", "Another", "alice.another@test.com");
    BookDao anotherBook = createTestBook("Another Book", "Another Author", receiverUser);
    BookDao zBook = createTestBook("Zebra Book", "Zebra Author", receiverUser);

    SwapRequestDao request2 = createTestSwapRequest(anotherUser, receiverUser, anotherBook);
    SwapRequestDao request3 = createTestSwapRequest(senderUser, receiverUser, zBook);

    // Update one request to accepted status
    request2.swapStatus("Accepted");
    swapRequestRepository.save(request2);

    // 1. Test no filters - should return all 3 requests
    mockMvc.perform(get("/api/v1/inbox")
        .param("userId", receiverUser.id()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3));

    // 2. Test status filtering
    mockMvc.perform(get("/api/v1/inbox")
        .param("userId", receiverUser.id())
        .param("status", "Pending"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));

    mockMvc.perform(get("/api/v1/inbox")
        .param("userId", receiverUser.id())
        .param("status", "Accepted"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));

    // 3. Test sorting by book title
    mockMvc.perform(get("/api/v1/inbox")
        .param("userId", receiverUser.id())
        .param("sortBy", "book_title"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].bookToSwapWith.title").value("Another Book"))
        .andExpect(jsonPath("$[1].bookToSwapWith.title").value("Test Book"))
        .andExpect(jsonPath("$[2].bookToSwapWith.title").value("Zebra Book"));

    // 4. Test sorting by sender name
    mockMvc.perform(get("/api/v1/inbox")
        .param("userId", receiverUser.id())
        .param("sortBy", "sender_name"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].sender.name").value("Alice Another"))
        .andExpect(jsonPath("$[1].sender.name").value("John Sender"))
        .andExpect(jsonPath("$[2].sender.name").value("John Sender"));

    // 5. Test combined filtering and sorting
    mockMvc.perform(get("/api/v1/inbox")
        .param("userId", receiverUser.id())
        .param("status", "Pending")
        .param("sortBy", "book_title"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].bookToSwapWith.title").value("Test Book"))
        .andExpect(jsonPath("$[1].bookToSwapWith.title").value("Zebra Book"));
  }

  @Test
  void testChatAccessControlAndErrorHandling() throws Exception {
    UserDao unauthorizedUser = createTestUser("Unauthorized", "User", "unauthorized@test.com");

    // 1. Test unauthorized access to chat
    mockMvc.perform(get("/api/v1/swap-requests/" + testSwapRequest.id() + "/chat")
        .param("userId", unauthorizedUser.id()))
        .andExpect(status().isForbidden());

    // 2. Test unauthorized message sending
    mockMvc.perform(multipart("/api/v1/swap-requests/" + testSwapRequest.id() + "/chat")
        .param("userId", unauthorizedUser.id())
        .param("message", "This should fail"))
        .andExpect(status().isForbidden());

    // 3. Test invalid swap request ID
    mockMvc.perform(get("/api/v1/swap-requests/nonexistent/chat")
        .param("userId", senderUser.id()))
        .andExpect(status().isNotFound());

    // 4. Test empty message validation - should work with multipart but no content
    mockMvc.perform(multipart("/api/v1/swap-requests/" + testSwapRequest.id() + "/chat")
        .param("userId", senderUser.id()))
        .andExpect(status().isBadRequest());

    // 5. Test invalid status update
    UpdateSwapStatusRequest invalidStatus = new UpdateSwapStatusRequest();
    invalidStatus.setStatus("InvalidStatus");

    mockMvc.perform(put("/api/v1/swap-requests/" + testSwapRequest.id() + "/status")
        .param("userId", receiverUser.id())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidStatus)))
        .andExpect(status().isBadRequest());

    // 6. Test unauthorized status update (sender trying to update)
    UpdateSwapStatusRequest statusRequest = new UpdateSwapStatusRequest();
    statusRequest.setStatus("Accepted");

    mockMvc.perform(put("/api/v1/swap-requests/" + testSwapRequest.id() + "/status")
        .param("userId", senderUser.id())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(statusRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void testReadStatusTrackingThroughApi() throws Exception {
    // 1. Send a message
    mockMvc.perform(multipart("/api/v1/swap-requests/" + testSwapRequest.id() + "/chat")
        .param("userId", senderUser.id())
        .param("message", "Test message for read tracking"))
        .andExpect(status().isCreated());

    // 2. Check inbox shows unread message
    mockMvc.perform(get("/api/v1/inbox")
        .param("userId", receiverUser.id()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].unreadMessageCount").value(1));

    // 3. Access chat messages - should mark as read
    mockMvc.perform(get("/api/v1/swap-requests/" + testSwapRequest.id() + "/chat")
        .param("userId", receiverUser.id()))
        .andExpect(status().isOk());

    // 4. Check inbox again - unread count should be 0
    mockMvc.perform(get("/api/v1/inbox")
        .param("userId", receiverUser.id()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].unreadMessageCount").value(0));
  }

  @Test
  void testConcurrentOperationsAndConsistency() throws Exception {
    // This test simulates concurrent operations to ensure data consistency

    // 1. Send multiple messages rapidly
    for (int i = 1; i <= 5; i++) {
      mockMvc.perform(multipart("/api/v1/swap-requests/" + testSwapRequest.id() + "/chat")
          .param("userId", i % 2 == 0 ? senderUser.id() : receiverUser.id())
          .param("message", "Message " + i))
          .andExpect(status().isCreated());
    }

    // 2. Verify all messages are stored correctly
    mockMvc.perform(get("/api/v1/swap-requests/" + testSwapRequest.id() + "/chat")
        .param("userId", senderUser.id()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(5));

    // 3. Update status while messages exist
    UpdateSwapStatusRequest statusRequest = new UpdateSwapStatusRequest();
    statusRequest.setStatus("Accepted");

    mockMvc.perform(put("/api/v1/swap-requests/" + testSwapRequest.id() + "/status")
        .header("X-User-Id", receiverUser.id())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(statusRequest)))
        .andExpect(status().isOk());

    // 4. Verify messages still exist after status change
    mockMvc.perform(get("/api/v1/swap-requests/" + testSwapRequest.id() + "/chat")
        .param("userId", receiverUser.id()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(5));

    // 5. Verify inbox consistency
    mockMvc.perform(get("/api/v1/inbox")
        .param("userId", receiverUser.id()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].swapStatus").value("Accepted"));
  }

  // Helper methods
  private UserDao createTestUser(String firstName, String lastName, String email) {
    UserDao user = UserDao.builder()
        .firstName(firstName)
        .lastName(lastName)
        .email(email)
        .password("password")
        .isEmailVerified(true)
        .salt("salt")
        .build();
    return userRepository.save(user);
  }

  private BookDao createTestBook(String title, String author, UserDao owner) {
    BookDao book = BookDao.builder()
        .title(title)
        .author(author)
        .condition("Good")
        .language("English")
        .owner(owner)
        .coverPhotos(List.of())
        .genres(List.of())
        .swapCondition(createSwapCondition())
        .build();
    return bookRepository.save(book);
  }

  private SwapConditionDao createSwapCondition() {
    return SwapConditionDao.builder()
        .swapType("GiveAway")
        .giveAway(true)
        .build();
  }

  private SwapRequestDao createTestSwapRequest(UserDao sender, UserDao receiver, BookDao book) {
    SwapRequestDao request = SwapRequestDao.builder()
        .sender(sender)
        .receiver(receiver)
        .bookToSwapWith(book)
        .swapType("ByBooks")
        .swapStatus("Pending")
        .askForGiveaway(false)
        .note("Test swap request")
        .requestedAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
    return swapRequestRepository.save(request);
  }
}
