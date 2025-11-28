/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.kirjaswappi.backend.common.service.NotificationService;
import com.kirjaswappi.backend.jpa.daos.BookDao;
import com.kirjaswappi.backend.jpa.daos.GenreDao;
import com.kirjaswappi.backend.jpa.daos.SwapConditionDao;
import com.kirjaswappi.backend.jpa.daos.SwapOfferDao;
import com.kirjaswappi.backend.jpa.daos.SwapRequestDao;
import com.kirjaswappi.backend.jpa.daos.SwappableBookDao;
import com.kirjaswappi.backend.jpa.daos.UserDao;
import com.kirjaswappi.backend.jpa.repositories.SwapRequestRepository;
import com.kirjaswappi.backend.service.entities.Book;
import com.kirjaswappi.backend.service.entities.Genre;
import com.kirjaswappi.backend.service.entities.SwapCondition;
import com.kirjaswappi.backend.service.entities.SwapOffer;
import com.kirjaswappi.backend.service.entities.SwapRequest;
import com.kirjaswappi.backend.service.entities.SwappableBook;
import com.kirjaswappi.backend.service.entities.User;
import com.kirjaswappi.backend.service.enums.Condition;
import com.kirjaswappi.backend.service.enums.Language;
import com.kirjaswappi.backend.service.enums.SwapStatus;
import com.kirjaswappi.backend.service.enums.SwapType;
import com.kirjaswappi.backend.service.exceptions.IllegalSwapRequestException;
import com.kirjaswappi.backend.service.exceptions.InvalidStatusTransitionException;
import com.kirjaswappi.backend.service.exceptions.SwapRequestExistsAlreadyException;
import com.kirjaswappi.backend.service.exceptions.SwapRequestNotFoundException;

class SwapServiceTest {
  @Mock
  private SwapRequestRepository swapRequestRepository;
  @Mock
  private UserService userService;
  @Mock
  private BookService bookService;
  @Mock
  private GenreService genreService;
  @Mock
  private NotificationService notificationService;
  @InjectMocks
  private SwapService swapService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  @DisplayName("Should throw SwapRequestExistsAlreadyException when swap request already exists")
  void createSwapRequestThrowsWhenExistsAlready() {
    var sender = User.builder()
        .id("senderId")
        .build();

    var receiver = User.builder()
        .id("receiverId")
        .build();

    var book = Book.builder()
        .id("bookId")
        .build();

    var swapRequest = SwapRequest.builder()
        .sender(sender)
        .receiver(receiver)
        .bookToSwapWith(book)
        .build();
    when(swapRequestRepository.existsAlready("senderId", "receiverId", "bookId")).thenReturn(true);
    assertThrows(SwapRequestExistsAlreadyException.class, () -> swapService.createSwapRequest(swapRequest));
  }

  @Test
  @DisplayName("Should create swap request successfully when not duplicate")
  void createSwapRequestSuccess() {
    var sender = User.builder()
        .id("senderId")
        .build();

    var receiver = User.builder()
        .id("receiverId")
        .books(java.util.List.of())
        .build();

    var book = Book.builder()
        .id("bookId")
        .swapCondition(null)
        .build();

    var swapRequest = SwapRequest.builder()
        .sender(sender)
        .receiver(receiver)
        .bookToSwapWith(book)
        .swapType(SwapType.BY_BOOKS)
        .swapStatus(SwapStatus.PENDING)
        .build();
    when(swapRequestRepository.existsAlready("senderId", "receiverId", "bookId")).thenReturn(false);
    when(userService.getUser("senderId")).thenReturn(sender);
    when(userService.getUser("receiverId")).thenReturn(receiver);
    when(bookService.getBookById("bookId")).thenReturn(book);
    when(swapRequestRepository.save(any())).thenReturn(null);
    // receiver has no books, so IllegalSwapRequestException expected
    assertThrows(IllegalSwapRequestException.class, () -> swapService.createSwapRequest(swapRequest));
  }

  @Test
  @DisplayName("Should delete all swap requests")
  void deleteAllSwapRequests() {
    swapService.deleteAllSwapRequests();
    verify(swapRequestRepository, times(1)).deleteAll();
  }

  @Test
  @DisplayName("Should throw when book to swap with does not belong to receiver")
  void createSwapRequestThrowsWhenBookNotBelongToReceiver() {
    var sender = User.builder()
        .id("senderId")
        .build();

    var otherBook = Book.builder()
        .id("otherBookId")
        .build();

    var receiver = User.builder()
        .id("receiverId")
        .books(List.of(otherBook))
        .build();

    var swapCondition = com.kirjaswappi.backend.service.entities.SwapCondition.builder()
        .swappableBooks(List.of())
        .swappableGenres(List.of())
        .build();

    var book = Book.builder()
        .id("bookId")
        .swapCondition(swapCondition)
        .build();

    var swapRequest = SwapRequest.builder()
        .sender(sender)
        .receiver(receiver)
        .bookToSwapWith(book)
        .swapType(SwapType.BY_BOOKS)
        .swapStatus(SwapStatus.PENDING)
        .build();
    when(swapRequestRepository.existsAlready("senderId", "receiverId", "bookId")).thenReturn(false);
    when(userService.getUser("senderId")).thenReturn(sender);
    when(userService.getUser("receiverId")).thenReturn(receiver);
    when(bookService.getBookById("bookId")).thenReturn(book);
    assertThrows(IllegalSwapRequestException.class, () -> swapService.createSwapRequest(swapRequest));
  }

