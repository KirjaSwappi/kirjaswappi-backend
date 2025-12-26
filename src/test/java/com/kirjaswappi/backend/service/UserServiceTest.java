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

import com.kirjaswappi.backend.common.service.EmailService;
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
  @Mock
  private EmailService emailService;
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
    UserDao userDao = UserDao.builder().id("id").isEmailVerified(true).build();

    when(userRepository.findByIdAndIsEmailVerifiedTrue("id")).thenReturn(Optional.of(userDao));

    User result = userService.getUser("id");
    assertEquals("id", result.id());
  }

  @Test
  @DisplayName("Should throw UserAlreadyExistsException if user already exists and is verified")
  void addUserThrowsIfAlreadyExists() {
    User user = new User().email("test@example.com");
    when(userRepository.findByEmailAndIsEmailVerified("test@example.com", true))
        .thenReturn(Optional.of(UserDao.builder().build()));
    assertThrows(UserAlreadyExistsException.class, () -> userService.addUser(user));
  }

  @Test
  @DisplayName("Should throw BadRequestException if user exists but not verified")
  void addUserThrowsIfExistsButNotVerified() {
    User user = new User().email("test@example.com");
    when(userRepository.findByEmailAndIsEmailVerified("test@example.com", false))
        .thenReturn(Optional.of(UserDao.builder().build()));
    assertThrows(BadRequestException.class, () -> userService.addUser(user));
  }

  @Test
  @DisplayName("Should save user when adding a new user with all required fields")
  void addUserSavesUser() {
    User user = new User()
        .email("test@example.com")
        .password("password")
        .firstName("Test")
        .lastName("User")
        .favGenres(List.of());
    when(userRepository.findByEmailAndIsEmailVerified("test@example.com", false)).thenReturn(Optional.empty());
    when(userRepository.findByEmailAndIsEmailVerified("test@example.com", true)).thenReturn(Optional.empty());
    when(userRepository.save(any())).thenReturn(UserDao.builder().build());
    assertNotNull(userService.addUser(user));
  }

  @Test
  @DisplayName("Should throw UserNotFoundException when updating user that does not exist")
  void updateUserThrowsWhenNotFound() {
    User user = new User().id("id");
    when(userRepository.findByIdAndIsEmailVerifiedTrue("id")).thenReturn(Optional.empty());
    assertThrows(UserNotFoundException.class, () -> userService.updateUser(user));
  }

  @Test
  @DisplayName("Should throw BadRequestException when updating user that is not verified")
  void updateUserThrowsIfNotVerified() {
    User user = new User().id("id");
    UserDao dao = UserDao.builder().id("id").isEmailVerified(false).build();
    when(userRepository.findByIdAndIsEmailVerifiedTrue("id")).thenReturn(Optional.of(dao));
    assertThrows(BadRequestException.class, () -> userService.updateUser(user));
  }

  @Test
  @DisplayName("Should update user when all required fields are present")
  void updateUserUpdatesUser() {
    User user = new User()
        .id("id")
        .firstName("Test")
        .lastName("User")
        .favGenres(List.of(new Genre("genre")));

    UserDao dao = UserDao.builder().id("id").isEmailVerified(true).build();

    when(userRepository.findByIdAndIsEmailVerifiedTrue("id")).thenReturn(Optional.of(dao));
    when(genreRepository.findByName(any())).thenReturn(Optional.of(GenreDao.builder().build()));
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
    UserDao dao = UserDao.builder().id("id").build();
    when(userRepository.findById("id")).thenReturn(Optional.of(dao));
    doNothing().when(userRepository).delete(dao);
    userService.deleteUser("id");
    verify(userRepository, times(1)).delete(dao);
  }

  @Test
  @DisplayName("Should return list of users when users exist")
  void getUsersReturnsList() {
    UserDao dao1 = UserDao.builder().id("id1").isEmailVerified(true).build();
    UserDao dao2 = UserDao.builder().id("id2").isEmailVerified(true).build();

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
    UserDao dao = UserDao.builder().email("test@example.com").isEmailVerified(false).build();

    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(dao));
    when(userRepository.save(dao)).thenReturn(dao);
    String email = userService.verifyEmail("test@example.com");
    assertEquals("test@example.com", email);
    assertTrue(dao.isEmailVerified());
  }

  @Test
  @DisplayName("Should throw UserNotFoundException when adding favourite book for non-existent user")
  void addFavouriteBookThrowsIfUserNotFound() {
    User user = new User().id("id");
    when(userRepository.findByIdAndIsEmailVerifiedTrue("id")).thenReturn(Optional.empty());
    assertThrows(UserNotFoundException.class, () -> userService.addFavouriteBook(user));
  }

  @Test
  @DisplayName("Should throw BookNotFoundException when adding favourite book that does not exist")
  void addFavouriteBookThrowsIfBookNotFound() {

    User user = new User().id("id").favBooks(List.of(Book.builder().build()));

    UserDao userDao = UserDao.builder().id("id").build();

    when(userRepository.findByIdAndIsEmailVerifiedTrue("id")).thenReturn(Optional.of(userDao));
    when(bookRepository.findByIdAndIsDeletedFalse(any())).thenReturn(Optional.empty());

    assertThrows(BookNotFoundException.class, () -> userService.addFavouriteBook(user));
  }

  @Test
  @DisplayName("Should throw BadRequestException when adding own book as favourite")
  void addFavouriteBookThrowsIfOwnBook() {

    Book book = Book.builder().id("bookId").build();

    User user = new User().id("id").favBooks(List.of(book));

    UserDao userDao = UserDao.builder().id("id").build();

    UserDao ownerDao = UserDao.builder().id("id").build();
    BookDao bookDao = BookDao.builder().id("bookId").owner(ownerDao).build();

    when(userRepository.findByIdAndIsEmailVerifiedTrue("id")).thenReturn(Optional.of(userDao));
    when(bookRepository.findByIdAndIsDeletedFalse("bookId")).thenReturn(Optional.of(bookDao));
    assertThrows(BadRequestException.class, () -> userService.addFavouriteBook(user));
  }

  @Test
  @DisplayName("Should throw BadRequestException when adding already favourite book")
  void addFavouriteBookThrowsIfAlreadyFav() {

    Book book = Book.builder()
        .id("bookId")
        .language(Language.ENGLISH)
        .condition(Condition.NEW)
        .build();

    User user = new User().id("id").favBooks(List.of(book));

    UserDao ownerDao = UserDao.builder().id("other").build();

    BookDao bookDao = BookDao.builder()
        .id("bookId")
        .language("English")
        .owner(ownerDao)
        .condition("New")
        .build();

    UserDao userDao = UserDao.builder()
        .id("id")
        .favBooks(List.of(bookDao))
        .build();

    when(userRepository.findByIdAndIsEmailVerifiedTrue("id")).thenReturn(Optional.of(userDao));
    when(bookRepository.findByIdAndIsDeletedFalse("bookId")).thenReturn(Optional.of(bookDao));
    assertThrows(BadRequestException.class, () -> userService.addFavouriteBook(user));
  }

  @Test
  @DisplayName("Should add favourite book successfully when all conditions are met")
  void addFavouriteBookSuccess() {

    Book favBook = Book.builder()
        .id("bookId")
        .language(Language.ENGLISH)
        .condition(Condition.NEW)
        .genres(List.of(new Genre("genreId", "Genre Name", null)))
        .build();

    User user = new User()
        .id("id")
        .favBooks(List.of(favBook));

    UserDao ownerDao = UserDao.builder().id("other").build();

    BookDao bookDao = BookDao.builder()
        .id("bookId")
        .owner(ownerDao)
        .language("English")
        .condition("New")
        .genres(List.of(GenreDao.builder().id("genreId").name("Genre Name").parent(null).build()))
        .build();

    UserDao userDao = UserDao.builder().id("id").favBooks(null).build();
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
    UserDao existingDao = UserDao.builder()
        .email(email)
        .firstName(firstName)
        .lastName(lastName)
        .salt(googleSub)
        .isEmailVerified(true)
        .build();
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingDao));

    User result = userService.findOrCreateGoogleUser(email, firstName, lastName, googleSub);
    assertEquals(email, result.email());
    assertEquals(firstName, result.firstName());
    assertEquals(lastName, result.lastName());
  }

  @Test
  @DisplayName("Should create and return new user for findOrCreateGoogleUser if user does not exist")
  void findOrCreateGoogleUser_createsNewUser() {
    String email = "new@example.com";
    String firstName = "New";
    String lastName = "User";
    String googleSub = "google-sub-456";

    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

    UserDao savedDao = UserDao.builder()
        .email(email)
        .firstName(firstName)
        .lastName(lastName)
        .salt(googleSub)
        .isEmailVerified(true)
        .build();

    when(userRepository.save(any(UserDao.class))).thenReturn(savedDao);

    User result = userService.findOrCreateGoogleUser(email, firstName, lastName, googleSub);
    assertEquals(email, result.email());
    assertEquals(firstName, result.firstName());
    assertEquals(lastName, result.lastName());
  }
}
