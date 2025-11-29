/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import com.kirjaswappi.backend.jpa.daos.BookDao;
import com.kirjaswappi.backend.jpa.daos.SwapConditionDao;
import com.kirjaswappi.backend.jpa.daos.UserDao;
import com.kirjaswappi.backend.jpa.repositories.BookRepository;
import com.kirjaswappi.backend.jpa.repositories.UserRepository;
import com.kirjaswappi.backend.mapper.BookMapper;
import com.kirjaswappi.backend.service.entities.Book;
import com.kirjaswappi.backend.service.entities.SwapCondition;
import com.kirjaswappi.backend.service.entities.User;
import com.kirjaswappi.backend.service.enums.Condition;
import com.kirjaswappi.backend.service.enums.Language;
import com.kirjaswappi.backend.service.enums.SwapType;
import com.kirjaswappi.backend.service.exceptions.BookNotFoundException;
import com.kirjaswappi.backend.service.filters.FindAllBooksFilter;

class BookServiceTest {
  @Mock
  private BookRepository bookRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private PhotoService photoService;
  @InjectMocks
  private BookService bookService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  @DisplayName("Throws when book is not found by ID")
  void getBookByIdThrowsWhenNotFound() {
    when(bookRepository.findByIdAndIsDeletedFalse("id")).thenReturn(Optional.empty());
    assertThrows(BookNotFoundException.class, () -> bookService.getBookById("id"));
  }

  @Test
  @DisplayName("Returns book when found by ID")
  void getBookByIdReturnsBookWhenFound() {
    var dao = BookDao.builder()
        .id("id")
        .swapCondition(new SwapConditionDao("ByBooks", false, false, List.of(), List.of()))
        .owner(new UserDao().id("owner-id"))
        .language("English")
        .condition("New")
        .genres(List.of())
        .coverPhotos(List.of())
        .bookAddedAt(Instant.now().minusSeconds(3600))
        .bookUpdatedAt(Instant.now().minusSeconds(1800))
        .isDeleted(false)
        .build();

    when(bookRepository.findByIdAndIsDeletedFalse("id")).thenReturn(Optional.of(dao));

    Book book = bookService.getBookById("id");

    assertEquals("id", book.id());
    assertNotNull(book.bookAddedAt());
    assertNotNull(book.bookUpdatedAt());
    assertNotNull(book.getOfferedAgo());
  }

  @Test
  @DisplayName("Returns page of books by filter")
  void getAllBooksByFilterReturnsPage() {
    FindAllBooksFilter filter = mock(FindAllBooksFilter.class);
    Pageable pageable = PageRequest.of(0, 10);
    when(filter.buildSearchAndFilterCriteria()).thenReturn(null);
    when(bookRepository.findAllBooksByFilter(any(), any())).thenReturn(new PageImpl<>(List.of()));
    Page<Book> result = bookService.getAllBooksByFilter(filter, pageable);
    assertNotNull(result);
    assertEquals(0, result.getTotalElements());
  }

  @Test
  @DisplayName("Saves a new book")
  void createBookSavesBook() {

    Book book = Book.builder()
        .swapCondition(new SwapCondition(
            SwapType.OPEN_FOR_OFFERS, false, true, null, null))
        .language(Language.ENGLISH)
        .condition(Condition.NEW)
        .genres(List.of())
        .coverPhotos(List.of())
        .coverPhotoFiles(List.of())
        .owner(new User())
        .bookAddedAt(Instant.now().minusSeconds(7200))
        .bookUpdatedAt(Instant.now().minusSeconds(3600))
        .build();

    UserDao userDao = UserDao.builder()
        .id("owner-id")
        .firstName("Test")
        .lastName("User")
        .email("test@example.com")
        .password("password")
        .salt("salt")
        .isEmailVerified(true)
        .build();

    when(userRepository.findByIdAndIsEmailVerifiedTrue(any())).thenReturn(Optional.of(userDao));

    var bookDao = BookMapper.toDao(book)
        .isDeleted(false);

    when(bookRepository.save(any())).thenReturn(bookDao);
    when(bookRepository.findByIdAndIsDeletedFalse(any())).thenReturn(Optional.of(bookDao));

    Book savedBook = bookService.createBook(book);

    assertNotNull(savedBook);
    assertNotNull(savedBook.bookAddedAt());
    assertNotNull(savedBook.bookUpdatedAt());
    assertNotNull(savedBook.getOfferedAgo());
  }