  @Test
  @DisplayName("Should throw when offered book is not in swappable books")
  void createSwapRequestThrowsWhenOfferedBookNotSwappable() {
    var sender = User.builder()
        .id("senderId")
        .build();

    var swapCondition = SwapCondition.builder()
        .swappableBooks(List.of())
        .swappableGenres(List.of())
        .swapType(SwapType.BY_BOOKS)
        .build();

    var book = Book.builder()
        .id("bookId")
        .swapCondition(swapCondition)
        .build();

    var offeredBook = com.kirjaswappi.backend.service.entities.SwappableBook.builder()
        .id("offeredBookId")
        .build();

    var swapOffer = SwapOffer.builder()
        .offeredBook(offeredBook)
        .build();

    var receiver = User.builder()
        .id("receiverId")
        .books(List.of(book))
        .build();

    var swapRequest = SwapRequest.builder()
        .sender(sender)
        .receiver(receiver)
        .bookToSwapWith(book)
        .swapType(SwapType.BY_BOOKS)
        .swapStatus(SwapStatus.PENDING)
        .swapOffer(swapOffer)
        .build();
    when(swapRequestRepository.existsAlready("senderId", "receiverId", "bookId")).thenReturn(false);
    when(userService.getUser("senderId")).thenReturn(sender);
    when(userService.getUser("receiverId")).thenReturn(receiver);
    when(bookService.getBookById("bookId")).thenReturn(book);
    when(bookService.getSwappableBookById("offeredBookId")).thenReturn(offeredBook);
    assertThrows(IllegalSwapRequestException.class, () -> swapService.createSwapRequest(swapRequest));
  }

  @Test
  @DisplayName("Should throw when offered genre is not in swappable genres")
  void createSwapRequestThrowsWhenOfferedGenreNotSwappable() {
    var sender = User.builder()
        .id("senderId")
        .build();

    var swapCondition = SwapCondition.builder()
        .swappableGenres(List.of())
        .swappableBooks(List.of())
        .swapType(SwapType.BY_GENRES)
        .build();

    var book = Book.builder()
        .id("bookId")
        .swapCondition(swapCondition)
        .build();

    var offeredGenre = Genre.builder()
        .id("offeredGenreId")
        .build();

    var swapOffer = SwapOffer.builder()
        .offeredGenre(offeredGenre)
        .build();

    var receiver = User.builder()
        .id("receiverId")
        .books(List.of(book))
        .build();

    var swapRequest = SwapRequest.builder()
        .sender(sender)
        .receiver(receiver)
        .bookToSwapWith(book)
        .swapType(SwapType.BY_GENRES)
        .swapStatus(SwapStatus.PENDING)
        .swapOffer(swapOffer)
        .build();
    when(swapRequestRepository.existsAlready("senderId", "receiverId", "bookId")).thenReturn(false);
    when(userService.getUser("senderId")).thenReturn(sender);
    when(userService.getUser("receiverId")).thenReturn(receiver);
    when(bookService.getBookById("bookId")).thenReturn(book);
    when(genreService.getGenreById("offeredGenreId")).thenReturn(offeredGenre);
    assertThrows(IllegalSwapRequestException.class, () -> swapService.createSwapRequest(swapRequest));
  }

  @Test
  @DisplayName("Should create swap request with valid offered book")
  void createSwapRequestWithValidOfferedBook() {
    var sender = User.builder()
        .id("senderId")
        .build();

    var offeredBook = SwappableBook.builder()
        .id("offeredBookId")
        .build();

    var swapCondition = SwapCondition.builder()
        .swappableBooks(List.of(offeredBook))
        .swappableGenres(List.of())
        .swapType(SwapType.BY_BOOKS)
        .build();

    var book = Book.builder()
        .id("bookId")
        .language(Language.ENGLISH)
        .condition(Condition.GOOD)
        .swapCondition(swapCondition)
        .build();

    var swapOffer = SwapOffer.builder()
        .offeredBook(offeredBook)
        .build();

    var receiver = User.builder()
        .id("receiverId")
        .books(List.of(book))
        .build();

    var swapRequest = SwapRequest.builder()
        .sender(sender)
        .receiver(receiver)
        .bookToSwapWith(book)
        .swapType(SwapType.BY_BOOKS)
        .swapStatus(SwapStatus.PENDING)
        .swapOffer(swapOffer)
        .build();

    when(swapRequestRepository.existsAlready("senderId", "receiverId", "bookId")).thenReturn(false);

    when(userService.getUser("senderId")).thenReturn(sender);

    when(userService.getUser("receiverId")).thenReturn(receiver);

    when(bookService.getBookById("bookId")).thenReturn(book);

    when(bookService.getSwappableBookById("offeredBookId")).thenReturn(offeredBook);

    var mockUserDao = mock(UserDao.class);
    when(mockUserDao.id()).thenReturn("senderId");
    var mockReceiverDao = mock(UserDao.class);
    when(mockReceiverDao.id()).thenReturn("receiverId");
    var mockSwapRequestDao = mock(SwapRequestDao.class);
    when(mockSwapRequestDao.sender()).thenReturn(mockUserDao);
    when(mockSwapRequestDao.receiver()).thenReturn(mockReceiverDao);
    var mockBookDao = mock(BookDao.class);
    when(mockBookDao.id()).thenReturn("bookId");
    when(mockSwapRequestDao.bookToSwapWith()).thenReturn(mockBookDao);
    var mockSwapOfferDao = mock(SwapOfferDao.class);
    var mockSwappableBookDao = mock(SwappableBookDao.class);
    when(mockSwappableBookDao.id()).thenReturn("offeredBookId");
    when(mockSwapOfferDao.offeredBook()).thenReturn(mockSwappableBookDao);
    when(mockSwapRequestDao.swapOfferDao()).thenReturn(mockSwapOfferDao);
    when(mockBookDao.title()).thenReturn("title");
    when(mockBookDao.author()).thenReturn("author");
    when(mockBookDao.description()).thenReturn("desc");
    when(mockBookDao.language()).thenReturn("English");
    when(mockBookDao.condition()).thenReturn("New");
    when(mockBookDao.genres()).thenReturn(List.of());
    when(mockBookDao.coverPhotos()).thenReturn(List.of());
    var mockOwnerDao = mock(UserDao.class);
    when(mockBookDao.owner()).thenReturn(mockOwnerDao);
    var mockSwapConditionDao = mock(SwapConditionDao.class);
    when(mockBookDao.swapCondition()).thenReturn(mockSwapConditionDao);
    when(mockSwapConditionDao.swapType()).thenReturn("ByBooks");
    when(mockSwapConditionDao.giveAway()).thenReturn(false);
    when(mockSwapConditionDao.openForOffers()).thenReturn(false);
    when(mockSwapConditionDao.swappableGenres()).thenReturn(List.of());
    when(mockSwapConditionDao.swappableBooks()).thenReturn(List.of());
    when(mockSwapRequestDao.swapType()).thenReturn("ByBooks");
    when(mockSwapRequestDao.swapStatus()).thenReturn("Pending");
    when(mockSwapRequestDao.requestedAt()).thenReturn(Instant.now());
    when(mockSwapRequestDao.updatedAt()).thenReturn(Instant.now());
    when(swapRequestRepository.save(any())).thenReturn(mockSwapRequestDao);
    assertDoesNotThrow(() -> swapService.createSwapRequest(swapRequest));
  }

