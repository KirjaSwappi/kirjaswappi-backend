/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.kirjaswappi.backend.jpa.daos.BookDao;
import com.kirjaswappi.backend.jpa.daos.GenreDao;
import com.kirjaswappi.backend.jpa.daos.UserDao;
import com.kirjaswappi.backend.jpa.repositories.BookRepository;
import com.kirjaswappi.backend.jpa.repositories.GenreRepository;
import com.kirjaswappi.backend.jpa.repositories.UserRepository;
import com.kirjaswappi.backend.service.entities.Book;
import com.kirjaswappi.backend.service.entities.Genre;
import com.kirjaswappi.backend.service.entities.User;
import com.kirjaswappi.backend.service.enums.Condition;
import com.kirjaswappi.backend.service.enums.Language;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;
import com.kirjaswappi.backend.service.exceptions.BookNotFoundException;
import com.kirjaswappi.backend.service.exceptions.UserAlreadyExistsException;
import com.kirjaswappi.backend.service.exceptions.UserNotFoundException;

class UserServiceTest {
  @Mock
  private UserRepository userRepository;
  @Mock
  private BookRepository bookRepository;
  @Mock
  private GenreRepository genreRepository;
  @InjectMocks
  private UserService userService;

  @BeforeEach
  @DisplayName("Setup mocks for UserServiceTest")
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  @DisplayName("Should throw UserNotFoundException when user not found by id")
  void getUserThrowsWhenNotFound() {
    when(userRepository.findByIdAndIsEmailVerifiedTrue("id")).thenReturn(Optional.empty());
    assertThrows(UserNotFoundException.class, () -> userService.getUser("id"));
  }

  @Test
  @DisplayName("Should return user when found by id")
  void getUserReturnsUserWhenFound() {
    UserDao userDao = new UserDao();
    userDao.setId("id");
    userDao.setEmailVerified(true);
    when(userRepository.findByIdAndIsEmailVerifiedTrue("id")).thenReturn(Optional.of(userDao));
    User result = userService.getUser("id");
    assertEquals("id", result.getId());
  }

  @Test
  @DisplayName("Should throw UserAlreadyExistsException if user already exists and is verified")
  void addUserThrowsIfAlreadyExists() {
    User user = new User();
    user.setEmail("test@example.com");
    when(userRepository.findByEmailAndIsEmailVerified("test@example.com", true)).thenReturn(Optional.of(new UserDao()));
    assertThrows(UserAlreadyExistsException.class, () -> userService.addUser(user));
  }

  @Test
  @DisplayName("Should throw BadRequestException if user exists but not verified")
  void addUserThrowsIfExistsButNotVerified() {
    User user = new User();
    user.setEmail("test@example.com");
    when(userRepository.findByEmailAndIsEmailVerified("test@example.com", false))
        .thenReturn(Optional.of(new UserDao()));
    assertThrows(BadRequestException.class, () -> userService.addUser(user));
  }

  @Test
  @DisplayName("Should save user when adding a new user with all required fields")
  void addUserSavesUser() {
    User user = new User();
    user.setEmail("test@example.com");
    user.setPassword("password");
    user.setFirstName("Test");
    user.setLastName("User");
    user.setFavGenres(List.of());
    when(userRepository.findByEmailAndIsEmailVerified("test@example.com", false)).thenReturn(Optional.empty());
    when(userRepository.findByEmailAndIsEmailVerified("test@example.com", true)).thenReturn(Optional.empty());
    when(userRepository.save(any())).thenReturn(new UserDao());
    assertNotNull(userService.addUser(user));
  }

  @Test
  @DisplayName("Should throw UserNotFoundException when updating user that does not exist")
  void updateUserThrowsWhenNotFound() {
    User user = new User();
    user.setId("id");
    when(userRepository.findByIdAndIsEmailVerifiedTrue("id")).thenReturn(Optional.empty());
    assertThrows(UserNotFoundException.class, () -> userService.updateUser(user));
  }

  @Test
  @DisplayName("Should throw BadRequestException when updating user that is not verified")
  void updateUserThrowsIfNotVerified() {
    User user = new User();
    user.setId("id");
    UserDao dao = new UserDao();
    dao.setId("id");
    dao.setEmailVerified(false);
    when(userRepository.findByIdAndIsEmailVerifiedTrue("id")).thenReturn(Optional.of(dao));
    assertThrows(BadRequestException.class, () -> userService.updateUser(user));
  }

