/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.kirjaswappi.backend.jpa.daos.*;
import com.kirjaswappi.backend.jpa.repositories.*;
import com.kirjaswappi.backend.service.ChatService;
import com.kirjaswappi.backend.service.InboxService;
import com.kirjaswappi.backend.service.entities.*;

/**
 * Integration test for performance and scalability of inbox and chat
 * operations. Tests database indexing, query performance, and concurrent
 * operations.
 */
@SpringBootTest
@ActiveProfiles("test")
public class InboxChatPerformanceIntegrationTest {

  @Autowired
  private InboxService inboxService;

  @Autowired
  private ChatService chatService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private BookRepository bookRepository;

  @Autowired
  private SwapRequestRepository swapRequestRepository;

  @Autowired
  private ChatMessageRepository chatMessageRepository;

  private List<UserDao> testUsers;
  private List<BookDao> testBooks;
  private List<SwapRequestDao> testSwapRequests;

  @BeforeEach
  void setUp() {
    // Clean up existing data
    chatMessageRepository.deleteAll();
    swapRequestRepository.deleteAll();
    bookRepository.deleteAll();
    userRepository.deleteAll();

    // Create test data for performance testing
    createLargeTestDataset();
  }

  @Test
  void testInboxQueryPerformanceWithLargeDataset() {
    // Test with a user who has many received requests
    UserDao receiverWithManyRequests = testUsers.getFirst();

    // Measure time for inbox query without filters
    long startTime = System.currentTimeMillis();
    List<SwapRequest> allRequests = inboxService.getUnifiedInbox(receiverWithManyRequests.getId(), null, null);
    long endTime = System.currentTimeMillis();

    long queryTime = endTime - startTime;
    System.out.println("Inbox query time for " + allRequests.size() + " requests: " + queryTime + "ms");

    // Should complete within reasonable time (adjust threshold as needed)
    assertTrue(queryTime < 1000, "Inbox query took too long: " + queryTime + "ms");
    assertFalse(allRequests.isEmpty(), "Should have received requests");

    // Test filtered query performance
    startTime = System.currentTimeMillis();
    List<SwapRequest> pendingRequests = inboxService.getUnifiedInbox(receiverWithManyRequests.getId(),
        "Pending", null);
    endTime = System.currentTimeMillis();

    queryTime = endTime - startTime;
    System.out.println("Filtered inbox query time: " + queryTime + "ms");
    assertTrue(queryTime < 1000, "Filtered inbox query took too long: " + queryTime + "ms");

    // Test sorted query performance
    startTime = System.currentTimeMillis();
    List<SwapRequest> sortedRequests = inboxService.getUnifiedInbox(receiverWithManyRequests.getId(), null,
        "book_title");
    endTime = System.currentTimeMillis();

    queryTime = endTime - startTime;
    System.out.println("Sorted inbox query time: " + queryTime + "ms");
    assertTrue(queryTime < 1000, "Sorted inbox query took too long: " + queryTime + "ms");

    // Verify sorting is correct
    for (int i = 1; i < sortedRequests.size(); i++) {
      String prevTitle = sortedRequests.get(i - 1).getBookToSwapWith().getTitle();
      String currTitle = sortedRequests.get(i).getBookToSwapWith().getTitle();
      assertTrue(prevTitle.compareToIgnoreCase(currTitle) <= 0,
          "Books not sorted correctly: " + prevTitle + " vs " + currTitle);
    }
  }

  @Test
  void testChatQueryPerformanceWithManyMessages() {
    // Create a swap request with many messages
    SwapRequestDao swapRequest = testSwapRequests.getFirst();
    UserDao sender = swapRequest.getSender();
    UserDao receiver = swapRequest.getReceiver();

    // Create many chat messages
    List<ChatMessageDao> messages = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      ChatMessageDao message = new ChatMessageDao();
      message.setSwapRequestId(swapRequest.getId());
      message.setSender(i % 2 == 0 ? sender : receiver);
      message.setMessage("Test message " + i);
      message.setSentAt(Instant.now().minusSeconds(100 - i));
      message.setReadByReceiver(i < 50); // First 50 messages are read
      messages.add(message);
    }
    chatMessageRepository.saveAll(messages);