  @Test
  @DisplayName("Should create swap request with valid offered genre")
  void createSwapRequestWithValidOfferedGenre() {
    var sender = User.builder()
        .id("senderId")
        .build();

    var offeredGenre = Genre.builder()
        .id("offeredGenreId")
        .build();

    var swapCondition = SwapCondition.builder()
        .swappableGenres(List.of(offeredGenre))
        .swappableBooks(List.of())
        .swapType(SwapType.BY_GENRES)
        .build();

    var book = Book.builder()
        .id("bookId")
        .language(Language.ENGLISH)
        .condition(Condition.GOOD)
        .swapCondition(swapCondition)
        .build();

    var swapOffer = SwapOffer.builder()
        .offeredGenre(offeredGenre)
        .build();

    var receiver = User.builder()
        .id("receiverId")
        .books(List.of(book))
        .build();

    var swapRequest = SwapRequest.builder()
        .sender(sender)
        .receiver(receiver)
        .bookToSwapWith(book)
        .swapType(SwapType.BY_GENRES)
        .swapStatus(SwapStatus.PENDING)
        .swapOffer(swapOffer)
        .build();

    when(swapRequestRepository.existsAlready("senderId", "receiverId", "bookId")).thenReturn(false);
    when(userService.getUser("senderId")).thenReturn(sender);
    when(userService.getUser("receiverId")).thenReturn(receiver);
    when(bookService.getBookById("bookId")).thenReturn(book);
    when(genreService.getGenreById("offeredGenreId")).thenReturn(offeredGenre);
    var mockUserDao2 = mock(UserDao.class);
    when(mockUserDao2.id()).thenReturn("senderId");
    var mockReceiverDao2 = mock(UserDao.class);
    when(mockReceiverDao2.id()).thenReturn("receiverId");
    var mockSwapRequestDao2 = mock(SwapRequestDao.class);
    when(mockSwapRequestDao2.sender()).thenReturn(mockUserDao2);
    when(mockSwapRequestDao2.receiver()).thenReturn(mockReceiverDao2);
    var mockBookDao2 = mock(BookDao.class);
    when(mockBookDao2.id()).thenReturn("bookId");
    when(mockSwapRequestDao2.bookToSwapWith()).thenReturn(mockBookDao2);
    var mockSwapOfferDao2 = mock(SwapOfferDao.class);
    var mockGenreDao2 = mock(GenreDao.class);
    when(mockGenreDao2.id()).thenReturn("offeredGenreId");
    when(mockSwapOfferDao2.offeredGenre()).thenReturn(mockGenreDao2);
    when(mockSwapRequestDao2.swapOfferDao()).thenReturn(mockSwapOfferDao2);
    when(mockBookDao2.title()).thenReturn("title");
    when(mockBookDao2.author()).thenReturn("author");
    when(mockBookDao2.description()).thenReturn("desc");
    when(mockBookDao2.language()).thenReturn("English");
    when(mockBookDao2.condition()).thenReturn("New");
    when(mockBookDao2.genres()).thenReturn(java.util.List.of());
    when(mockBookDao2.coverPhotos()).thenReturn(java.util.List.of());
    var mockOwnerDao2 = mock(UserDao.class);
    when(mockBookDao2.owner()).thenReturn(mockOwnerDao2);
    var mockSwapConditionDao2 = mock(SwapConditionDao.class);
    when(mockBookDao2.swapCondition()).thenReturn(mockSwapConditionDao2);
    when(mockSwapConditionDao2.swapType()).thenReturn("ByGenres");
    when(mockSwapConditionDao2.giveAway()).thenReturn(false);
    when(mockSwapConditionDao2.openForOffers()).thenReturn(false);
    when(mockSwapConditionDao2.swappableGenres()).thenReturn(java.util.List.of());
    when(mockSwapConditionDao2.swappableBooks()).thenReturn(java.util.List.of());
    when(mockSwapRequestDao2.swapType()).thenReturn("ByGenres");
    when(mockSwapRequestDao2.swapStatus()).thenReturn("Pending");
    when(mockSwapRequestDao2.requestedAt()).thenReturn(java.time.Instant.now());
    when(mockSwapRequestDao2.updatedAt()).thenReturn(java.time.Instant.now());
    when(swapRequestRepository.save(any())).thenReturn(mockSwapRequestDao2);
    assertDoesNotThrow(() -> swapService.createSwapRequest(swapRequest));
  }

