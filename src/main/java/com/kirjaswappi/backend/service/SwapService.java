/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kirjaswappi.backend.common.service.NotificationClient;
import com.kirjaswappi.backend.jpa.daos.SwapRequestDao;
import com.kirjaswappi.backend.jpa.repositories.SwapRequestRepository;
import com.kirjaswappi.backend.jpa.repositories.UserRepository;
import com.kirjaswappi.backend.mapper.SwapRequestMapper;
import com.kirjaswappi.backend.service.entities.*;
import com.kirjaswappi.backend.service.enums.SwapStatus;
import com.kirjaswappi.backend.service.exceptions.IllegalSwapRequestException;
import com.kirjaswappi.backend.service.exceptions.InvalidStatusTransitionException;
import com.kirjaswappi.backend.service.exceptions.SwapRequestExistsAlreadyException;
import com.kirjaswappi.backend.service.exceptions.SwapRequestNotFoundException;

@Service
@Transactional
@RequiredArgsConstructor
public class SwapService {
  private static final Logger logger = LoggerFactory.getLogger(SwapService.class);

  private final UserService userService;

  private final UserRepository userRepository;

  private final BookService bookService;

  private final GenreService genreService;

  private final SwapRequestRepository swapRequestRepository;

  private final NotificationClient notificationClient;

  private final PhotoService photoService;

  public SwapRequest createSwapRequest(SwapRequest swapRequest) {
    // validation: check if the swap request exists already for this book
    if (swapRequestRepository.existsAlready(swapRequest.sender().id(),
        swapRequest.receiver().id(), swapRequest.bookToSwapWith().id())) {
      throw new SwapRequestExistsAlreadyException();
    }

    // validation: check if the user is trying to swap their own book
    if (swapRequest.sender().id().equals(swapRequest.receiver().id())) {
      throw new IllegalSwapRequestException("senderAndReceiverCannotBeSame");
    }

    // set sender:
    User sender = userService.getUser(swapRequest.sender().id());
    // set receiver:
    User receiver = userService.getUser(swapRequest.receiver().id());

    // validation: receiver has blocked the sender — refuse the request.
    boolean blocked = userRepository.findById(receiver.id())
        .map(dao -> dao.blockedUserIds())
        .map(list -> list != null && list.contains(sender.id()))
        .orElse(false);
    if (blocked) {
      throw new IllegalSwapRequestException("receiverHasBlockedSender");
    }
    // set bookToSwapWith:
    Book bookToSwapWith = bookService.getBookById(swapRequest.bookToSwapWith().id());

    // check if the bookToSwapWith belongs to the receiver:
    if (Optional.ofNullable(receiver.books())
        .stream()
        .flatMap(Collection::stream)
        .noneMatch(book -> book.id().equals(bookToSwapWith.id()))) {
      throw new IllegalSwapRequestException("bookToSwapWithDoesNotBelongToReceiver");
    }

    if (swapRequest.swapOffer() != null) {
      // set offeredBook if present:
      if (swapRequest.swapOffer().offeredBook() != null) {
        SwappableBook offeredBook = bookService
            .getSwappableBookById(swapRequest.swapOffer().offeredBook().getId());

        // check if the offeredBook is present as one of the swappableBooks conditions
        // of bookToSwapWith:
        if (Optional.ofNullable(bookToSwapWith.swapCondition().swappableBooks())
            .stream()
            .flatMap(Collection::stream)
            .noneMatch(book -> book.getId().equals(offeredBook.getId()))) {
          throw new IllegalSwapRequestException("offeredBookDoesNotBelongToOneOfTheSwappableBooks");
        }

        swapRequest.swapOffer().offeredBook(offeredBook);
      }

      // set offeredGenre if present:
      if (swapRequest.swapOffer().offeredGenre() != null) {
        Genre offeredGenre = genreService.getGenreById(swapRequest.swapOffer().offeredGenre().getId());

        // check if the offeredGenre is present as one of the swappableGenres conditions
        // of bookToSwapWith:
        if (Optional.ofNullable(bookToSwapWith.swapCondition().swappableGenres())
            .stream()
            .flatMap(Collection::stream)
            .noneMatch(genre -> genre.getId().equals(offeredGenre.getId()))) {
          throw new IllegalSwapRequestException("offeredGenreDoesNotBelongToOneOfTheSwappableGenres");
        }

        swapRequest.swapOffer().offeredGenre(offeredGenre);
      }
    }

    var updatedSwapRequest = swapRequest.withSender(sender)
        .withReceiver(receiver)
        .withBookToSwapWith(bookToSwapWith)
        .withSwapStatus(SwapStatus.PENDING);

    SwapRequestDao dao = SwapRequestMapper.toDao(updatedSwapRequest);
    SwapRequestDao createdDao = swapRequestRepository.save(dao);

    // Send notification to receiver about new swap request
    try {
      String notificationTitle = "New Swap Request";
      String notificationMessage = String.format("%s %s wants to swap for your book '%s'",
          sender.firstName(), sender.lastName(), bookToSwapWith.title());

      notificationClient.sendNotification(receiver.id(), notificationTitle, notificationMessage);
    } catch (Exception e) {
      // Log error but don't fail the swap request creation
      logger.error("Failed to send notification for new swap request. Receiver: {}, Book: {}",
          receiver.id(), bookToSwapWith.title(), e);
    }

    return resolveCoverPhotoUrls(SwapRequestMapper.toEntity(createdDao));
  }