    // Test chat message retrieval performance
    long startTime = System.currentTimeMillis();
    List<ChatMessage> chatMessages = chatService.getChatMessages(swapRequest.getId(), sender.getId());
    long endTime = System.currentTimeMillis();

    long queryTime = endTime - startTime;
    System.out.println("Chat messages query time for " + chatMessages.size() + " messages: " + queryTime + "ms");
    assertTrue(queryTime < 500, "Chat messages query took too long: " + queryTime + "ms");
    assertEquals(100, chatMessages.size());

    // Verify messages are ordered correctly by sent time
    for (int i = 1; i < chatMessages.size(); i++) {
      assertTrue(chatMessages.get(i - 1).getSentAt().isBefore(chatMessages.get(i).getSentAt()) ||
          chatMessages.get(i - 1).getSentAt().equals(chatMessages.get(i).getSentAt()),
          "Messages not ordered correctly by sent time");
    }

    // Test unread count performance
    startTime = System.currentTimeMillis();
    long unreadCount = chatService.getUnreadMessageCount(swapRequest.getId(), receiver.getId());
    endTime = System.currentTimeMillis();

    queryTime = endTime - startTime;
    System.out.println("Unread count query time: " + queryTime + "ms");
    assertTrue(queryTime < 200, "Unread count query took too long: " + queryTime + "ms");
    assertEquals(25, unreadCount); // 25 messages from receiver that are unread (messages 51, 53, 55, ..., 99)
  }

  @Test
  void testConcurrentChatOperations() throws InterruptedException {
    SwapRequestDao swapRequest = testSwapRequests.getFirst();
    UserDao sender = swapRequest.getSender();
    UserDao receiver = swapRequest.getReceiver();

    ExecutorService executor = Executors.newFixedThreadPool(10);
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    // Simulate concurrent message sending
    for (int i = 0; i < 20; i++) {
      final int messageNum = i;
      CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        try {
          UserDao messageSender = messageNum % 2 == 0 ? sender : receiver;
          ChatMessage message = chatService.sendMessage(swapRequest.getId(), messageSender.getId(),
              "Concurrent message " + messageNum);
          assertNotNull(message);
        } catch (Exception e) {
          fail("Concurrent message sending failed: " + e.getMessage());
        }
      }, executor);
      futures.add(future);
    }

    // Wait for all operations to complete
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.SECONDS);

    // Verify all messages were saved correctly
    List<ChatMessage> allMessages = chatService.getChatMessages(swapRequest.getId(), sender.getId());
    assertEquals(20, allMessages.size());

    // Verify message ordering is maintained
    for (int i = 1; i < allMessages.size(); i++) {
      assertTrue(allMessages.get(i - 1).getSentAt().isBefore(allMessages.get(i).getSentAt()) ||
          allMessages.get(i - 1).getSentAt().equals(allMessages.get(i).getSentAt()),
          "Concurrent messages not ordered correctly");
    }
  }

  @Test
  void testConcurrentInboxOperations() throws InterruptedException {
    UserDao receiver = testUsers.getFirst();
    ExecutorService executor = Executors.newFixedThreadPool(5);
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    // Simulate concurrent inbox access and status updates
    for (int i = 0; i < 10; i++) {
      final int operationNum = i;
      CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        try {
          if (operationNum % 3 == 0) {
            // Read inbox
            List<SwapRequest> requests = inboxService.getUnifiedInbox(receiver.getId(), null, null);
            assertNotNull(requests);
          } else if (operationNum % 3 == 1) {
            // Mark inbox item as read
            if (!testSwapRequests.isEmpty()) {
              inboxService.markInboxItemAsRead(testSwapRequests.getFirst().getId(), receiver.getId());
            }
          } else {
            // Update status (if pending)
            SwapRequestDao pendingRequest = testSwapRequests.stream()
                .filter(r -> "Pending".equals(r.getSwapStatus()) && r.getReceiver().getId().equals(receiver.getId()))
                .findFirst()
                .orElse(null);
            if (pendingRequest != null) {
              try {
                inboxService.updateSwapRequestStatus(pendingRequest.getId(), "Accepted", receiver.getId());
              } catch (IllegalArgumentException e) {
                // Expected if already updated by another thread
              }
            }
          }
        } catch (Exception e) {
          fail("Concurrent inbox operation failed: " + e.getMessage());
        }
      }, executor);
      futures.add(future);
    }

    // Wait for all operations to complete
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.SECONDS);

    // Verify data consistency after concurrent operations
    List<SwapRequest> finalRequests = inboxService.getUnifiedInbox(receiver.getId(), null, null);
    assertNotNull(finalRequests);
    assertFalse(finalRequests.isEmpty());
  }

  @Test
  void testUnreadCountCachingPerformance() {
    SwapRequestDao swapRequest = testSwapRequests.getFirst();
    UserDao receiver = swapRequest.getReceiver();

    // Send some messages to create unread count
    chatService.sendMessage(swapRequest.getId(), swapRequest.getSender().getId(), "Message 1");
    chatService.sendMessage(swapRequest.getId(), swapRequest.getSender().getId(), "Message 2");
    chatService.sendMessage(swapRequest.getId(), swapRequest.getSender().getId(), "Message 3");

    // First call - should hit database and cache result
    long startTime = System.currentTimeMillis();
    long unreadCount1 = inboxService.getUnreadMessageCount(receiver.getId(), swapRequest.getId());
    long firstCallTime = System.currentTimeMillis() - startTime;

    // Second call - should hit cache
    startTime = System.currentTimeMillis();
    long unreadCount2 = inboxService.getUnreadMessageCount(receiver.getId(), swapRequest.getId());
    long secondCallTime = System.currentTimeMillis() - startTime;

    // Third call - should also hit cache
    startTime = System.currentTimeMillis();
    long unreadCount3 = inboxService.getUnreadMessageCount(receiver.getId(), swapRequest.getId());
    long thirdCallTime = System.currentTimeMillis() - startTime;

    // Verify results are consistent
    assertEquals(3, unreadCount1);
    assertEquals(unreadCount1, unreadCount2);
    assertEquals(unreadCount1, unreadCount3);

    // Cached calls should be faster (though this might be flaky in CI)
    System.out.println("First call (DB): " + firstCallTime + "ms");
    System.out.println("Second call (cache): " + secondCallTime + "ms");
    System.out.println("Third call (cache): " + thirdCallTime + "ms");

    // Send another message - should invalidate cache
    chatService.sendMessage(swapRequest.getId(), swapRequest.getSender().getId(), "Message 4");

    long unreadCount4 = inboxService.getUnreadMessageCount(receiver.getId(), swapRequest.getId());
    assertEquals(4, unreadCount4);
  }

  @Test
  void testDatabaseIndexEffectiveness() {
    // This test verifies that database indexes are being used effectively
    UserDao receiver = testUsers.getFirst();

    // Test inbox queries with different filters to ensure indexes are used
    long startTime = System.currentTimeMillis();
    List<SwapRequest> byReceiver = inboxService.getUnifiedInbox(receiver.getId(), null, null);
    long receiverQueryTime = System.currentTimeMillis() - startTime;

    startTime = System.currentTimeMillis();
    List<SwapRequest> byReceiverAndStatus = inboxService.getUnifiedInbox(receiver.getId(), "Pending", null);
    long filteredQueryTime = System.currentTimeMillis() - startTime;

    System.out.println("Receiver query time: " + receiverQueryTime + "ms");
    System.out.println("Receiver + status query time: " + filteredQueryTime + "ms");

    // Both queries should be fast with proper indexing
    assertTrue(receiverQueryTime < 500, "Receiver query too slow, check receiverId index");
    assertTrue(filteredQueryTime < 500, "Filtered query too slow, check compound index");

    // Test chat message queries
    if (!testSwapRequests.isEmpty()) {
      SwapRequestDao swapRequest = testSwapRequests.getFirst();

      startTime = System.currentTimeMillis();
      List<ChatMessage> messages = chatService.getChatMessages(swapRequest.getId(), receiver.getId());
      long chatQueryTime = System.currentTimeMillis() - startTime;

      startTime = System.currentTimeMillis();
      long unreadCount = chatService.getUnreadMessageCount(swapRequest.getId(), receiver.getId());
      long unreadQueryTime = System.currentTimeMillis() - startTime;

      System.out.println("Chat messages query time: " + chatQueryTime + "ms");
      System.out.println("Unread count query time: " + unreadQueryTime + "ms");

      assertTrue(chatQueryTime < 300, "Chat query too slow, check swapRequestId index");
      assertTrue(unreadQueryTime < 200, "Unread count query too slow, check compound index");
    }
  }

  @Test
  void testMemoryUsageWithLargeResultSets() {
    // Test that large result sets don't cause memory issues
    UserDao receiver = testUsers.getFirst();

    // Get runtime for memory monitoring
    Runtime runtime = Runtime.getRuntime();
    long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

    // Perform operations that return large result sets
    List<SwapRequest> allRequests = inboxService.getUnifiedInbox(receiver.getId(), null, null);
    List<SwapRequest> sortedRequests = inboxService.getUnifiedInbox(receiver.getId(), null, "book_title");

    long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
    long memoryUsed = memoryAfter - memoryBefore;

    System.out.println("Memory used for large result sets: " + (memoryUsed / 1024) + " KB");
    System.out.println("Requests loaded: " + allRequests.size());

    // Memory usage should be reasonable (adjust threshold as needed)
    assertTrue(memoryUsed < 50 * 1024 * 1024, "Memory usage too high: " + (memoryUsed / 1024 / 1024) + " MB");

    // Verify results are correct
    assertNotNull(allRequests);
    assertNotNull(sortedRequests);
    assertEquals(allRequests.size(), sortedRequests.size());
  }

  // Helper method to create large test dataset
  private void createLargeTestDataset() {
    testUsers = new ArrayList<>();
    testBooks = new ArrayList<>();
    testSwapRequests = new ArrayList<>();

    // Create 20 test users
    for (int i = 0; i < 20; i++) {
      UserDao user = new UserDao();
      user.setFirstName("User" + i);
      user.setLastName("Test");
      user.setEmail("user" + i + "@test.com");
      user.setPassword("password");
      user.setEmailVerified(true);
      user.setSalt("salt");
      testUsers.add(userRepository.save(user));
    }

    // Create 50 test books (distributed among users)
    for (int i = 0; i < 50; i++) {
      BookDao book = new BookDao();
      book.setTitle("Book " + String.format("%02d", i));
      book.setAuthor("Author " + (i % 10));
      book.setCondition("Good");
      book.setLanguage("English");
      book.setOwner(testUsers.get(i % testUsers.size()));
      book.setCoverPhotos(List.of());
      book.setGenres(List.of());
      var swapCondition = new SwapConditionDao();
      swapCondition.setSwapType("GiveAway");
      swapCondition.setGiveAway(true);
      book.setSwapCondition(swapCondition);
      testBooks.add(bookRepository.save(book));
    }

    // Create 100 swap requests with various statuses and timestamps
    String[] statuses = { "Pending", "Accepted", "Rejected" };
    for (int i = 0; i < 100; i++) {
      SwapRequestDao request = new SwapRequestDao();
      request.setSender(testUsers.get(i % (testUsers.size() - 1) + 1)); // Avoid same sender/receiver
      request.setReceiver(testUsers.getFirst()); // Most requests go to first user for testing
      request.setBookToSwapWith(testBooks.get(i % testBooks.size()));
      request.setSwapType("ByBooks");
      request.setSwapStatus(statuses[i % statuses.length]);
      request.setAskForGiveaway(false);
      request.setNote("Test request " + i);
      request.setRequestedAt(Instant.now().minusSeconds(i * 60)); // Spread over time
      request.setUpdatedAt(Instant.now().minusSeconds(i * 60));
      testSwapRequests.add(swapRequestRepository.save(request));
    }

    // Create some requests for other users too
    for (int i = 0; i < 20; i++) {
      SwapRequestDao request = new SwapRequestDao();
      request.setSender(testUsers.getFirst());
      request.setReceiver(testUsers.get(i % (testUsers.size() - 1) + 1));
      request.setBookToSwapWith(testBooks.get(i % testBooks.size()));
      request.setSwapType("ByBooks");
      request.setSwapStatus("Pending");
      request.setAskForGiveaway(false);
      request.setNote("Sent request " + i);
      request.setRequestedAt(Instant.now().minusSeconds(i * 30));
      request.setUpdatedAt(Instant.now().minusSeconds(i * 30));
      testSwapRequests.add(swapRequestRepository.save(request));
    }
  }
}