  @Test
  @DisplayName("Should throw SwapRequestNotFoundException when swap request does not exist")
  void updateSwapRequestStatusThrowsWhenSwapRequestNotFound() {
    // Given
    String swapRequestId = "nonexistent123";
    String userId = "user123";
    SwapStatus newStatus = SwapStatus.ACCEPTED;

    when(swapRequestRepository.findById(swapRequestId)).thenReturn(Optional.empty());

    // When & Then
    assertThrows(SwapRequestNotFoundException.class,
        () -> swapService.updateSwapRequestStatus(swapRequestId, newStatus, userId));
    verify(swapRequestRepository).findById(swapRequestId);
  }

  @Test
  @DisplayName("Should throw IllegalSwapRequestException when user is not the receiver")
  void updateSwapRequestStatusThrowsWhenUserIsNotReceiver() {
    // Given
    String swapRequestId = "swap123";
    String userId = "wrongUser123";
    SwapStatus newStatus = SwapStatus.ACCEPTED;

    var mockSwapRequestDao = mock(SwapRequestDao.class);
    var mockSenderDao = mock(UserDao.class);
    var mockReceiverDao = mock(UserDao.class);
    var mockBookDao = mock(BookDao.class);
    var mockSwapOfferDao = mock(SwapOfferDao.class);
    var mockOwnerDao = mock(UserDao.class);
    var mockSwapConditionDao = mock(SwapConditionDao.class);
    var mockSwappableBookDao = mock(SwappableBookDao.class);

    // Mock SwapRequestDao
    when(mockSwapRequestDao.sender()).thenReturn(mockSenderDao);
    when(mockSwapRequestDao.receiver()).thenReturn(mockReceiverDao);
    when(mockSwapRequestDao.bookToSwapWith()).thenReturn(mockBookDao);
    when(mockSwapRequestDao.swapOfferDao()).thenReturn(mockSwapOfferDao);
    when(mockSwapRequestDao.swapType()).thenReturn("ByBooks");
    when(mockSwapRequestDao.swapStatus()).thenReturn("Pending");
    when(mockSwapRequestDao.requestedAt()).thenReturn(Instant.now());
    when(mockSwapRequestDao.updatedAt()).thenReturn(Instant.now());

    // Mock UserDao
    when(mockSenderDao.id()).thenReturn("sender123");
    when(mockReceiverDao.id()).thenReturn("receiver123");

    // Mock BookDao
    when(mockBookDao.id()).thenReturn("bookId");
    when(mockBookDao.title()).thenReturn("Test Book");
    when(mockBookDao.author()).thenReturn("Test Author");
    when(mockBookDao.description()).thenReturn("Test Description");
    when(mockBookDao.language()).thenReturn("English");
    when(mockBookDao.condition()).thenReturn("Good");
    when(mockBookDao.genres()).thenReturn(List.of());
    when(mockBookDao.coverPhotos()).thenReturn(List.of());
    when(mockBookDao.owner()).thenReturn(mockOwnerDao);
    when(mockBookDao.swapCondition()).thenReturn(mockSwapConditionDao);

    // Mock SwapConditionDao
    when(mockSwapConditionDao.swapType()).thenReturn("ByBooks");
    when(mockSwapConditionDao.giveAway()).thenReturn(false);
    when(mockSwapConditionDao.openForOffers()).thenReturn(false);
    when(mockSwapConditionDao.swappableGenres()).thenReturn(List.of());
    when(mockSwapConditionDao.swappableBooks()).thenReturn(List.of());

    // Mock SwapOfferDao
    when(mockSwapOfferDao.offeredBook()).thenReturn(mockSwappableBookDao);
    when(mockSwapOfferDao.offeredGenre()).thenReturn(null);

    when(swapRequestRepository.findById(swapRequestId)).thenReturn(Optional.of(mockSwapRequestDao));

    // When & Then
    assertThrows(IllegalSwapRequestException.class,
        () -> swapService.updateSwapRequestStatus(swapRequestId, newStatus, userId));
    verify(swapRequestRepository).findById(swapRequestId);
  }

