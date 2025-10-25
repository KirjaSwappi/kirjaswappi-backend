/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.kirjaswappi.backend.common.service.ImageService;
import com.kirjaswappi.backend.jpa.daos.ChatMessageDao;
import com.kirjaswappi.backend.jpa.daos.SwapRequestDao;
import com.kirjaswappi.backend.jpa.repositories.ChatMessageRepository;
import com.kirjaswappi.backend.jpa.repositories.SwapRequestRepository;
import com.kirjaswappi.backend.mapper.ChatMessageMapper;
import com.kirjaswappi.backend.mapper.SwapRequestMapper;
import com.kirjaswappi.backend.service.entities.ChatMessage;
import com.kirjaswappi.backend.service.entities.SwapRequest;
import com.kirjaswappi.backend.service.entities.User;
import com.kirjaswappi.backend.service.exceptions.ChatAccessDeniedException;
import com.kirjaswappi.backend.service.exceptions.SwapRequestNotFoundException;

@Service
@Transactional
public class ChatService {
  @Autowired
  private ChatMessageRepository chatMessageRepository;

  @Autowired
  private SwapRequestRepository swapRequestRepository;

  @Autowired
  private UserService userService;

  @Autowired
  private ImageService imageService;

  @Autowired
  private SimpMessagingTemplate messagingTemplate;

  public List<ChatMessage> getChatMessages(String swapRequestId, String userId) {
    // Validate swap request exists
    Optional<SwapRequestDao> swapRequestOpt = swapRequestRepository.findById(swapRequestId);
    if (swapRequestOpt.isEmpty()) {
      throw new SwapRequestNotFoundException();
    }

    SwapRequestDao swapRequest = swapRequestOpt.get();

    // Validate user has access to this chat (must be sender or receiver)
    if (!hasAccessToChat(swapRequest, userId)) {
      throw new ChatAccessDeniedException();
    }

    // Automatically mark messages as read when user accesses chat
    markMessagesAsRead(swapRequestId, userId);

    // Get chat messages ordered by sent time
    List<ChatMessageDao> messageDaos = chatMessageRepository.findBySwapRequestIdOrderBySentAtAsc(swapRequestId);

    // Convert each message with image IDs converted to URLs
    return messageDaos.stream()
        .map(dao -> {
          List<String> imageUrls = convertImageIdsToUrls(dao.getImageIds());
          return ChatMessageMapper.toEntity(dao, imageUrls);
        })
        .toList();
  }

  public ChatMessage sendMessage(String swapRequestId, String senderId, String message) {
    // Validate swap request exists
    Optional<SwapRequestDao> swapRequestOpt = swapRequestRepository.findById(swapRequestId);
    if (swapRequestOpt.isEmpty()) {
      throw new SwapRequestNotFoundException();
    }

    SwapRequestDao swapRequest = swapRequestOpt.get();

    // Validate user has access to this chat (must be sender or receiver)
    if (!hasAccessToChat(swapRequest, senderId)) {
      throw new ChatAccessDeniedException();
    }

    // Validate message content
    if (message == null || message.trim().isEmpty()) {
      throw new IllegalArgumentException("Message cannot be empty");
    }

    // Get sender user
    User sender = userService.getUser(senderId);

    // Create chat message
    ChatMessage chatMessage = new ChatMessage();
    chatMessage.setSwapRequestId(swapRequestId);
    chatMessage.setSender(sender);
    // TODO: filter explicit or dangerous words in message
    chatMessage.setMessage(message.trim());
    chatMessage.setReadByReceiver(false);

    // Save message
    ChatMessageDao messageDao = ChatMessageMapper.toDao(chatMessage);
    ChatMessageDao savedDao = chatMessageRepository.save(messageDao);

    // Clear unread count cache for both users to ensure consistency
    String receiverId = swapRequest.getSender().getId().equals(senderId)
        ? swapRequest.getReceiver().getId()
        : swapRequest.getSender().getId();

    // Clear cache for both sender and receiver to avoid stale data
    clearUnreadCountCache(receiverId, swapRequestId);
    clearUnreadCountCache(senderId, swapRequestId);

    // Broadcast inbox update to both users for real-time inbox refresh
    broadcastInboxUpdate(senderId);
    broadcastInboxUpdate(receiverId);

    return ChatMessageMapper.toEntity(savedDao);
  }

