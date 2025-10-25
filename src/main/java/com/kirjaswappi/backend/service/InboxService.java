/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.kirjaswappi.backend.service.exceptions.SwapRequestNotFoundException;

@Service
@Transactional
public class InboxService {
  @Autowired
  private SwapRequestRepository swapRequestRepository;

  @Autowired
  private UserService userService;

  @Autowired
  private ChatService chatService;

  @Autowired
  private ApplicationEventPublisher eventPublisher;

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
    SwapStatus currentStatus = SwapStatus.fromCode(swapRequestDao.getSwapStatus());
    if (!isValidStatusTransition(currentStatus, newSwapStatus)) {
      throw new IllegalArgumentException(
          "Invalid status transition from " + currentStatus.getCode() + " to " + newSwapStatus.getCode());
    }

    // Update status
    swapRequestDao.setSwapStatus(newSwapStatus.getCode());
    SwapRequestDao updatedDao = swapRequestRepository.save(swapRequestDao);

    // Clear unread count cache for both users when status changes
    clearUnreadCountCache(swapRequestDao.getSender().getId(), swapRequestId);
    clearUnreadCountCache(swapRequestDao.getReceiver().getId(), swapRequestId);

    // Publish inbox update events for real-time status changes
    eventPublisher.publishEvent(new InboxUpdateEvent(
        swapRequestDao.getSender().getId(),
        swapRequestId,
        InboxUpdateEvent.STATUS_CHANGE));
    eventPublisher.publishEvent(new InboxUpdateEvent(
        swapRequestDao.getReceiver().getId(),
        swapRequestId,
        InboxUpdateEvent.STATUS_CHANGE));

    return SwapRequestMapper.toEntity(updatedDao);
  }

  public long getUnreadMessageCount(String userId, String swapRequestId) {
    return chatService.getUnreadMessageCount(swapRequestId, userId);
  }

  public void markInboxItemAsRead(String swapRequestId, String userId) {
    // Get swap request
    Optional<SwapRequestDao> swapRequestOpt = swapRequestRepository.findById(swapRequestId);
    if (swapRequestOpt.isEmpty()) {
      throw new SwapRequestNotFoundException();
    }

    SwapRequestDao swapRequestDao = swapRequestOpt.get();
    Instant now = Instant.now();
    boolean updated = false;

    // Mark as read by the appropriate user
    if (swapRequestDao.getReceiver().getId().equals(userId) && swapRequestDao.getReadByReceiverAt() == null) {
      swapRequestDao.setReadByReceiverAt(now);
      updated = true;
    } else if (swapRequestDao.getSender().getId().equals(userId) && swapRequestDao.getReadBySenderAt() == null) {
      swapRequestDao.setReadBySenderAt(now);
      updated = true;
    }

    // Save if updated
    if (updated) {
      swapRequestRepository.save(swapRequestDao);
    }
  }

  public boolean isInboxItemUnread(SwapRequest swapRequest, String userId) {
    if (swapRequest.getReceiver().getId().equals(userId)) {
      return swapRequest.getReadByReceiverAt() == null;
    } else if (swapRequest.getSender().getId().equals(userId)) {
      return swapRequest.getReadBySenderAt() == null;
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
      // Sort by latest message timestamp, fallback to request date if no messages
      return swapRequests.stream()
          .sorted((sr1, sr2) -> {
            Optional<Instant> timestamp1 = chatService.getLatestMessageTimestamp(sr1.getId());
            Optional<Instant> timestamp2 = chatService.getLatestMessageTimestamp(sr2.getId());

            // If both have messages, compare by latest message timestamp (desc)
            if (timestamp1.isPresent() && timestamp2.isPresent()) {
              return timestamp2.get().compareTo(timestamp1.get());
            }
            // If only one has messages, prioritize the one with messages
            if (timestamp1.isPresent())
              return -1;
            if (timestamp2.isPresent())
              return 1;
            // If neither has messages, sort by request date (desc)
            return sr2.getRequestedAt().compareTo(sr1.getRequestedAt());
          })
          .toList();
    }

    return switch (sortBy.toLowerCase()) {
    case "date" -> swapRequests.stream()
        .sorted(Comparator.comparing(SwapRequest::getRequestedAt).reversed())
        .toList();
    case "book_title" -> swapRequests.stream()
        .sorted(Comparator.comparing(sr -> sr.getBookToSwapWith().getTitle(), String.CASE_INSENSITIVE_ORDER))
        .toList();
    case "sender_name" -> swapRequests.stream()
        .sorted(Comparator.comparing(sr -> sr.getSender().getFirstName() + " " + sr.getSender().getLastName(),
            String.CASE_INSENSITIVE_ORDER))
        .toList();
    case "status" -> swapRequests.stream()
        .sorted(Comparator.comparing(sr -> sr.getSwapStatus().getCode(), String.CASE_INSENSITIVE_ORDER))
        .toList();
    default -> sortByLatestMessage(swapRequests);
    };
  }

  /**
   * Sorts swap requests by latest message timestamp (descending), falling back to
   * request date if no messages.
   */
  private List<SwapRequest> sortByLatestMessage(List<SwapRequest> swapRequests) {
    return swapRequests.stream()
        .sorted((sr1, sr2) -> {
          Optional<Instant> timestamp1 = chatService.getLatestMessageTimestamp(sr1.getId());
          Optional<Instant> timestamp2 = chatService.getLatestMessageTimestamp(sr2.getId());

          // If both have messages, compare by latest message timestamp (desc)
          if (timestamp1.isPresent() && timestamp2.isPresent()) {
            return timestamp2.get().compareTo(timestamp1.get());
          }
          // If only one has messages, prioritize the one with messages
          if (timestamp1.isPresent())
            return -1;
          if (timestamp2.isPresent())
            return 1;
          // If neither has messages, sort by request date (desc)
          return sr2.getRequestedAt().compareTo(sr1.getRequestedAt());
        })
        .toList();
  }

  private boolean canUpdateStatus(SwapRequestDao swapRequest, String userId, SwapStatus newStatus) {
    // Receiver can accept, reject, or mark as reserved
    if (swapRequest.getReceiver().getId().equals(userId)) {
      return newStatus == SwapStatus.ACCEPTED ||
          newStatus == SwapStatus.REJECTED ||
          newStatus == SwapStatus.RESERVED;
    }

    // Sender can only mark as expired (cancel their own request)
    if (swapRequest.getSender().getId().equals(userId)) {
      return newStatus == SwapStatus.EXPIRED;
    }

    return false;
  }

  private boolean isValidStatusTransition(SwapStatus currentStatus, SwapStatus newStatus) {
    return switch (currentStatus) {
    case PENDING -> newStatus == SwapStatus.ACCEPTED ||
        newStatus == SwapStatus.REJECTED ||
        newStatus == SwapStatus.EXPIRED;
    case ACCEPTED -> newStatus == SwapStatus.RESERVED ||
        newStatus == SwapStatus.EXPIRED;
    case RESERVED -> newStatus == SwapStatus.EXPIRED;
    case REJECTED, EXPIRED -> false; // Terminal states
    };
  }
}