  @Test
  @DisplayName("Should throw InvalidStatusTransitionException when trying invalid status transition")
  void updateSwapRequestStatusThrowsWhenInvalidStatusTransition() {
    // Given
    String swapRequestId = "swap123";
    String userId = "receiver123";
    SwapStatus newStatus = SwapStatus.PENDING; // Invalid: can't go from ACCEPTED back to PENDING

    var mockSwapRequestDao = mock(SwapRequestDao.class);
    var mockSenderDao = mock(UserDao.class);
    var mockReceiverDao = mock(UserDao.class);
    var mockBookDao = mock(BookDao.class);
    var mockSwapOfferDao = mock(SwapOfferDao.class);
    var mockOwnerDao = mock(UserDao.class);
    var mockSwapConditionDao = mock(SwapConditionDao.class);
    var mockSwappableBookDao = mock(SwappableBookDao.class);

    // Mock SwapRequestDao
    when(mockSwapRequestDao.sender()).thenReturn(mockSenderDao);
    when(mockSwapRequestDao.receiver()).thenReturn(mockReceiverDao);
    when(mockSwapRequestDao.bookToSwapWith()).thenReturn(mockBookDao);
    when(mockSwapRequestDao.swapOfferDao()).thenReturn(mockSwapOfferDao);
    when(mockSwapRequestDao.swapType()).thenReturn("ByBooks");
    when(mockSwapRequestDao.swapStatus()).thenReturn("Accepted"); // Current status is ACCEPTED
    when(mockSwapRequestDao.requestedAt()).thenReturn(Instant.now());
    when(mockSwapRequestDao.updatedAt()).thenReturn(Instant.now());

    // Mock UserDao
    when(mockSenderDao.id()).thenReturn("sender123");
    when(mockReceiverDao.id()).thenReturn(userId);

    // Mock BookDao
    when(mockBookDao.id()).thenReturn("bookId");
    when(mockBookDao.title()).thenReturn("Test Book");
    when(mockBookDao.author()).thenReturn("Test Author");
    when(mockBookDao.description()).thenReturn("Test Description");
    when(mockBookDao.language()).thenReturn("English");
    when(mockBookDao.condition()).thenReturn("Good");
    when(mockBookDao.genres()).thenReturn(List.of());
    when(mockBookDao.coverPhotos()).thenReturn(List.of());
    when(mockBookDao.owner()).thenReturn(mockOwnerDao);
    when(mockBookDao.swapCondition()).thenReturn(mockSwapConditionDao);

    // Mock SwapConditionDao
    when(mockSwapConditionDao.swapType()).thenReturn("ByBooks");
    when(mockSwapConditionDao.giveAway()).thenReturn(false);
    when(mockSwapConditionDao.openForOffers()).thenReturn(false);
    when(mockSwapConditionDao.swappableGenres()).thenReturn(List.of());
    when(mockSwapConditionDao.swappableBooks()).thenReturn(List.of());

    // Mock SwapOfferDao
    when(mockSwapOfferDao.offeredBook()).thenReturn(mockSwappableBookDao);
    when(mockSwapOfferDao.offeredGenre()).thenReturn(null);

    when(swapRequestRepository.findById(swapRequestId)).thenReturn(Optional.of(mockSwapRequestDao));

    // When & Then
    assertThrows(InvalidStatusTransitionException.class,
        () -> swapService.updateSwapRequestStatus(swapRequestId, newStatus, userId));
    verify(swapRequestRepository).findById(swapRequestId);
  }

  @Test
  @DisplayName("Should successfully update status from PENDING to ACCEPTED")
  void updateSwapRequestStatusSuccessfullyFromPendingToAccepted() {
    // Given
    String swapRequestId = "swap123";
    String userId = "receiver123";
    SwapStatus newStatus = SwapStatus.ACCEPTED;

    var mockSwapRequestDao = mock(SwapRequestDao.class);
    var mockSenderDao = mock(UserDao.class);
    var mockReceiverDao = mock(UserDao.class);
    var mockBookDao = mock(BookDao.class);
    var mockSwapOfferDao = mock(SwapOfferDao.class);
    var mockOwnerDao = mock(UserDao.class);
    var mockSwapConditionDao = mock(SwapConditionDao.class);
    var mockSwappableBookDao = mock(SwappableBookDao.class);

    // Mock SwapRequestDao
    when(mockSwapRequestDao.sender()).thenReturn(mockSenderDao);
    when(mockSwapRequestDao.receiver()).thenReturn(mockReceiverDao);
    when(mockSwapRequestDao.bookToSwapWith()).thenReturn(mockBookDao);
    when(mockSwapRequestDao.swapOfferDao()).thenReturn(mockSwapOfferDao);
    when(mockSwapRequestDao.swapType()).thenReturn("ByBooks");
    when(mockSwapRequestDao.swapStatus()).thenReturn("Pending");
    when(mockSwapRequestDao.requestedAt()).thenReturn(Instant.now());
    when(mockSwapRequestDao.updatedAt()).thenReturn(Instant.now());

    // Mock UserDao
    when(mockSenderDao.id()).thenReturn("sender123");
    when(mockReceiverDao.id()).thenReturn(userId);

    // Mock BookDao
    when(mockBookDao.id()).thenReturn("bookId");
    when(mockBookDao.title()).thenReturn("Test Book");
    when(mockBookDao.author()).thenReturn("Test Author");
    when(mockBookDao.description()).thenReturn("Test Description");
    when(mockBookDao.language()).thenReturn("English");
    when(mockBookDao.condition()).thenReturn("Good");
    when(mockBookDao.genres()).thenReturn(List.of());
    when(mockBookDao.coverPhotos()).thenReturn(List.of());
    when(mockBookDao.owner()).thenReturn(mockOwnerDao);
    when(mockBookDao.swapCondition()).thenReturn(mockSwapConditionDao);

    // Mock SwapConditionDao
    when(mockSwapConditionDao.swapType()).thenReturn("ByBooks");
    when(mockSwapConditionDao.giveAway()).thenReturn(false);
    when(mockSwapConditionDao.openForOffers()).thenReturn(false);
    when(mockSwapConditionDao.swappableGenres()).thenReturn(List.of());
    when(mockSwapConditionDao.swappableBooks()).thenReturn(List.of());

    // Mock SwapOfferDao
    when(mockSwapOfferDao.offeredBook()).thenReturn(mockSwappableBookDao);
    when(mockSwapOfferDao.offeredGenre()).thenReturn(null);

    when(swapRequestRepository.findById(swapRequestId)).thenReturn(Optional.of(mockSwapRequestDao));
    when(swapRequestRepository.save(any(SwapRequestDao.class))).thenReturn(mockSwapRequestDao);

    // When & Then
    assertDoesNotThrow(() -> swapService.updateSwapRequestStatus(swapRequestId, newStatus, userId));
    verify(swapRequestRepository).findById(swapRequestId);
    verify(swapRequestRepository).save(any(SwapRequestDao.class));
  }

