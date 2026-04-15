/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.bson.types.ObjectId;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.kirjaswappi.backend.common.service.ImageService;
import com.kirjaswappi.backend.common.service.ProfanityFilterService;
import com.kirjaswappi.backend.jpa.daos.ChatMessageDao;
import com.kirjaswappi.backend.jpa.daos.SwapRequestDao;
import com.kirjaswappi.backend.jpa.repositories.ChatMessageRepository;
import com.kirjaswappi.backend.jpa.repositories.SwapRequestRepository;
import com.kirjaswappi.backend.mapper.ChatMessageMapper;
import com.kirjaswappi.backend.mapper.SwapRequestMapper;
import com.kirjaswappi.backend.service.entities.ChatMessage;
import com.kirjaswappi.backend.service.entities.SwapRequest;
import com.kirjaswappi.backend.service.entities.User;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;
import com.kirjaswappi.backend.service.exceptions.ChatAccessDeniedException;
import com.kirjaswappi.backend.service.exceptions.SwapRequestNotFoundException;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatService {

  private final ChatMessageRepository chatMessageRepository;

  private final SwapRequestRepository swapRequestRepository;

  private final UserService userService;

  private final ImageService imageService;

  private final SimpMessagingTemplate messagingTemplate;

  private final ProfanityFilterService profanityFilterService;

  private final org.springframework.cache.CacheManager cacheManager;

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
          List<String> imageUrls = convertImageIdsToUrls(dao.imageIds());
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
      throw new BadRequestException("messageCannotBeBlank");
    }

    // Get sender user
    User sender = userService.getUser(senderId);

    // Create chat message
    var chatMessage = ChatMessage.builder()
        .swapRequestId(swapRequestId)
        .sender(sender)
        .message(profanityFilterService.filter(message.trim()))
        .readByReceiver(false)
        .build();

    // Save message
    ChatMessageDao messageDao = ChatMessageMapper.toDao(chatMessage);
    ChatMessageDao savedDao = chatMessageRepository.save(messageDao);

    // Clear unread count cache for both users to ensure consistency
    String receiverId = swapRequest.sender().id().equals(senderId)
        ? swapRequest.receiver().id()
        : swapRequest.sender().id();

    // Reset the receiver's read timestamp so the inbox shows unread
    if (swapRequest.receiver().id().equals(receiverId)) {
      swapRequest.readByReceiverAt(null);
    } else {
      swapRequest.readBySenderAt(null);
    }
    swapRequestRepository.save(swapRequest);

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
      throw new BadRequestException("messageOrImageRequired");
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
      long maxImageSize = 5 * 1024 * 1024; // 5MB per image
      for (MultipartFile image : images) {
        if (image.getSize() > maxImageSize) {
          throw new BadRequestException("imageSizeExceedsLimit");
        }
        String uniqueId = UUID.randomUUID().toString();
        imageService.uploadImage(image, uniqueId);
        imageIds.add(uniqueId);
      }
    }

    // Create chat message
    var chatMessage = ChatMessage.builder()
        .swapRequestId(swapRequestId)
        .sender(sender)
        .message(message != null && !message.trim().isEmpty() ? profanityFilterService.filter(message.trim()) : null)
        .imageIds(imageIds.isEmpty() ? null : imageIds)

        .readByReceiver(false)
        .build();

    // Save message
    ChatMessageDao messageDao = ChatMessageMapper.toDao(chatMessage);
    ChatMessageDao savedDao = chatMessageRepository.save(messageDao);

    // Clear unread count cache for both users to ensure consistency
    String receiverId = swapRequest.sender().id().equals(senderId)
        ? swapRequest.receiver().id()
        : swapRequest.sender().id();

    // Reset the receiver's read timestamp so the inbox shows unread
    if (swapRequest.receiver().id().equals(receiverId)) {
      swapRequest.readByReceiverAt(null);
    } else {
      swapRequest.readBySenderAt(null);
    }
    swapRequestRepository.save(swapRequest);

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

    // Bulk-update unread messages in a single DB operation
    chatMessageRepository.markAsRead(swapRequestId, userId);

    // Also mark the swap request inbox item as read so the inbox endpoint returns
    // unread=false
    Instant now = Instant.now();
    if (swapRequest.receiver().id().equals(userId)) {
      swapRequest.readByReceiverAt(now);
      swapRequestRepository.save(swapRequest);
    } else if (swapRequest.sender().id().equals(userId)) {
      swapRequest.readBySenderAt(now);
      swapRequestRepository.save(swapRequest);
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
        swapRequestId, new ObjectId(userId));
  }

  public void clearUnreadCountCache(String userId, String swapRequestId) {
    if (cacheManager != null) {
      org.springframework.cache.Cache cache = cacheManager.getCache("unreadCounts");
      if (cache != null) {
        cache.evict(userId + "_" + swapRequestId);
      }
    }
  }

  public Optional<Instant> getLatestMessageTimestamp(String swapRequestId) {
    return chatMessageRepository.findFirstBySwapRequestIdOrderBySentAtDesc(swapRequestId)
        .map(ChatMessageDao::sentAt);
  }

  public Map<String, Instant> getLatestMessageTimestamps(List<String> swapRequestIds) {
    if (swapRequestIds == null || swapRequestIds.isEmpty()) {
      return Map.of();
    }
    List<ChatMessageDao> messages = chatMessageRepository.findBySwapRequestIdInOrderBySentAtDesc(swapRequestIds);
    Map<String, Instant> result = new HashMap<>();
    for (ChatMessageDao msg : messages) {
      result.putIfAbsent(msg.swapRequestId(), msg.sentAt());
    }
    return result;
  }

  public Map<String, Long> getBatchUnreadMessageCounts(List<String> swapRequestIds, String userId) {
    if (swapRequestIds == null || swapRequestIds.isEmpty()) {
      return Map.of();
    }
    List<ChatMessageDao> unreadMessages = chatMessageRepository
        .findUnreadBySwapRequestIdInAndSenderIdNot(swapRequestIds, new ObjectId(userId));
    Map<String, Long> result = new HashMap<>();
    for (ChatMessageDao msg : unreadMessages) {
      result.merge(msg.swapRequestId(), 1L, Long::sum);
    }
    return result;
  }

  public Optional<ChatMessage> getLatestMessage(String swapRequestId) {
    return chatMessageRepository.findFirstBySwapRequestIdOrderBySentAtDesc(swapRequestId)
        .map(dao -> {
          List<String> imageUrls = convertImageIdsToUrls(dao.imageIds());
          return ChatMessageMapper.toEntity(dao, imageUrls);
        });
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
    return swapRequest.sender().id().equals(userId) ||
        swapRequest.receiver().id().equals(userId);
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
      if (TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            try {
              messagingTemplate.convertAndSendToUser(userId, "/queue/inbox.refresh", "refresh");
            } catch (Exception e) {
              log.debug("Failed to broadcast inbox update to user {}: {}", userId, e.getMessage());
            }
          }
        });
      } else {
        messagingTemplate.convertAndSendToUser(userId, "/queue/inbox.refresh", "refresh");
      }
    } catch (Exception e) {
      log.debug("Failed to register inbox broadcast for user {}: {}", userId, e.getMessage());
    }
  }
}
