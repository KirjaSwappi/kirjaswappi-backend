/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kirjaswappi.backend.jpa.daos.ChatMessageDao;
import com.kirjaswappi.backend.jpa.daos.SwapRequestDao;
import com.kirjaswappi.backend.jpa.repositories.ChatMessageRepository;
import com.kirjaswappi.backend.jpa.repositories.SwapRequestRepository;
import com.kirjaswappi.backend.mapper.ChatMessageMapper;
import com.kirjaswappi.backend.service.entities.ChatMessage;
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

    // Get chat messages ordered by sent time
    List<ChatMessageDao> messageDaos = chatMessageRepository.findBySwapRequestIdOrderBySentAtAsc(swapRequestId);

    return messageDaos.stream()
        .map(ChatMessageMapper::toEntity)
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
    chatMessage.setMessage(message.trim());
    chatMessage.setReadByReceiver(false);

    // Save message
    ChatMessageDao messageDao = ChatMessageMapper.toDao(chatMessage);
    ChatMessageDao savedDao = chatMessageRepository.save(messageDao);

    // Clear unread count cache for the receiver
    String receiverId = swapRequest.getSender().getId().equals(senderId)
        ? swapRequest.getReceiver().getId()
        : swapRequest.getSender().getId();
    clearUnreadCountCache(receiverId, swapRequestId);

    return ChatMessageMapper.toEntity(savedDao);
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
    List<ChatMessageDao> unreadMessages = chatMessageRepository.findBySwapRequestIdOrderBySentAtAsc(swapRequestId)
        .stream()
        .filter(msg -> !msg.getSender().getId().equals(userId) && !msg.isReadByReceiver())
        .toList();

    // Mark messages as read
    for (ChatMessageDao message : unreadMessages) {
      message.setReadByReceiver(true);
      chatMessageRepository.save(message);
    }

    // Clear unread count cache for the user
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
    return chatMessageRepository.countBySwapRequestIdAndSenderIdNotAndReadByReceiverFalse(swapRequestId, userId);
  }

  @CacheEvict(value = "unreadCounts", key = "#userId + '_' + #swapRequestId")
  public void clearUnreadCountCache(String userId, String swapRequestId) {
    // Cache eviction method - implementation is handled by Spring AOP
  }

  private boolean hasAccessToChat(SwapRequestDao swapRequest, String userId) {
    return swapRequest.getSender().getId().equals(userId) ||
        swapRequest.getReceiver().getId().equals(userId);
  }
}