  @Test
  @DisplayName("Should update user when all required fields are present")
  void updateUserUpdatesUser() {
    User user = new User();
    user.setId("id");
    user.setFirstName("Test");
    user.setLastName("User");
    user.setFavGenres(List.of(new Genre("genre")));
    UserDao dao = new UserDao();
    dao.setId("id");
    dao.setEmailVerified(true);
    when(userRepository.findByIdAndIsEmailVerifiedTrue("id")).thenReturn(Optional.of(dao));
    when(genreRepository.findByName(any())).thenReturn(Optional.of(new GenreDao()));
    when(userRepository.save(any())).thenReturn(dao);
    assertNotNull(userService.updateUser(user));
  }

  @Test
  @DisplayName("Should throw UserNotFoundException when deleting user that does not exist")
  void deleteUserThrowsWhenNotFound() {
    when(userRepository.findById("id")).thenReturn(Optional.empty());
    assertThrows(UserNotFoundException.class, () -> userService.deleteUser("id"));
  }

  @Test
  @DisplayName("Should delete user when found by id")
  void deleteUserDeletesUser() {
    UserDao dao = new UserDao();
    dao.setId("id");
    when(userRepository.findById("id")).thenReturn(Optional.of(dao));
    doNothing().when(userRepository).delete(dao);
    userService.deleteUser("id");
    verify(userRepository, times(1)).delete(dao);
  }

  @Test
  @DisplayName("Should return list of users when users exist")
  void getUsersReturnsList() {
    UserDao dao1 = new UserDao();
    dao1.setId("id1");
    dao1.setEmailVerified(true);
    UserDao dao2 = new UserDao();
    dao2.setId("id2");
    dao2.setEmailVerified(true);
    when(userRepository.findAllByIsEmailVerifiedTrue()).thenReturn(List.of(dao1, dao2));
    List<User> users = userService.getUsers();
    assertEquals(2, users.size());
  }

  @Test
  @DisplayName("Should throw UserNotFoundException when verifying email for non-existent user")
  void verifyEmailThrowsWhenNotFound() {
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
    assertThrows(UserNotFoundException.class, () -> userService.verifyEmail("test@example.com"));
  }