  // Used only by admin
  public void deleteAllSwapRequests() {
    swapRequestRepository.deleteAll();
  }

  public SwapRequest updateSwapRequestStatus(String swapRequestId, SwapStatus newStatus, String userId) {
    // Find the swap request
    Optional<SwapRequestDao> swapRequestDaoOpt = swapRequestRepository.findById(swapRequestId);
    if (swapRequestDaoOpt.isEmpty()) {
      throw new SwapRequestNotFoundException(swapRequestId);
    }

    SwapRequestDao swapRequestDao = swapRequestDaoOpt.get();
    SwapRequest swapRequest = SwapRequestMapper.toEntity(swapRequestDao);

    // Validate that the user is a participant
    boolean isSender = swapRequest.sender().id().equals(userId);
    boolean isReceiver = swapRequest.receiver().id().equals(userId);

    if (!isSender && !isReceiver) {
      throw new IllegalSwapRequestException("onlyParticipantsCanChangeStatus");
    }

    // Only receivers can accept/reject/reserve; only senders can expire
    if ((newStatus == SwapStatus.ACCEPTED || newStatus == SwapStatus.REJECTED || newStatus == SwapStatus.RESERVED)
        && !isReceiver) {
      throw new IllegalSwapRequestException("onlyReceiverCanChangeStatus");
    }
    if (newStatus == SwapStatus.EXPIRED && !isSender) {
      throw new IllegalSwapRequestException("onlySenderCanExpire");
    }

    SwapStatus currentStatus = swapRequest.swapStatus();

    // Validate status transition
    if (!currentStatus.canTransitionTo(newStatus)) {
      throw new InvalidStatusTransitionException(currentStatus.getCode(), newStatus.getCode());
    }

    // Update the status (preserve @Version on the existing DAO so optimistic
    // locking catches concurrent updates).
    swapRequestDao.swapStatus(newStatus.getCode());
    swapRequestDao.updatedAt(java.time.Instant.now());
    SwapRequestDao updatedDao = swapRequestRepository.save(swapRequestDao);

    // Notify the *counterparty* (the user who did NOT initiate this transition).
    // Receiver-only transitions (ACCEPTED/REJECTED/RESERVED) inform the sender;
    // sender-only transitions (EXPIRED) and either-party transitions
    // (COMPLETED/CANCELLED) inform the other participant.
    String counterpartyId = isSender ? swapRequest.receiver().id() : swapRequest.sender().id();
    try {
      String notificationTitle = "Swap Request Update";
      String notificationMessage = String.format("Swap request for '%s' has been %s",
          swapRequest.bookToSwapWith().title(),
          newStatus.getCode().toLowerCase());

      notificationClient.sendNotification(counterpartyId, notificationTitle, notificationMessage);
    } catch (Exception e) {
      // Log error but don't fail the status update
      logger.error("Failed to send notification for swap request status update. Counterparty: {}, Status: {}",
          counterpartyId, newStatus.getCode(), e);
    }

    return resolveCoverPhotoUrls(SwapRequestMapper.toEntity(updatedDao));
  }

  /**
   * Replace storage IDs on the embedded book's cover photos with presigned URLs.
   * Without this, swap responses leak raw S3 keys to clients.
   */
  private SwapRequest resolveCoverPhotoUrls(SwapRequest swapRequest) {
    if (swapRequest == null || swapRequest.bookToSwapWith() == null) {
      return swapRequest;
    }
    List<String> rawCovers = swapRequest.bookToSwapWith().coverPhotos();
    if (rawCovers == null || rawCovers.isEmpty()) {
      return swapRequest;
    }
    List<String> resolved = new ArrayList<>(rawCovers.size());
    for (String uniqueId : rawCovers) {
      resolved.add(photoService.getBookCoverPhoto(uniqueId));
    }
    return swapRequest.withBookToSwapWith(swapRequest.bookToSwapWith().withCoverPhotos(resolved));
  }
}
