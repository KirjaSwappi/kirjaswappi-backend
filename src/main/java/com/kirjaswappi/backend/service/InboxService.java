/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kirjaswappi.backend.events.InboxUpdateEvent;
import com.kirjaswappi.backend.jpa.daos.SwapRequestDao;
import com.kirjaswappi.backend.jpa.repositories.SwapRequestRepository;
import com.kirjaswappi.backend.mapper.SwapRequestMapper;
import com.kirjaswappi.backend.service.entities.SwapRequest;
import com.kirjaswappi.backend.service.enums.SwapStatus;
import com.kirjaswappi.backend.service.exceptions.InvalidStatusTransitionException;
import com.kirjaswappi.backend.service.exceptions.SwapRequestNotFoundException;

@Service
@Transactional
@RequiredArgsConstructor
public class InboxService {

  private final SwapRequestRepository swapRequestRepository;

  private final UserService userService;

  private final ChatService chatService;

  private final ApplicationEventPublisher eventPublisher;

  public List<SwapRequest> getUnifiedInbox(String userId, String status, String sortBy) {
    // Validate user exists
    userService.getUser(userId);

    List<SwapRequestDao> allSwapRequestDaos = new ArrayList<>();

    if (status != null && !status.trim().isEmpty()) {
      // Validate status
      SwapStatus.fromCode(status); // This will throw BadRequestException if invalid

      // Get both sent and received with status filter
      List<SwapRequestDao> receivedDaos = swapRequestRepository
          .findByReceiverIdAndSwapStatusOrderByRequestedAtDesc(userId, status);
      List<SwapRequestDao> sentDaos = swapRequestRepository.findBySenderIdAndSwapStatusOrderByRequestedAtDesc(userId,
          status);

      allSwapRequestDaos.addAll(receivedDaos);
      allSwapRequestDaos.addAll(sentDaos);
    } else {
      // Get all sent and received without status filter
      List<SwapRequestDao> receivedDaos = swapRequestRepository.findByReceiverIdOrderByRequestedAtDesc(userId);
      List<SwapRequestDao> sentDaos = swapRequestRepository.findBySenderIdOrderByRequestedAtDesc(userId);

      allSwapRequestDaos.addAll(receivedDaos);
      allSwapRequestDaos.addAll(sentDaos);
    }

    List<SwapRequest> swapRequests = allSwapRequestDaos.stream()
        .map(SwapRequestMapper::toEntity)
        .toList();

    return applySorting(swapRequests, sortBy);
  }

  public SwapRequest getInboxItem(String userId, String swapRequestId) {
    // Validate user exists
    userService.getUser(userId);

    return swapRequestRepository.findById(swapRequestId)
        .map(SwapRequestMapper::toEntity)
        .filter(request -> request.sender().id().equals(userId) || request.receiver().id().equals(userId))
        .orElseThrow(() -> new SwapRequestNotFoundException());
  }

  public SwapRequest updateSwapRequestStatus(String swapRequestId, String newStatus, String userId) {
    // Validate new status
    SwapStatus newSwapStatus = SwapStatus.fromCode(newStatus);

    // Get swap request
    Optional<SwapRequestDao> swapRequestOpt = swapRequestRepository.findById(swapRequestId);
    if (swapRequestOpt.isEmpty()) {
      throw new SwapRequestNotFoundException();
    }

    SwapRequestDao swapRequestDao = swapRequestOpt.get();

    // Validate user has permission to update (must be receiver for most status
    // changes)
    if (!canUpdateStatus(swapRequestDao, userId, newSwapStatus)) {
      throw new IllegalArgumentException("User not authorized to update this swap request status");
    }

    // Validate status transition
    SwapStatus currentStatus = SwapStatus.fromCode(swapRequestDao.swapStatus());
    if (!currentStatus.canTransitionTo(newSwapStatus)) {
      throw new InvalidStatusTransitionException(currentStatus.getCode(), newSwapStatus.getCode());
    }

    // Update status
    swapRequestDao.swapStatus(newSwapStatus.getCode());
    SwapRequestDao updatedDao = swapRequestRepository.save(swapRequestDao);

    // Clear unread count cache for both users when status changes
    clearUnreadCountCache(swapRequestDao.sender().id(), swapRequestId);
    clearUnreadCountCache(swapRequestDao.receiver().id(), swapRequestId);

    // Publish inbox update events for real-time status changes
    eventPublisher.publishEvent(new InboxUpdateEvent(
        swapRequestDao.sender().id(),
        swapRequestId,
        InboxUpdateEvent.STATUS_CHANGE));
    eventPublisher.publishEvent(new InboxUpdateEvent(
        swapRequestDao.receiver().id(),
        swapRequestId,
        InboxUpdateEvent.STATUS_CHANGE));

    return SwapRequestMapper.toEntity(updatedDao);
  }

  public Optional<Instant> getLatestMessageTimestamp(String swapRequestId) {
    return chatService.getLatestMessageTimestamp(swapRequestId);
  }