  @Test
  @DisplayName("Should set email as verified when verifying email for existing user")
  void verifyEmailSetsVerified() {
    UserDao dao = new UserDao();
    dao.setEmail("test@example.com");
    dao.setEmailVerified(false);
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(dao));
    when(userRepository.save(dao)).thenReturn(dao);
    String email = userService.verifyEmail("test@example.com");
    assertEquals("test@example.com", email);
    assertTrue(dao.isEmailVerified());
  }

  @Test
  @DisplayName("Should throw UserNotFoundException when adding favourite book for non-existent user")
  void addFavouriteBookThrowsIfUserNotFound() {
    User user = new User();
    user.setId("id");
    when(userRepository.findByIdAndIsEmailVerifiedTrue("id")).thenReturn(Optional.empty());
    assertThrows(UserNotFoundException.class, () -> userService.addFavouriteBook(user));
  }

  @Test
  @DisplayName("Should throw BookNotFoundException when adding favourite book that does not exist")
  void addFavouriteBookThrowsIfBookNotFound() {
    User user = new User();
    user.setId("id");
    BookDao bookDao = new BookDao();
    bookDao.setId("bookId");
    user.setFavBooks(List.of(new Book()));
    UserDao userDao = new UserDao();
    userDao.setId("id");
    when(userRepository.findByIdAndIsEmailVerifiedTrue("id")).thenReturn(Optional.of(userDao));
    when(bookRepository.findByIdAndIsDeletedFalse(any())).thenReturn(Optional.empty());
    assertThrows(BookNotFoundException.class, () -> userService.addFavouriteBook(user));
  }

  @Test
  @DisplayName("Should throw BadRequestException when adding own book as favourite")
  void addFavouriteBookThrowsIfOwnBook() {
    User user = new User();
    user.setId("id");
    Book book = new Book();
    book.setId("bookId");
    user.setFavBooks(List.of(book));
    UserDao userDao = new UserDao();
    userDao.setId("id");
    BookDao bookDao = new BookDao();
    bookDao.setId("bookId");
    UserDao ownerDao = new UserDao();
    ownerDao.setId("id");
    bookDao.setOwner(ownerDao);
    when(userRepository.findByIdAndIsEmailVerifiedTrue("id")).thenReturn(Optional.of(userDao));
    when(bookRepository.findByIdAndIsDeletedFalse("bookId")).thenReturn(Optional.of(bookDao));
    assertThrows(BadRequestException.class, () -> userService.addFavouriteBook(user));
  }

  @Test
  @DisplayName("Should throw BadRequestException when adding already favourite book")
  void addFavouriteBookThrowsIfAlreadyFav() {
    User user = new User();
    user.setId("id");
    Book book = new Book();
    book.setId("bookId");
    book.setLanguage(Language.ENGLISH);
    book.setCondition(Condition.NEW);
    user.setFavBooks(List.of(book));
    UserDao userDao = new UserDao();
    userDao.setId("id");
    BookDao bookDao = new BookDao();
    bookDao.setId("bookId");
    bookDao.setLanguage("English");
    bookDao.setCondition("New");
    UserDao ownerDao = new UserDao();
    ownerDao.setId("other");
    bookDao.setOwner(ownerDao);
    userDao.setFavBooks(List.of(bookDao));
    when(userRepository.findByIdAndIsEmailVerifiedTrue("id")).thenReturn(Optional.of(userDao));
    when(bookRepository.findByIdAndIsDeletedFalse("bookId")).thenReturn(Optional.of(bookDao));
    assertThrows(BadRequestException.class, () -> userService.addFavouriteBook(user));
  }

  @Test
  @DisplayName("Should add favourite book successfully when all conditions are met")
  void addFavouriteBookSuccess() {
    User user = new User();
    user.setId("id");
    Book favBook = new Book();
    favBook.setId("bookId");
    favBook.setLanguage(Language.ENGLISH);
    favBook.setCondition(Condition.NEW);
    favBook.setGenres(
        List.of(new Genre("genreId", "Genre Name", null)));
    user.setFavBooks(List.of(favBook));
    UserDao userDao = new UserDao();
    userDao.setId("id");
    BookDao bookDao = new BookDao();
    bookDao.setId("bookId");
    bookDao.setLanguage("English");
    bookDao.setCondition("New");
    bookDao.setGenres(List.of(new GenreDao("genreId", "Genre Name", null)));
    UserDao ownerDao = new UserDao();
    ownerDao.setId("other");
    bookDao.setOwner(ownerDao);
    userDao.setFavBooks(null);
    when(userRepository.findByIdAndIsEmailVerifiedTrue("id")).thenReturn(Optional.of(userDao));
    when(bookRepository.findByIdAndIsDeletedFalse("bookId")).thenReturn(Optional.of(bookDao));
    when(userRepository.save(userDao)).thenReturn(userDao);
    when(userRepository.findByIdAndIsEmailVerifiedTrue("id")).thenReturn(Optional.of(userDao));
    assertNotNull(userService.addFavouriteBook(user));
  }

  @Test
  @DisplayName("Should return existing user for findOrCreateGoogleUser if user exists")
  void findOrCreateGoogleUser_returnsExistingUser() {
    String email = "test@example.com";
    String firstName = "Test";
    String lastName = "User";
    String googleSub = "google-sub-123";
    UserDao existingDao = new UserDao();
    existingDao.setEmail(email);
    existingDao.setFirstName(firstName);
    existingDao.setLastName(lastName);
    existingDao.setSalt(googleSub);
    existingDao.setEmailVerified(true);
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingDao));

    User result = userService.findOrCreateGoogleUser(email, firstName, lastName, googleSub);
    assertEquals(email, result.getEmail());
    assertEquals(firstName, result.getFirstName());
    assertEquals(lastName, result.getLastName());
  }

  @Test
  @DisplayName("Should create and return new user for findOrCreateGoogleUser if user does not exist")
  void findOrCreateGoogleUser_createsNewUser() {
    String email = "new@example.com";
    String firstName = "New";
    String lastName = "User";
    String googleSub = "google-sub-456";
    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
    UserDao savedDao = new UserDao();
    savedDao.setEmail(email);
    savedDao.setFirstName(firstName);
    savedDao.setLastName(lastName);
    savedDao.setSalt(googleSub);
    savedDao.setEmailVerified(true);
    when(userRepository.save(any(UserDao.class))).thenReturn(savedDao);

    User result = userService.findOrCreateGoogleUser(email, firstName, lastName, googleSub);
    assertEquals(email, result.getEmail());
    assertEquals(firstName, result.getFirstName());
    assertEquals(lastName, result.getLastName());
  }
}
