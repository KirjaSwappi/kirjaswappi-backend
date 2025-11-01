/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service;

import java.util.Collection;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kirjaswappi.backend.common.service.NotificationService;
import com.kirjaswappi.backend.jpa.daos.SwapRequestDao;
import com.kirjaswappi.backend.jpa.repositories.SwapRequestRepository;
import com.kirjaswappi.backend.mapper.SwapRequestMapper;
import com.kirjaswappi.backend.service.entities.*;
import com.kirjaswappi.backend.service.enums.SwapStatus;
import com.kirjaswappi.backend.service.exceptions.IllegalSwapRequestException;
import com.kirjaswappi.backend.service.exceptions.InvalidStatusTransitionException;
import com.kirjaswappi.backend.service.exceptions.SwapRequestExistsAlreadyException;
import com.kirjaswappi.backend.service.exceptions.SwapRequestNotFoundException;

@Service
@Transactional
public class SwapService {
  private static final Logger logger = LoggerFactory.getLogger(SwapService.class);

  @Autowired
  private UserService userService;

  @Autowired
  private BookService bookService;

  @Autowired
  private GenreService genreService;

  @Autowired
  private SwapRequestRepository swapRequestRepository;

  @Autowired
  private NotificationService notificationService;

  public SwapRequest createSwapRequest(SwapRequest swapRequest) {
    // validation: check if the swap request exists already for this book
    if (swapRequestRepository.existsAlready(swapRequest.getSender().getId(),
        swapRequest.getReceiver().getId(), swapRequest.getBookToSwapWith().getId())) {
      throw new SwapRequestExistsAlreadyException();
    }

    // validation: check if the user is trying to swap their own book
    if (swapRequest.getSender().getId().equals(swapRequest.getReceiver().getId())) {
      throw new IllegalSwapRequestException("senderAndReceiverCannotBeSame");
    }

    // set sender:
    User sender = userService.getUser(swapRequest.getSender().getId());
    swapRequest.setSender(sender);

    // set receiver:
    User receiver = userService.getUser(swapRequest.getReceiver().getId());
    swapRequest.setReceiver(receiver);

    // set bookToSwapWith:
    Book bookToSwapWith = bookService.getBookById(swapRequest.getBookToSwapWith().getId());
    swapRequest.setBookToSwapWith(bookToSwapWith);

    // check if the bookToSwapWith belongs to the receiver:
    if (Optional.ofNullable(receiver.getBooks())
        .stream()
        .flatMap(Collection::stream)
        .noneMatch(book -> book.getId().equals(bookToSwapWith.getId()))) {
      throw new IllegalSwapRequestException("bookToSwapWithDoesNotBelongToReceiver");
    }

    if (swapRequest.getSwapOffer() != null) {
      // set offeredBook if present:
      if (swapRequest.getSwapOffer().getOfferedBook() != null) {
        SwappableBook offeredBook = bookService
            .getSwappableBookById(swapRequest.getSwapOffer().getOfferedBook().getId());

        // check if the offeredBook is present as one of the swappableBooks conditions
        // of bookToSwapWith:
        if (Optional.ofNullable(bookToSwapWith.getSwapCondition().getSwappableBooks())
            .stream()
            .flatMap(Collection::stream)
            .noneMatch(book -> book.getId().equals(offeredBook.getId()))) {
          throw new IllegalSwapRequestException("offeredBookDoesNotBelongToOneOfTheSwappableBooks");
        }

        swapRequest.getSwapOffer().setOfferedBook(offeredBook);
      }

      // set offeredGenre if present:
      if (swapRequest.getSwapOffer().getOfferedGenre() != null) {
        Genre offeredGenre = genreService.getGenreById(swapRequest.getSwapOffer().getOfferedGenre().getId());

        // check if the offeredGenre is present as one of the swappableGenres conditions
        // of bookToSwapWith:
        if (Optional.ofNullable(bookToSwapWith.getSwapCondition().getSwappableGenres())
            .stream()
            .flatMap(Collection::stream)
            .noneMatch(genre -> genre.getId().equals(offeredGenre.getId()))) {
          throw new IllegalSwapRequestException("offeredGenreDoesNotBelongToOneOfTheSwappableGenres");
        }

        swapRequest.getSwapOffer().setOfferedGenre(offeredGenre);
      }
    }

    swapRequest.setSwapStatus(SwapStatus.PENDING);
    SwapRequestDao dao = SwapRequestMapper.toDao(swapRequest);
    SwapRequestDao createdDao = swapRequestRepository.save(dao);

    // Send notification to receiver about new swap request
    try {
      String notificationTitle = "New Swap Request";
      String notificationMessage = String.format("%s %s wants to swap for your book '%s'",
          sender.getFirstName(), sender.getLastName(), bookToSwapWith.getTitle());

      notificationService.sendNotification(receiver.getId(), notificationTitle, notificationMessage);
    } catch (Exception e) {
      // Log error but don't fail the swap request creation
      logger.error("Failed to send notification for new swap request. Receiver: {}, Book: {}",
          receiver.getId(), bookToSwapWith.getTitle(), e);
      // TODO: Consider adding retry mechanism or dead letter queue
    }

    return SwapRequestMapper.toEntity(createdDao);
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

    // Validate that the user is the receiver (only receivers can change status)
    if (!swapRequest.getReceiver().getId().equals(userId)) {
      throw new IllegalSwapRequestException("onlyReceiverCanChangeStatus");
    }

    SwapStatus currentStatus = swapRequest.getSwapStatus();

    // Validate status transition
    if (!isValidStatusTransition(currentStatus, newStatus)) {
      throw new InvalidStatusTransitionException(currentStatus.getCode(), newStatus.getCode());
    }

    // Update the status
    swapRequest.setSwapStatus(newStatus);
    swapRequestDao = SwapRequestMapper.toDao(swapRequest);
    SwapRequestDao updatedDao = swapRequestRepository.save(swapRequestDao);

    // Send notification to sender about status change
    try {
      String notificationTitle = "Swap Request Update";
      String notificationMessage = String.format("Your swap request for '%s' has been %s",
          swapRequest.getBookToSwapWith().getTitle(),
          newStatus.getCode().toLowerCase());

      notificationService.sendNotification(swapRequest.getSender().getId(), notificationTitle, notificationMessage);
    } catch (Exception e) {
      // Log error but don't fail the status update
      logger.error("Failed to send notification for swap request status update. Sender: {}, Status: {}",
          swapRequest.getSender().getId(), newStatus.getCode(), e);
      // TODO: Consider adding retry mechanism or dead letter queue
    }

    return SwapRequestMapper.toEntity(updatedDao);
  }

  private boolean isValidStatusTransition(SwapStatus currentStatus, SwapStatus newStatus) {
    // Only allow transitions from PENDING to ACCEPTED or REJECTED
    if (currentStatus == SwapStatus.PENDING) {
      return newStatus == SwapStatus.ACCEPTED || newStatus == SwapStatus.REJECTED;
    }

    // No other transitions are allowed
    return false;
  }
}