  @Test
  @DisplayName("Updates an existing book")
  void updateBookUpdatesBook() {
    Book book = Book.builder()
        .id("id")
        .swapCondition(new SwapCondition(
            SwapType.OPEN_FOR_OFFERS, false, true, null, null))
        .language(Language.ENGLISH)
        .condition(Condition.NEW)
        .genres(List.of())
        .coverPhotos(List.of())
        .coverPhotoFiles(List.of())
        .owner(new User())
        .bookAddedAt(java.time.Instant.now().minusSeconds(7200))
        .bookUpdatedAt(java.time.Instant.now().minusSeconds(3600))
        .build();

    var dao = BookDao.builder()
        .id("id")
        .swapCondition(
            new SwapConditionDao("OpenForOffers", false, true, null, null))
        .owner(new UserDao())
        .language("English")
        .condition("New")
        .genres(List.of())
        .coverPhotos(List.of())
        .bookAddedAt(Instant.now().minusSeconds(7200))
        .bookUpdatedAt(Instant.now().minusSeconds(3600))
        .isDeleted(false)
        .build();

    when(bookRepository.findByIdAndIsDeletedFalse("id")).thenReturn(Optional.of(dao));
    when(bookRepository.save(any())).thenReturn(dao);

    Book updatedBook = bookService.updateBook(book);

    assertNotNull(updatedBook);
    assertNotNull(updatedBook.bookAddedAt());
    assertNotNull(updatedBook.bookUpdatedAt());
    assertNotNull(updatedBook.getOfferedAgo());
  }

  @Test
  @DisplayName("Throws when updating a non-existent book")
  void updateBookThrowsWhenNotFound() {
    Book book = Book.builder().id("id").build();
    when(bookRepository.findByIdAndIsDeletedFalse("id")).thenReturn(Optional.empty());
    assertThrows(BookNotFoundException.class, () -> bookService.updateBook(book));
  }

  @Test
  @DisplayName("Deletes a book by ID")
  void deleteBookDeletesBook() {
    var dao = BookDao.builder()
        .id("id")
        .owner(new UserDao())
        .swapCondition(
            new SwapConditionDao("ByBooks", false, false, List.of(), List.of()))
        .language("English")
        .condition("New")
        .genres(List.of())
        .coverPhotos(List.of())
        .build();

    when(bookRepository.findByIdAndIsDeletedFalse("id")).thenReturn(Optional.of(dao));

    UserDao userDao = UserDao.builder()
        .id("owner-id")
        .firstName("Test")
        .lastName("User")
        .email("test@example.com")
        .password("password")
        .salt("salt")
        .isEmailVerified(true)
        .build();

    when(userRepository.findByIdAndIsEmailVerifiedTrue(any())).thenReturn(Optional.of(userDao));
    doNothing().when(bookRepository).deleteLogically("id");

    bookService.deleteBook("id");

    verify(bookRepository, times(1)).deleteLogically("id");
  }

  @Test
  @DisplayName("Throws when deleting a non-existent book")
  void deleteBookThrowsWhenNotFound() {
    when(bookRepository.findByIdAndIsDeletedFalse("id")).thenReturn(Optional.empty());
    assertThrows(BookNotFoundException.class, () -> bookService.deleteBook("id"));
  }

  @Test
  @DisplayName("Returns page of books by user ID and filter")
  void getUserBooksByFilterReturnsPage() {
    String userId = "user-123";
    FindAllBooksFilter filter = mock(FindAllBooksFilter.class);
    Pageable pageable = PageRequest.of(0, 10);
    Criteria mockCriteria = mock(Criteria.class);

    when(filter.buildSearchAndFilterCriteria()).thenReturn(mockCriteria);
    when(bookRepository.findAllBooksByFilter(eq(mockCriteria), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(), pageable, 0));

    Page<Book> result = bookService.getUserBooksByFilter(userId, filter, pageable);

    assertNotNull(result);
    assertEquals(0, result.getTotalElements());
    verify(filter).buildSearchAndFilterCriteria();
    verify(bookRepository).findAllBooksByFilter(eq(mockCriteria), any(Pageable.class));
  }

  @Test
  @DisplayName("getAllBooksByFilter returns books with owner info populated")
  void getAllBooksByFilterReturnsBooksWithOwnerInfo() {
    UserDao ownerDao = UserDao.builder()
        .id("owner-123")
        .firstName("Alice")
        .lastName("Smith")
        .build();

    SwapConditionDao swapConditionDao = SwapConditionDao.builder()
        .swapType("OpenForOffers")
        .build();

    BookDao bookDao = BookDao.builder()
        .id("book-1")
        .title("Book Title")
        .author("Author")
        .genres(List.of())
        .language("English")
        .condition("New")
        .coverPhotos(List.of("cover-url"))
        .swapCondition(swapConditionDao)
        .owner(ownerDao)
        .build();

    FindAllBooksFilter filter = mock(FindAllBooksFilter.class);
    Pageable pageable = PageRequest.of(0, 10);
    when(filter.buildSearchAndFilterCriteria()).thenReturn(null);
    when(bookRepository.findAllBooksByFilter(any(), any())).thenReturn(new PageImpl<>(List.of(bookDao), pageable, 1));
    when(photoService.getBookCoverPhoto(any())).thenReturn("dummy-url");
    Page<Book> result = bookService.getAllBooksByFilter(filter, pageable);
    assertEquals(1, result.getTotalElements());
    Book book = result.getContent().getFirst();
    assertNotNull(book.owner());
    assertEquals("owner-123", book.owner().id());
    assertEquals("Alice", book.owner().firstName());
    assertEquals("Smith", book.owner().lastName());
  }
}