  public Optional<com.kirjaswappi.backend.service.entities.ChatMessage> getLatestMessage(String swapRequestId) {
    return chatService.getLatestMessage(swapRequestId);
  }

  public long getUnreadMessageCount(String userId, String swapRequestId) {
    return chatService.getUnreadMessageCount(swapRequestId, userId);
  }

  public Map<String, Long> getBatchUnreadMessageCounts(String userId, List<String> swapRequestIds) {
    return chatService.getBatchUnreadMessageCounts(swapRequestIds, userId);
  }

  public void markInboxItemAsRead(String swapRequestId, String userId) {
    // Get swap request
    Optional<SwapRequestDao> swapRequestOpt = swapRequestRepository.findById(swapRequestId);
    if (swapRequestOpt.isEmpty()) {
      throw new SwapRequestNotFoundException();
    }

    SwapRequestDao swapRequestDao = swapRequestOpt.get();
    Instant now = Instant.now();

    // Always update the read timestamp
    if (swapRequestDao.receiver().id().equals(userId)) {
      swapRequestDao.readByReceiverAt(now);
      swapRequestRepository.save(swapRequestDao);
    } else if (swapRequestDao.sender().id().equals(userId)) {
      swapRequestDao.readBySenderAt(now);
      swapRequestRepository.save(swapRequestDao);
    }
  }

  public boolean isInboxItemUnread(SwapRequest swapRequest, String userId) {
    if (swapRequest.receiver().id().equals(userId)) {
      return swapRequest.readByReceiverAt() == null;
    } else if (swapRequest.sender().id().equals(userId)) {
      return swapRequest.readBySenderAt() == null;
    }
    return false;
  }

  @CacheEvict(value = "unreadCounts", key = "#userId + '_' + #swapRequestId", beforeInvocation = true)
  public void clearUnreadCountCache(String userId, String swapRequestId) {
    // This method is used to clear the cache for unread message counts.
    // The @CacheEvict annotation ensures that the cache is cleared before the
    // method is invoked.
    // No implementation needed here, just the annotation is sufficient.
  }

  private List<SwapRequest> applySorting(List<SwapRequest> swapRequests, String sortBy) {
    if (sortBy == null || sortBy.trim().isEmpty() || "latest_message".equalsIgnoreCase(sortBy)) {
      return sortByLatestMessage(swapRequests);
    }

    return switch (sortBy.toLowerCase()) {
    case "date" -> swapRequests.stream()
        .sorted(Comparator.comparing(SwapRequest::requestedAt).reversed())
        .toList();
    case "book_title" -> swapRequests.stream()
        .sorted(Comparator.comparing(sr -> sr.bookToSwapWith().title(), String.CASE_INSENSITIVE_ORDER))
        .toList();
    case "sender_name" -> swapRequests.stream()
        .sorted(Comparator.comparing(sr -> sr.sender().firstName() + " " + sr.sender().lastName(),
            String.CASE_INSENSITIVE_ORDER))
        .toList();
    case "status" -> swapRequests.stream()
        .sorted(Comparator.comparing(sr -> sr.swapStatus().getCode(), String.CASE_INSENSITIVE_ORDER))
        .toList();
    default -> sortByLatestMessage(swapRequests);
    };
  }

  private List<SwapRequest> sortByLatestMessage(List<SwapRequest> swapRequests) {
    // Fetch latest message timestamps for all swap requests before sorting.
    List<String> swapRequestIds = swapRequests.stream().map(SwapRequest::id).toList();
    Map<String, Instant> timestampMap = chatService.getLatestMessageTimestamps(swapRequestIds);

    return swapRequests.stream()
        .sorted((sr1, sr2) -> {
          Instant timestamp1 = timestampMap.get(sr1.id());
          Instant timestamp2 = timestampMap.get(sr2.id());

          if (timestamp1 != null && timestamp2 != null) {
            return timestamp2.compareTo(timestamp1);
          }
          if (timestamp1 != null)
            return -1;
          if (timestamp2 != null)
            return 1;
          return sr2.requestedAt().compareTo(sr1.requestedAt());
        })
        .toList();
  }

  private boolean canUpdateStatus(SwapRequestDao swapRequest, String userId, SwapStatus newStatus) {
    boolean isReceiver = swapRequest.receiver().id().equals(userId);
    boolean isSender = swapRequest.sender().id().equals(userId);

    if (!isReceiver && !isSender) {
      return false;
    }

    // Accept/Reject/Reserve are receiver-only actions
    if (newStatus == SwapStatus.ACCEPTED || newStatus == SwapStatus.REJECTED
        || newStatus == SwapStatus.RESERVED) {
      return isReceiver;
    }

    // Complete and Cancel can be done by either participant
    if (newStatus == SwapStatus.COMPLETED || newStatus == SwapStatus.CANCELLED) {
      return true;
    }

    // Expired can be triggered by sender (withdraw) or system
    if (newStatus == SwapStatus.EXPIRED) {
      return isSender;
    }

    return false;
  }
}