  @Test
  @DisplayName("Should successfully update status from PENDING to REJECTED")
  void updateSwapRequestStatusSuccessfullyFromPendingToRejected() {
    // Given
    String swapRequestId = "swap123";
    String userId = "receiver123";
    SwapStatus newStatus = SwapStatus.REJECTED;

    var mockSwapRequestDao = mock(SwapRequestDao.class);
    var mockSenderDao = mock(UserDao.class);
    var mockReceiverDao = mock(UserDao.class);
    var mockBookDao = mock(BookDao.class);
    var mockSwapOfferDao = mock(SwapOfferDao.class);
    var mockOwnerDao = mock(UserDao.class);
    var mockSwapConditionDao = mock(SwapConditionDao.class);
    var mockSwappableBookDao = mock(SwappableBookDao.class);

    // Mock SwapRequestDao
    when(mockSwapRequestDao.sender()).thenReturn(mockSenderDao);
    when(mockSwapRequestDao.receiver()).thenReturn(mockReceiverDao);
    when(mockSwapRequestDao.bookToSwapWith()).thenReturn(mockBookDao);
    when(mockSwapRequestDao.swapOfferDao()).thenReturn(mockSwapOfferDao);
    when(mockSwapRequestDao.swapType()).thenReturn("ByBooks");
    when(mockSwapRequestDao.swapStatus()).thenReturn("Pending");
    when(mockSwapRequestDao.requestedAt()).thenReturn(Instant.now());
    when(mockSwapRequestDao.updatedAt()).thenReturn(Instant.now());

    // Mock UserDao
    when(mockSenderDao.id()).thenReturn("sender123");
    when(mockReceiverDao.id()).thenReturn(userId);

    // Mock BookDao
    when(mockBookDao.id()).thenReturn("bookId");
    when(mockBookDao.title()).thenReturn("Test Book");
    when(mockBookDao.author()).thenReturn("Test Author");
    when(mockBookDao.description()).thenReturn("Test Description");
    when(mockBookDao.language()).thenReturn("English");
    when(mockBookDao.condition()).thenReturn("Good");
    when(mockBookDao.genres()).thenReturn(List.of());
    when(mockBookDao.coverPhotos()).thenReturn(List.of());
    when(mockBookDao.owner()).thenReturn(mockOwnerDao);
    when(mockBookDao.swapCondition()).thenReturn(mockSwapConditionDao);

    // Mock SwapConditionDao
    when(mockSwapConditionDao.swapType()).thenReturn("ByBooks");
    when(mockSwapConditionDao.giveAway()).thenReturn(false);
    when(mockSwapConditionDao.openForOffers()).thenReturn(false);
    when(mockSwapConditionDao.swappableGenres()).thenReturn(List.of());
    when(mockSwapConditionDao.swappableBooks()).thenReturn(List.of());

    // Mock SwapOfferDao
    when(mockSwapOfferDao.offeredBook()).thenReturn(mockSwappableBookDao);
    when(mockSwapOfferDao.offeredGenre()).thenReturn(null);

    when(swapRequestRepository.findById(swapRequestId)).thenReturn(Optional.of(mockSwapRequestDao));
    when(swapRequestRepository.save(any(SwapRequestDao.class))).thenReturn(mockSwapRequestDao);

    // When & Then
    assertDoesNotThrow(() -> swapService.updateSwapRequestStatus(swapRequestId, newStatus, userId));
    verify(swapRequestRepository).findById(swapRequestId);
    verify(swapRequestRepository).save(any(SwapRequestDao.class));
  }

  @Test
  @DisplayName("Should throw IllegalSwapRequestException when sender and receiver are the same")
  void createSwapRequestThrowsWhenSenderAndReceiverAreSame() {
    // Given
    var swapRequest = new SwapRequest();
    var user = new User();
    user.setId("sameUserId");
    var book = new Book();
    book.setId("bookId");

    swapRequest.setSender(user);
    swapRequest.setReceiver(user);
    swapRequest.setBookToSwapWith(book);

    when(swapRequestRepository.existsAlready("sameUserId", "sameUserId", "bookId")).thenReturn(false);

    // When & Then
    assertThrows(IllegalSwapRequestException.class,
        () -> swapService.createSwapRequest(swapRequest));
    verify(swapRequestRepository).existsAlready("sameUserId", "sameUserId", "bookId");
  }

  @Test
  @DisplayName("Should create swap request without swap offer successfully")
  void createSwapRequestWithoutSwapOfferSuccess() {
    // Given
    var swapRequest = new SwapRequest();
    var sender = new User();
    sender.setId("senderId");
    sender.setFirstName("John");
    sender.setLastName("Doe");
    sender.setBooks(List.of());
    var receiver = new User();
    receiver.setId("receiverId");
    var book = new Book();
    book.setId("bookId");
    book.setTitle("Test Book");
    book.setLanguage(Language.ENGLISH);
    book.setCondition(Condition.GOOD);

    swapRequest.setSender(sender);
    swapRequest.setReceiver(receiver);
    swapRequest.setBookToSwapWith(book);
    swapRequest.setSwapType(SwapType.BY_BOOKS);
    swapRequest.setSwapOffer(null); // No swap offer

    receiver.setBooks(List.of(book));
    var swapCondition = new SwapCondition();
    swapCondition.setSwappableBooks(List.of());
    swapCondition.setSwappableGenres(List.of());
    swapCondition.setSwapType(SwapType.BY_BOOKS);
    swapCondition.setGiveAway(false);
    swapCondition.setOpenForOffers(false);
    book.setSwapCondition(swapCondition);

    when(swapRequestRepository.existsAlready("senderId", "receiverId", "bookId")).thenReturn(false);
    when(userService.getUser("senderId")).thenReturn(sender);
    when(userService.getUser("receiverId")).thenReturn(receiver);
    when(bookService.getBookById("bookId")).thenReturn(book);

    var mockSwapRequestDao = createMockSwapRequestDao("senderId", "receiverId", "bookId", "Pending", null);
    when(swapRequestRepository.save(any())).thenReturn(mockSwapRequestDao);

    // When
    SwapRequest result = swapService.createSwapRequest(swapRequest);

    // Then
    assertNotNull(result);
    verify(swapRequestRepository).save(any());
    verify(notificationService).sendNotification(eq("receiverId"), anyString(), anyString());
  }

