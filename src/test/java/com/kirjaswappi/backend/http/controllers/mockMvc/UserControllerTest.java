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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirjaswappi.backend.common.service.OTPService;
import com.kirjaswappi.backend.common.service.exceptions.InvalidCredentials;
import com.kirjaswappi.backend.http.controllers.UserController;
import com.kirjaswappi.backend.http.controllers.mockMvc.config.CustomMockMvcConfiguration;
import com.kirjaswappi.backend.http.dtos.requests.*;
import com.kirjaswappi.backend.http.dtos.responses.*;
import com.kirjaswappi.backend.service.UserService;
import com.kirjaswappi.backend.service.entities.Genre;
import com.kirjaswappi.backend.service.entities.User;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;
import com.kirjaswappi.backend.service.exceptions.BookNotFoundException;
import com.kirjaswappi.backend.service.exceptions.UserNotFoundException;

@WebMvcTest(UserController.class)
@Import(CustomMockMvcConfiguration.class)
public class UserControllerTest {
  private static final String API_BASE = "/api/v1/users";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private UserService userService;

  @MockBean
  private OTPService otpService;

  private User user;

  private CreateUserRequest createUserRequest;

  @BeforeEach
  void setUp() {
    user = new User();
    user.setId("1");
    user.setEmail("test@example.com");

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
    when(otpService.saveAndSendOTP(any(String.class))).thenReturn(user.getEmail());

    mockMvc.perform(post(API_BASE + "/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(createUserRequest))
        .header("Authorization ", "Bearer a.b.c"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value(user.getEmail()));
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
        .andExpect(jsonPath("$.email").value(user.getEmail()));
  }

  @Test
  @DisplayName("Should get all users")
  void shouldGetUsers() throws Exception {
    when(userService.getUsers()).thenReturn(List.of(user));

    mockMvc.perform(get(API_BASE)
        .header("Authorization ", "Bearer a.b.c"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].email").value(user.getEmail()));
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
        .andExpect(jsonPath("$.email").value(user.getEmail()));
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

  private static User getUpdatedUser() {
    User updatedUser = new User();
    updatedUser.setId("1");
    updatedUser.setFirstName("UpdatedFirstName");
    updatedUser.setLastName("UpdatedLastName");
    updatedUser.setStreetName("UpdatedStreetName");
    updatedUser.setHouseNumber("UpdatedHouseNumber");
    updatedUser.setZipCode(12345);
    updatedUser.setCity("UpdatedCity");
    updatedUser.setCountry("UpdatedCountry");
    updatedUser.setPhoneNumber("UpdatedPhoneNumber");
    updatedUser.setAboutMe("UpdatedAboutMe");
    updatedUser.setFavGenres(List.of(new Genre("GenreId1", "UpdatedGenre1"),
        new Genre("GenreId2", "UpdatedGenre2")));
    return updatedUser;
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
}