  public ChatMessage sendMessage(String swapRequestId, String senderId, String message, List<MultipartFile> images) {
    // Validate that either message or images are provided
    boolean hasMessage = message != null && !message.trim().isEmpty();
    boolean hasImages = images != null && !images.isEmpty();

    if (!hasMessage && !hasImages) {
      throw new IllegalArgumentException("Either message text or images must be provided");
    }

    // Validate swap request exists
    Optional<SwapRequestDao> swapRequestOpt = swapRequestRepository.findById(swapRequestId);
    if (swapRequestOpt.isEmpty()) {
      throw new SwapRequestNotFoundException();
    }

    SwapRequestDao swapRequest = swapRequestOpt.get();

    // Validate user has access to this chat (must be sender or receiver)
    if (!hasAccessToChat(swapRequest, senderId)) {
      throw new ChatAccessDeniedException();
    }

    // Get sender user
    User sender = userService.getUser(senderId);

    // Upload images and get unique IDs
    List<String> imageIds = new ArrayList<>();
    if (images != null && !images.isEmpty()) {
      for (MultipartFile image : images) {
        String uniqueId = UUID.randomUUID().toString();
        imageService.uploadImage(image, uniqueId);
        imageIds.add(uniqueId);
      }
    }

    // Create chat message
    ChatMessage chatMessage = new ChatMessage();
    chatMessage.setSwapRequestId(swapRequestId);
    chatMessage.setSender(sender);
    chatMessage.setMessage(message != null && !message.trim().isEmpty() ? message.trim() : null);
    chatMessage.setImageIds(imageIds.isEmpty() ? null : imageIds);
    chatMessage.setReadByReceiver(false);

    // Save message
    ChatMessageDao messageDao = ChatMessageMapper.toDao(chatMessage);
    ChatMessageDao savedDao = chatMessageRepository.save(messageDao);

    // Clear unread count cache for both users to ensure consistency
    String receiverId = swapRequest.getSender().getId().equals(senderId)
        ? swapRequest.getReceiver().getId()
        : swapRequest.getSender().getId();

    // Clear cache for both sender and receiver to avoid stale data
    clearUnreadCountCache(receiverId, swapRequestId);
    clearUnreadCountCache(senderId, swapRequestId);

    // Broadcast inbox update to both users for real-time inbox refresh
    broadcastInboxUpdate(senderId);
    broadcastInboxUpdate(receiverId);

    // Convert image IDs to URLs for response
    List<String> imageUrls = convertImageIdsToUrls(imageIds);
    return ChatMessageMapper.toEntity(savedDao, imageUrls);
  }

  public void markMessagesAsRead(String swapRequestId, String userId) {
    // Validate swap request exists
    Optional<SwapRequestDao> swapRequestOpt = swapRequestRepository.findById(swapRequestId);
    if (swapRequestOpt.isEmpty()) {
      throw new SwapRequestNotFoundException();
    }

    SwapRequestDao swapRequest = swapRequestOpt.get();

    // Validate user has access to this chat (must be sender or receiver)
    if (!hasAccessToChat(swapRequest, userId)) {
      throw new ChatAccessDeniedException();
    }

    // Get all messages in this chat that are not from the current user and are
    // unread
    List<ChatMessageDao> allMessages = chatMessageRepository.findBySwapRequestIdOrderBySentAtAsc(swapRequestId);
    List<ChatMessageDao> unreadMessages = allMessages
        .stream()
        .filter(msg -> !msg.getSender().getId().equals(userId) && !msg.isReadByReceiver())
        .toList();

    // Mark messages as read
    for (ChatMessageDao message : unreadMessages) {
      message.setReadByReceiver(true);
      chatMessageRepository.save(message);
    }

    // Clear unread count cache AFTER marking messages as read to ensure consistency
    clearUnreadCountCache(userId, swapRequestId);
  }

  public long getUnreadMessageCount(String swapRequestId, String userId) {
    // Validate swap request exists
    Optional<SwapRequestDao> swapRequestOpt = swapRequestRepository.findById(swapRequestId);
    if (swapRequestOpt.isEmpty()) {
      throw new SwapRequestNotFoundException();
    }

    SwapRequestDao swapRequest = swapRequestOpt.get();

    // Validate user has access to this chat (must be sender or receiver)
    if (!hasAccessToChat(swapRequest, userId)) {
      throw new ChatAccessDeniedException();
    }

    // Count unread messages not sent by the current user
    return chatMessageRepository.countBySwapRequestIdAndReadByReceiverFalseAndSenderIdNot(
        swapRequestId, userId);
  }

  @CacheEvict(value = "unreadCounts", key = "#userId + '_' + #swapRequestId", beforeInvocation = true)
  public void clearUnreadCountCache(String userId, String swapRequestId) {
    // This method is used to clear the cache for unread message counts.
    // The @CacheEvict annotation ensures that the cache is cleared before the
    // method
    // is invoked.
    // No implementation needed here, as the annotation handles the cache eviction.
  }

  public Optional<Instant> getLatestMessageTimestamp(String swapRequestId) {
    return chatMessageRepository.findFirstBySwapRequestIdOrderBySentAtDesc(swapRequestId)
        .map(ChatMessageDao::getSentAt);
  }

  public SwapRequest getSwapRequestForChat(String swapRequestId, String userId) {
    // Validate swap request exists
    Optional<SwapRequestDao> swapRequestOpt = swapRequestRepository.findById(swapRequestId);
    if (swapRequestOpt.isEmpty()) {
      throw new SwapRequestNotFoundException();
    }

    SwapRequestDao swapRequest = swapRequestOpt.get();

    // Validate user has access to this chat (must be sender or receiver)
    if (!hasAccessToChat(swapRequest, userId)) {
      throw new ChatAccessDeniedException();
    }

    return SwapRequestMapper.toEntity(swapRequest);
  }

  private boolean hasAccessToChat(SwapRequestDao swapRequest, String userId) {
    return swapRequest.getSender().getId().equals(userId) ||
        swapRequest.getReceiver().getId().equals(userId);
  }

  private List<String> convertImageIdsToUrls(List<String> imageIds) {
    if (imageIds == null || imageIds.isEmpty()) {
      return null;
    }

    return imageIds.stream()
        .map(imageService::getDownloadUrl)
        .toList();
  }

  private void broadcastInboxUpdate(String userId) {
    try {
      // Send inbox refresh signal to user's WebSocket connection
      messagingTemplate.convertAndSendToUser(userId, "/queue/inbox/refresh", "refresh");
    } catch (Exception e) {
      // Log error but don't fail the message sending
      // Real-time updates are nice-to-have, not critical
    }
  }
}
