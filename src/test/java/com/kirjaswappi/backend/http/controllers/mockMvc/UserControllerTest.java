/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.controllers.mockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.kirjaswappi.backend.common.http.controllers.mockMvc.config.CustomMockMvcConfiguration;
import com.kirjaswappi.backend.common.service.OTPService;
import com.kirjaswappi.backend.common.service.exceptions.InvalidCredentials;
import com.kirjaswappi.backend.http.controllers.UserController;
import com.kirjaswappi.backend.http.dtos.requests.*;
import com.kirjaswappi.backend.service.BookService;
import com.kirjaswappi.backend.service.UserService;
import com.kirjaswappi.backend.service.entities.Book;
import com.kirjaswappi.backend.service.entities.Genre;
import com.kirjaswappi.backend.service.entities.User;
import com.kirjaswappi.backend.service.enums.Condition;
import com.kirjaswappi.backend.service.enums.Language;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;
import com.kirjaswappi.backend.service.exceptions.BookNotFoundException;
import com.kirjaswappi.backend.service.exceptions.UserNotFoundException;
import com.kirjaswappi.backend.service.filters.FindAllBooksFilter;

@WebMvcTest(UserController.class)
@Import(CustomMockMvcConfiguration.class)
public class UserControllerTest {
  private static final String API_BASE = "/api/v1/users";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private OTPService otpService;

  @MockitoBean
  private BookService bookService;

  @MockitoBean
  private GoogleIdTokenVerifier googleIdTokenVerifier;

  private User user;

  private CreateUserRequest createUserRequest;

  @BeforeEach
  void setUp() {
    user = User.builder()
        .id("1")
        .email("test@example.com")
        .build();

    createUserRequest = new CreateUserRequest();
    createUserRequest.setFirstName("Test");
    createUserRequest.setLastName("User");
    createUserRequest.setEmail("test@example.com");
    createUserRequest.setPassword("password");
    createUserRequest.setConfirmPassword("password");
  }