  @Test
  @DisplayName("Should send notification when creating swap request successfully")
  void createSwapRequestSendsNotificationOnSuccess() {
    // Given
    var swapRequest = new SwapRequest();
    var sender = new User();
    sender.setId("senderId");
    sender.setFirstName("John");
    sender.setLastName("Doe");
    sender.setBooks(List.of());
    var receiver = new User();
    receiver.setId("receiverId");
    var book = new Book();
    book.setId("bookId");
    book.setTitle("Amazing Book");
    book.setLanguage(Language.ENGLISH);
    book.setCondition(Condition.GOOD);

    swapRequest.setSender(sender);
    swapRequest.setReceiver(receiver);
    swapRequest.setBookToSwapWith(book);
    swapRequest.setSwapType(SwapType.BY_BOOKS);

    receiver.setBooks(List.of(book));
    var swapCondition = new SwapCondition();
    swapCondition.setSwappableBooks(List.of());
    swapCondition.setSwappableGenres(List.of());
    swapCondition.setSwapType(SwapType.BY_BOOKS);
    swapCondition.setGiveAway(false);
    swapCondition.setOpenForOffers(false);
    book.setSwapCondition(swapCondition);

    when(swapRequestRepository.existsAlready("senderId", "receiverId", "bookId")).thenReturn(false);
    when(userService.getUser("senderId")).thenReturn(sender);
    when(userService.getUser("receiverId")).thenReturn(receiver);
    when(bookService.getBookById("bookId")).thenReturn(book);

    var mockSwapRequestDao = createMockSwapRequestDao("senderId", "receiverId", "bookId", "Pending", null);
    when(swapRequestRepository.save(any())).thenReturn(mockSwapRequestDao);

    // When
    swapService.createSwapRequest(swapRequest);

    // Then
    verify(notificationService).sendNotification(
        eq("receiverId"),
        eq("New Swap Request"),
        eq("John Doe wants to swap for your book 'Amazing Book'"));
  }

  @Test
  @DisplayName("Should continue creating swap request even when notification fails")
  void createSwapRequestContinuesWhenNotificationFails() {
    // Given
    var swapRequest = new SwapRequest();
    var sender = new User();
    sender.setId("senderId");
    sender.setFirstName("John");
    sender.setLastName("Doe");
    sender.setBooks(List.of());
    var receiver = new User();
    receiver.setId("receiverId");
    var book = new Book();
    book.setId("bookId");
    book.setTitle("Test Book");
    book.setLanguage(Language.ENGLISH);
    book.setCondition(Condition.GOOD);

    swapRequest.setSender(sender);
    swapRequest.setReceiver(receiver);
    swapRequest.setBookToSwapWith(book);
    swapRequest.setSwapType(SwapType.BY_BOOKS);

    receiver.setBooks(List.of(book));
    var swapCondition = new SwapCondition();
    swapCondition.setSwappableBooks(List.of());
    swapCondition.setSwappableGenres(List.of());
    swapCondition.setSwapType(SwapType.BY_BOOKS);
    swapCondition.setGiveAway(false);
    swapCondition.setOpenForOffers(false);
    book.setSwapCondition(swapCondition);

    when(swapRequestRepository.existsAlready("senderId", "receiverId", "bookId")).thenReturn(false);
    when(userService.getUser("senderId")).thenReturn(sender);
    when(userService.getUser("receiverId")).thenReturn(receiver);
    when(bookService.getBookById("bookId")).thenReturn(book);

    var mockSwapRequestDao = createMockSwapRequestDao("senderId", "receiverId", "bookId", "Pending", null);
    when(swapRequestRepository.save(any())).thenReturn(mockSwapRequestDao);

    // Notification service throws exception
    doThrow(new RuntimeException("Notification service unavailable"))
        .when(notificationService).sendNotification(anyString(), anyString(), anyString());

    // When & Then - should not throw exception
    assertDoesNotThrow(() -> swapService.createSwapRequest(swapRequest));
    verify(swapRequestRepository).save(any());
  }

  @Test
  @DisplayName("Should send notification when updating swap request status successfully")
  void updateSwapRequestStatusSendsNotificationOnSuccess() {
    // Given
    String swapRequestId = "swap123";
    String userId = "receiver123";
    SwapStatus newStatus = SwapStatus.ACCEPTED;

    var mockSwapRequestDao = createMockSwapRequestDao("sender123", userId, "bookId", "Pending", null);
    when(mockSwapRequestDao.getBookToSwapWith().getTitle()).thenReturn("Amazing Book");

    when(swapRequestRepository.findById(swapRequestId)).thenReturn(Optional.of(mockSwapRequestDao));
    when(swapRequestRepository.save(any(SwapRequestDao.class))).thenReturn(mockSwapRequestDao);

    // When
    swapService.updateSwapRequestStatus(swapRequestId, newStatus, userId);

    // Then
    verify(notificationService).sendNotification(
        eq("sender123"),
        eq("Swap Request Update"),
        eq("Your swap request for 'Amazing Book' has been accepted"));
  }