  @Test
  @DisplayName("Should create a new user")
  void shouldCreateUser() throws Exception {
    when(userService.addUser(any(User.class))).thenReturn(user);
    when(otpService.saveAndSendOTP(any(String.class))).thenReturn(user.email());

    mockMvc.perform(post(API_BASE + "/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(createUserRequest))
        .header("Authorization ", "Bearer a.b.c"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value(user.email()));
  }

  @Test
  @DisplayName("Should verify email")
  void shouldVerifyEmail() throws Exception {
    VerifyEmailRequest request = new VerifyEmailRequest();
    request.setEmail("test@example.com");
    request.setOtp("123456");

    when(otpService.verifyOTPByEmail(any())).thenReturn("test@example.com");
    when(userService.verifyEmail(any())).thenReturn("test@example.com");

    mockMvc.perform(post(API_BASE + "/verify-email")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
        .header("Authorization ", "Bearer a.b.c"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("test@example.com verified successfully."));
  }

  @Test
  @DisplayName("Should update user")
  void shouldUpdateUser() throws Exception {
    UpdateUserRequest request = getUserUpdateRequest();
    User updatedUser = getUpdatedUser();

    when(userService.updateUser(any(User.class))).thenReturn(updatedUser);

    mockMvc.perform(put(API_BASE + "/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
        .header("Authorization ", "Bearer a.b.c"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName").value("UpdatedFirstName"));
  }

  @Test
  @DisplayName("Should get user by ID")
  void shouldGetUser() throws Exception {
    when(userService.getUser("1")).thenReturn(user);

    mockMvc.perform(get(API_BASE + "/1")
        .header("Authorization ", "Bearer a.b.c"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(user.email()));
  }

  @Test
  @DisplayName("Should get all users")
  void shouldGetUsers() throws Exception {
    when(userService.getUsers()).thenReturn(List.of(user));

    mockMvc.perform(get(API_BASE)
        .header("Authorization ", "Bearer a.b.c"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].email").value(user.email()));
  }

  @Test
  @DisplayName("Should delete user by ID")
  void shouldDeleteUser() throws Exception {
    mockMvc.perform(delete(API_BASE + "/1")
        .header("Authorization ", "Bearer a.b.c"))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("Should login user")
  void shouldLogin() throws Exception {
    AuthenticateUserRequest request = new AuthenticateUserRequest();
    request.setEmail("test@example.com");
    request.setPassword("password");

    when(userService.verifyLogin(any(User.class))).thenReturn(user);

    mockMvc.perform(post(API_BASE + "/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
        .header("Authorization ", "Bearer a.b.c"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(user.email()));
  }

  @Test
  @DisplayName("Should change user password")
  void shouldChangePassword() throws Exception {
    ChangePasswordRequest request = new ChangePasswordRequest();
    request.setCurrentPassword("currentPassword");
    request.setNewPassword("newPassword");
    request.setConfirmPassword("newPassword");

    when(userService.changePassword(any())).thenReturn("test@example.com");

    mockMvc.perform(post(API_BASE + "/change-password/test@example.com")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
        .header("Authorization ", "Bearer a.b.c"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Password changed for user: test@example.com"));
  }

  @Test
  @DisplayName("Should reset user password")
  void shouldResetPassword() throws Exception {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setNewPassword("newPassword");
    request.setConfirmPassword("newPassword");

    when(userService.changePassword(any())).thenReturn("test@example.com");

    mockMvc.perform(post(API_BASE + "/reset-password/test@example.com")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
        .header("Authorization ", "Bearer a.b.c"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Password changed for user: test@example.com"));
  }

  @Test
  @DisplayName("Should find books by user ID with filter")
  void shouldFindUserBooks() throws Exception {
    // Prepare test data
    String userId = "1";
    var book = Book.builder()
        .id("book123")
        .title("Test")
        .author("Test Author")
        .genres(List.of())
        .language(Language.ENGLISH)
        .condition(Condition.FAIR)
        .owner(user)
        .build();

    Page<@NonNull Book> bookPage = new PageImpl<>(List.of(book), PageRequest.of(0, 10), 1);

    // Setup mocks
    when(bookService.getUserBooksByFilter(
        Mockito.eq(userId),
        any(FindAllBooksFilter.class),
        any(Pageable.class)))
            .thenReturn(bookPage);

    // Execute and verify
    mockMvc.perform(get(API_BASE + "/" + userId + "/books")
        .param("page", "0")
        .param("size", "10")
        .header("Authorization", "Bearer a.b.c"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.books[0].id").value("book123"))
        .andExpect(jsonPath("$._embedded.books[0].title").value("Test"))
        .andExpect(jsonPath("$._embedded.books[0].author").value("Test Author"))
        .andExpect(jsonPath("$.page.totalElements").value(1));
  }

  @Test
  @DisplayName("Should return 404 when user ID not found in findUserBooks")
  void shouldReturnNotFoundWhenUserIdNotFoundInFindUserBooks() throws Exception {
    when(bookService.getUserBooksByFilter(
        Mockito.eq("non-existent"),
        any(FindAllBooksFilter.class),
        any(Pageable.class)))
            .thenThrow(new UserNotFoundException("non-existent"));

    mockMvc.perform(get(API_BASE + "/non-existent/books")
        .header("Authorization", "Bearer a.b.c"))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return empty page when user has no books")
  void shouldReturnEmptyPageWhenUserHasNoBooks() throws Exception {
    Page<Book> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

    when(bookService.getUserBooksByFilter(
        Mockito.eq("1"),
        any(FindAllBooksFilter.class),
        any(Pageable.class)))
            .thenReturn(emptyPage);

    mockMvc.perform(get(API_BASE + "/1/books")
        .header("Authorization", "Bearer a.b.c"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements").value(0));
  }

  private static User getUpdatedUser() {
    return User.builder()
        .id("1")
        .firstName("UpdatedFirstName")
        .lastName("UpdatedLastName")
        .streetName("UpdatedStreetName")
        .houseNumber("UpdatedHouseNumber")
        .zipCode(12345)
        .city("UpdatedCity")
        .country("UpdatedCountry")
        .phoneNumber("UpdatedPhoneNumber")
        .aboutMe("UpdatedAboutMe")
        .favGenres(List.of(
            new Genre("GenreId1", "UpdatedGenre1", null),
            new Genre("GenreId2", "UpdatedGenre2", null)))
        .build();
  }

  private static UpdateUserRequest getUserUpdateRequest() {
    UpdateUserRequest request = new UpdateUserRequest();
    request.setId("1");
    request.setFirstName("UpdatedFirstName");
    request.setLastName("UpdatedLastName");
    request.setStreetName("UpdatedStreetName");
    request.setHouseNumber("UpdatedHouseNumber");
    request.setZipCode(12345);
    request.setCity("UpdatedCity");
    request.setCountry("UpdatedCountry");
    request.setPhoneNumber("UpdatedPhoneNumber");
    request.setAboutMe("UpdatedAboutMe");
    request.setFavGenres(List.of("UpdatedGenre1", "UpdatedGenre2"));
    return request;
  }

  // Negative Cases:
  @Test
  @DisplayName("Should return 400 when required fields are missing")
  void shouldReturnBadRequestWhenFieldsAreMissing() throws Exception {
    CreateUserRequest invalidRequest = new CreateUserRequest();
    mockMvc.perform(post(API_BASE + "/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidRequest))
        .header("Authorization", "Bearer a.b.c"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when password and confirm password mismatch")
  void shouldReturnBadRequestWhenPasswordMismatch() throws Exception {
    createUserRequest.setConfirmPassword("differentPassword");

    mockMvc.perform(post(API_BASE + "/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(createUserRequest))
        .header("Authorization", "Bearer a.b.c"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when email format is invalid")
  void shouldReturnBadRequestWhenEmailFormatIsInvalid() throws Exception {
    createUserRequest.setEmail("invalidEmailFormat");

    mockMvc.perform(post(API_BASE + "/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(createUserRequest))
        .header("Authorization", "Bearer a.b.c"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when email already exists")
  void shouldReturnBadRequestWhenEmailAlreadyExists() throws Exception {
    when(userService.addUser(any(User.class))).thenThrow(new BadRequestException("Email already exists"));

    mockMvc.perform(post(API_BASE + "/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(createUserRequest))
        .header("Authorization", "Bearer a.b.c"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when OTP is invalid or expired")
  void shouldReturnBadRequestWhenOTPIsInvalid() throws Exception {
    VerifyEmailRequest request = new VerifyEmailRequest();
    request.setEmail("test@example.com");
    request.setOtp("invalidOtp");

    when(otpService.verifyOTPByEmail(any()))
        .thenThrow(new BadRequestException("otpNotFound", request.getEmail()));

    mockMvc.perform(post(API_BASE + "/verify-email")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
        .header("Authorization", "Bearer a.b.c"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when OTP and email do not match")
  void shouldReturnBadRequestWhenOTPAndEmailMismatch() throws Exception {
    VerifyEmailRequest request = new VerifyEmailRequest();
    request.setEmail("test@example.com");
    request.setOtp("123456");

    when(otpService.verifyOTPByEmail(any()))
        .thenThrow(new BadRequestException("otpDoesNotMatch", request.getOtp()));

    mockMvc.perform(post(API_BASE + "/verify-email")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
        .header("Authorization", "Bearer a.b.c"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when request body is missing required fields")
  void shouldReturnBadRequestWhenMissingRequiredFields() throws Exception {
    AddFavouriteBookRequest invalidRequest = new AddFavouriteBookRequest();
    mockMvc.perform(post(API_BASE + "/favourite-books")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidRequest))
        .header("Authorization", "Bearer a.b.c"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when user does not exist")
  void shouldReturnBadRequestWhenUserDoesNotExistForFavBook() throws Exception {
    AddFavouriteBookRequest request = new AddFavouriteBookRequest();
    request.setUserId("nonExistentUser");

    when(userService.addFavouriteBook(any()))
        .thenThrow(new UserNotFoundException());

    mockMvc.perform(post(API_BASE + "/favourite-books")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
        .header("Authorization", "Bearer a.b.c"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when book does not exist")
  void shouldReturnNotFoundWhenBookDoesNotExist() throws Exception {
    AddFavouriteBookRequest request = new AddFavouriteBookRequest();
    request.setBookId("nonExistentBook");

    when(userService.addFavouriteBook(any()))
        .thenThrow(new BookNotFoundException());

    mockMvc.perform(post(API_BASE + "/favourite-books")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
        .header("Authorization", "Bearer a.b.c"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when ID mismatch between path variable and request body")
  void shouldReturnBadRequestWhenIdMismatch() throws Exception {
    UpdateUserRequest request = getUserUpdateRequest();
    request.setId("2"); // Mismatched ID

    mockMvc.perform(put(API_BASE + "/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
        .header("Authorization", "Bearer a.b.c"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 404 when user does not exist")
  void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
    UpdateUserRequest request = getUserUpdateRequest();

    when(userService.updateUser(any(User.class))).thenThrow(new UserNotFoundException());

    mockMvc.perform(put(API_BASE + "/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
        .header("Authorization", "Bearer a.b.c"))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 404 when user does not exist")
  void shouldReturnNotFoundWhenUserDoesNotExistWhenFetchingUser() throws Exception {
    when(userService.getUser("1")).thenThrow(new UserNotFoundException());

    mockMvc.perform(get(API_BASE + "/1")
        .header("Authorization", "Bearer a.b.c"))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 401 when password is incorrect")
  void shouldReturnUnauthorizedWhenPasswordIsIncorrect() throws Exception {
    AuthenticateUserRequest request = new AuthenticateUserRequest();
    request.setEmail("test@example.com");
    request.setPassword("wrongPassword");

    when(userService.verifyLogin(any(User.class))).thenThrow(new InvalidCredentials());

    mockMvc.perform(post(API_BASE + "/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
        .header("Authorization", "Bearer a.b.c"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 404 when email is not registered")
  void shouldReturnNotFoundWhenEmailIsNotRegistered() throws Exception {
    AuthenticateUserRequest request = new AuthenticateUserRequest();
    request.setEmail("nonexistent@example.com");
    request.setPassword("password");

    when(userService.verifyLogin(any(User.class))).thenThrow(new UserNotFoundException());

    mockMvc.perform(post(API_BASE + "/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
        .header("Authorization", "Bearer a.b.c"))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 400 when new and confirm password do not match")
  void shouldReturnBadRequestWhenPasswordsDoNotMatch() throws Exception {
    ChangePasswordRequest request = new ChangePasswordRequest();
    request.setCurrentPassword("currentPassword");
    request.setNewPassword("newPassword");
    request.setConfirmPassword("differentPassword");

    mockMvc.perform(post(API_BASE + "/change-password/test@example.com")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
        .header("Authorization", "Bearer a.b.c"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should login with Google successfully")
  void shouldLoginWithGoogle() throws Exception {
    String idTokenString = "valid.token";
    String googleSub = "google-sub-123";
    String firstName = "Test";
    String lastName = "User";
    String email = "test@example.com";

    GoogleIdToken.Payload payload = Mockito.mock(GoogleIdToken.Payload.class);
    Mockito.when(payload.getEmail()).thenReturn(email);
    Mockito.when(payload.get("given_name")).thenReturn(firstName);
    Mockito.when(payload.get("family_name")).thenReturn(lastName);
    Mockito.when(payload.getSubject()).thenReturn(googleSub);

    GoogleIdToken idToken = Mockito.mock(GoogleIdToken.class);
    Mockito.when(idToken.getPayload()).thenReturn(payload);
    Mockito.when(googleIdTokenVerifier.verify(idTokenString)).thenReturn(idToken);
    Mockito.when(userService.findOrCreateGoogleUser(email, firstName, lastName, googleSub)).thenReturn(user);

    mockMvc.perform(post(API_BASE + "/login-with-google")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"idToken\":\"" + idTokenString + "\"}")
        .header("Authorization", "Bearer a.b.c"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(user.email()));
  }

  @Test
  @DisplayName("Should fail Google login with invalid token")
  void shouldFailLoginWithGoogleInvalidToken() throws Exception {
    String idTokenString = "invalid.token";
    Mockito.when(googleIdTokenVerifier.verify(idTokenString)).thenThrow(new RuntimeException("Invalid token"));

    mockMvc.perform(post(API_BASE + "/login-with-google")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"idToken\":\"" + idTokenString + "\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().string("Invalid token"));
  }
}