  @Test
  @DisplayName("Should continue updating status even when notification fails")
  void updateSwapRequestStatusContinuesWhenNotificationFails() {
    // Given
    String swapRequestId = "swap123";
    String userId = "receiver123";
    SwapStatus newStatus = SwapStatus.REJECTED;

    var mockSwapRequestDao = createMockSwapRequestDao("sender123", userId, "bookId", "Pending", null);

    when(swapRequestRepository.findById(swapRequestId)).thenReturn(Optional.of(mockSwapRequestDao));
    when(swapRequestRepository.save(any(SwapRequestDao.class))).thenReturn(mockSwapRequestDao);

    // Notification service throws exception
    doThrow(new RuntimeException("Notification service unavailable"))
        .when(notificationService).sendNotification(anyString(), anyString(), anyString());

    // When & Then - should not throw exception
    assertDoesNotThrow(() -> swapService.updateSwapRequestStatus(swapRequestId, newStatus, userId));
    verify(swapRequestRepository).save(any(SwapRequestDao.class));
  }

  @Test
  @DisplayName("Should validate that only PENDING status can transition to ACCEPTED")
  void updateSwapRequestStatusValidatesTransitionFromPendingToAccepted() {
    // Given
    String swapRequestId = "swap123";
    String userId = "receiver123";
    SwapStatus newStatus = SwapStatus.ACCEPTED;

    var mockSwapRequestDao = createMockSwapRequestDao("sender123", userId, "bookId", "Pending", null);

    when(swapRequestRepository.findById(swapRequestId)).thenReturn(Optional.of(mockSwapRequestDao));
    when(swapRequestRepository.save(any(SwapRequestDao.class))).thenReturn(mockSwapRequestDao);

    // When & Then
    assertDoesNotThrow(() -> swapService.updateSwapRequestStatus(swapRequestId, newStatus, userId));
    verify(swapRequestRepository).save(any(SwapRequestDao.class));
  }

  @Test
  @DisplayName("Should validate that only PENDING status can transition to REJECTED")
  void updateSwapRequestStatusValidatesTransitionFromPendingToRejected() {
    // Given
    String swapRequestId = "swap123";
    String userId = "receiver123";
    SwapStatus newStatus = SwapStatus.REJECTED;

    var mockSwapRequestDao = createMockSwapRequestDao("sender123", userId, "bookId", "Pending", null);

    when(swapRequestRepository.findById(swapRequestId)).thenReturn(Optional.of(mockSwapRequestDao));
    when(swapRequestRepository.save(any(SwapRequestDao.class))).thenReturn(mockSwapRequestDao);

    // When & Then
    assertDoesNotThrow(() -> swapService.updateSwapRequestStatus(swapRequestId, newStatus, userId));
    verify(swapRequestRepository).save(any(SwapRequestDao.class));
  }

  @Test
  @DisplayName("Should throw InvalidStatusTransitionException when transitioning from REJECTED")
  void updateSwapRequestStatusThrowsWhenTransitioningFromRejected() {
    // Given
    String swapRequestId = "swap123";
    String userId = "receiver123";
    SwapStatus newStatus = SwapStatus.ACCEPTED;

    var mockSwapRequestDao = createMockSwapRequestDao("sender123", userId, "bookId", "Rejected", null);

    when(swapRequestRepository.findById(swapRequestId)).thenReturn(Optional.of(mockSwapRequestDao));

    // When & Then
    assertThrows(InvalidStatusTransitionException.class,
        () -> swapService.updateSwapRequestStatus(swapRequestId, newStatus, userId));
  }

  // Helper method to create mock SwapRequestDao
  private SwapRequestDao createMockSwapRequestDao(String senderId, String receiverId, String bookId,
      String status, SwapOfferDao swapOfferDao) {
    var mockSwapRequestDao = mock(SwapRequestDao.class);
    var mockSenderDao = mock(UserDao.class);
    var mockReceiverDao = mock(UserDao.class);
    var mockBookDao = mock(BookDao.class);
    var mockOwnerDao = mock(UserDao.class);
    var mockSwapConditionDao = mock(SwapConditionDao.class);

    when(mockSwapRequestDao.getSender()).thenReturn(mockSenderDao);
    when(mockSwapRequestDao.getReceiver()).thenReturn(mockReceiverDao);
    when(mockSwapRequestDao.getBookToSwapWith()).thenReturn(mockBookDao);
    when(mockSwapRequestDao.getSwapOfferDao()).thenReturn(swapOfferDao);
    when(mockSwapRequestDao.getSwapType()).thenReturn("ByBooks");
    when(mockSwapRequestDao.getSwapStatus()).thenReturn(status);
    when(mockSwapRequestDao.getRequestedAt()).thenReturn(Instant.now());
    when(mockSwapRequestDao.getUpdatedAt()).thenReturn(Instant.now());

    when(mockSenderDao.getId()).thenReturn(senderId);
    when(mockReceiverDao.getId()).thenReturn(receiverId);

    when(mockBookDao.getId()).thenReturn(bookId);
    when(mockBookDao.getTitle()).thenReturn("Test Book");
    when(mockBookDao.getAuthor()).thenReturn("Test Author");
    when(mockBookDao.getDescription()).thenReturn("Test Description");
    when(mockBookDao.getLanguage()).thenReturn("English");
    when(mockBookDao.getCondition()).thenReturn("Good");
    when(mockBookDao.getGenres()).thenReturn(List.of());
    when(mockBookDao.getCoverPhotos()).thenReturn(List.of());
    when(mockBookDao.getOwner()).thenReturn(mockOwnerDao);
    when(mockBookDao.getSwapCondition()).thenReturn(mockSwapConditionDao);

    when(mockSwapConditionDao.getSwapType()).thenReturn("ByBooks");
    when(mockSwapConditionDao.isGiveAway()).thenReturn(false);
    when(mockSwapConditionDao.isOpenForOffers()).thenReturn(false);
    when(mockSwapConditionDao.getSwappableGenres()).thenReturn(List.of());
    when(mockSwapConditionDao.getSwappableBooks()).thenReturn(List.of());

    return mockSwapRequestDao;
  }
}
