/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirjaswappi.backend.config.TestContainersConfig;
import com.kirjaswappi.backend.http.dtos.requests.*;
import com.kirjaswappi.backend.jpa.daos.BookDao;
import com.kirjaswappi.backend.jpa.daos.SwapConditionDao;
import com.kirjaswappi.backend.jpa.daos.UserDao;
import com.kirjaswappi.backend.jpa.repositories.BookRepository;
import com.kirjaswappi.backend.jpa.repositories.GenreRepository;
import com.kirjaswappi.backend.jpa.repositories.UserRepository;

/**
 * Comprehensive integration tests for User API endpoints. Tests the complete
 * user lifecycle including signup, verification, login, profile updates,
 * password management, and user books.
 */
@SpringBootTest
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class UserApiIntegrationTest {

  private static final String API_BASE = "/api/v1/users";

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private BookRepository bookRepository;

  @Autowired
  private GenreRepository genreRepository;

  @Autowired
  private ObjectMapper objectMapper;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    // Clean up existing data
    bookRepository.deleteAll();
    userRepository.deleteAll();
    genreRepository.deleteAll();
  }

  @Nested
  @DisplayName("User Signup Tests")
  class UserSignupTests {

    @Test
    @DisplayName("Should create user successfully with valid data")
    void shouldCreateUserSuccessfully() throws Exception {
      CreateUserRequest request = new CreateUserRequest();
      request.setFirstName("John");
      request.setLastName("Doe");
      request.setEmail("john.doe@example.com");
      request.setPassword("SecureP@ss123");
      request.setConfirmPassword("SecureP@ss123");

      mockMvc.perform(post(API_BASE + "/signup")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    @DisplayName("Should return 400 when email is invalid format")
    void shouldReturn400WhenEmailInvalid() throws Exception {
      CreateUserRequest request = new CreateUserRequest();
      request.setFirstName("John");
      request.setLastName("Doe");
      request.setEmail("invalid-email");
      request.setPassword("SecureP@ss123");
      request.setConfirmPassword("SecureP@ss123");

      mockMvc.perform(post(API_BASE + "/signup")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when passwords do not match")
    void shouldReturn400WhenPasswordsMismatch() throws Exception {
      CreateUserRequest request = new CreateUserRequest();
      request.setFirstName("John");
      request.setLastName("Doe");
      request.setEmail("john.doe@example.com");
      request.setPassword("SecureP@ss123");
      request.setConfirmPassword("DifferentPassword");

      mockMvc.perform(post(API_BASE + "/signup")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when required fields are missing")
    void shouldReturn400WhenFieldsMissing() throws Exception {
      CreateUserRequest request = new CreateUserRequest();
      request.setEmail("john.doe@example.com");
      // Missing firstName, lastName, password, confirmPassword

      mockMvc.perform(post(API_BASE + "/signup")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when email already exists")
    void shouldReturn400WhenEmailExists() throws Exception {
      // Create existing user
      UserDao existingUser = UserDao.builder()
          .firstName("Existing")
          .lastName("User")
          .email("existing@example.com")
          .password("password")
          .salt("salt")
          .isEmailVerified(true)
          .build();
      userRepository.save(existingUser);

      CreateUserRequest request = new CreateUserRequest();
      request.setFirstName("John");
      request.setLastName("Doe");
      request.setEmail("existing@example.com");
      request.setPassword("SecureP@ss123");
      request.setConfirmPassword("SecureP@ss123");

      mockMvc.perform(post(API_BASE + "/signup")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("User Login Tests")
  class UserLoginTests {

    @Test
    @DisplayName("Should return 404 when user not found")
    void shouldReturn404WhenUserNotFound() throws Exception {
      AuthenticateUserRequest request = new AuthenticateUserRequest();
      request.setEmail("nonexistent@example.com");
      request.setPassword("password");

      mockMvc.perform(post(API_BASE + "/login")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 when request body is empty")
    void shouldReturn400WhenRequestEmpty() throws Exception {
      mockMvc.perform(post(API_BASE + "/login")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{}"))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("Verify Email Tests")
  class VerifyEmailTests {

    @Test
    @DisplayName("Should return 400 when email is missing")
    void shouldReturn400WhenEmailMissing() throws Exception {
      String requestBody = """
          {
            "otp": "123456"
          }
          """;

      mockMvc.perform(post(API_BASE + "/verify-email")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when OTP is missing")
    void shouldReturn400WhenOtpMissing() throws Exception {
      String requestBody = """
          {
            "email": "test@example.com"
          }
          """;

      mockMvc.perform(post(API_BASE + "/verify-email")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when request body is empty")
    void shouldReturn400WhenRequestBodyEmpty() throws Exception {
      mockMvc.perform(post(API_BASE + "/verify-email")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when email format is invalid")
    void shouldReturn400WhenEmailFormatInvalid() throws Exception {
      String requestBody = """
          {
            "email": "invalid-email",
            "otp": "123456"
          }
          """;

      mockMvc.perform(post(API_BASE + "/verify-email")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 404 when OTP is not found")
    void shouldReturn404WhenOtpNotFound() throws Exception {
      String requestBody = """
          {
            "email": "notregistered@example.com",
            "otp": "123456"
          }
          """;

      mockMvc.perform(post(API_BASE + "/verify-email")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("Login With Google Tests")
  class LoginWithGoogleTests {

    @Test
    @DisplayName("Should return 401 when Google ID token is null")
    void shouldReturn401WhenIdTokenIsNull() throws Exception {
      String requestBody = """
          {
            "idToken": null
          }
          """;

      mockMvc.perform(post(API_BASE + "/login-with-google")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when Google ID token is empty")
    void shouldReturn401WhenIdTokenIsEmpty() throws Exception {
      String requestBody = """
          {
            "idToken": ""
          }
          """;

      mockMvc.perform(post(API_BASE + "/login-with-google")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when Google ID token is invalid")
    void shouldReturn401WhenIdTokenIsInvalid() throws Exception {
      String requestBody = """
          {
            "idToken": "invalid.token.value"
          }
          """;

      mockMvc.perform(post(API_BASE + "/login-with-google")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when Google ID token is malformed JWT")
    void shouldReturn401WhenIdTokenIsMalformedJwt() throws Exception {
      String requestBody = """
          {
            "idToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.invalid"
          }
          """;

      mockMvc.perform(post(API_BASE + "/login-with-google")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 400 when request body is missing")
    void shouldReturn400WhenRequestBodyMissing() throws Exception {
      mockMvc.perform(post(API_BASE + "/login-with-google")
          .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("Get User Tests")
  class GetUserTests {

    @Test
    @DisplayName("Should return user by ID")
    void shouldReturnUserById() throws Exception {
      UserDao user = createTestUser("Test", "User", "test@example.com");

      mockMvc.perform(get(API_BASE + "/" + user.id()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.email").value("test@example.com"))
          .andExpect(jsonPath("$.firstName").value("Test"))
          .andExpect(jsonPath("$.lastName").value("User"));
    }

    @Test
    @DisplayName("Should return 404 for non-existent user")
    void shouldReturn404ForNonExistentUser() throws Exception {
      // Use a valid 24-character hex string format that doesn't exist
      mockMvc.perform(get(API_BASE + "/000000000000000000000000"))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return all users")
    void shouldReturnAllUsers() throws Exception {
      createTestUser("User1", "Last1", "user1@example.com");
      createTestUser("User2", "Last2", "user2@example.com");
      createTestUser("User3", "Last3", "user3@example.com");

      mockMvc.perform(get(API_BASE))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("Should return empty list when no users exist")
    void shouldReturnEmptyListWhenNoUsers() throws Exception {
      mockMvc.perform(get(API_BASE))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    }
  }

  @Nested
  @DisplayName("Update User Tests")
  class UpdateUserTests {

    @Test
    @DisplayName("Should update user successfully")
    void shouldUpdateUserSuccessfully() throws Exception {
      UserDao user = createTestUser("Original", "Name", "original@example.com");

      UpdateUserRequest request = new UpdateUserRequest();
      request.setId(user.id());
      request.setFirstName("Updated");
      request.setLastName("Name");
      request.setStreetName("New Street");
      request.setHouseNumber("42");
      request.setZipCode(12345);
      request.setCity("Helsinki");
      request.setCountry("Finland");
      request.setPhoneNumber("+358401234567");
      request.setAboutMe("I love books!");
      request.setFavGenres(List.of());

      mockMvc.perform(put(API_BASE + "/" + user.id())
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.firstName").value("Updated"))
          .andExpect(jsonPath("$.city").value("Helsinki"))
          .andExpect(jsonPath("$.aboutMe").value("I love books!"));
    }

    @Test
    @DisplayName("Should return 400 when path ID and body ID mismatch")
    void shouldReturn400WhenIdMismatch() throws Exception {
      UserDao user = createTestUser("Test", "User", "test@example.com");

      UpdateUserRequest request = new UpdateUserRequest();
      request.setId("different-id");
      request.setFirstName("Test");
      request.setLastName("User");
      request.setFavGenres(List.of());

      mockMvc.perform(put(API_BASE + "/" + user.id())
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent user")
    void shouldReturn404WhenUpdatingNonExistentUser() throws Exception {
      // Use a valid 24-character hex string format that doesn't exist
      String nonExistentId = "000000000000000000000000";
      UpdateUserRequest request = new UpdateUserRequest();
      request.setId(nonExistentId);
      request.setFirstName("Test");
      request.setLastName("User");
      request.setFavGenres(List.of());

      mockMvc.perform(put(API_BASE + "/" + nonExistentId)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("Delete User Tests")
  class DeleteUserTests {

    @Test
    @DisplayName("Should delete user successfully")
    void shouldDeleteUserSuccessfully() throws Exception {
      UserDao user = createTestUser("Delete", "Me", "delete@example.com");

      mockMvc.perform(delete(API_BASE + "/" + user.id()))
          .andExpect(status().isNoContent());

      // Verify user is deleted
      mockMvc.perform(get(API_BASE + "/" + user.id()))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("User Books Tests")
  class UserBooksTests {

    @Test
    @DisplayName("Should return user books with pagination")
    void shouldReturnUserBooksWithPagination() throws Exception {
      UserDao user = createTestUser("Book", "Owner", "owner@example.com");

      // Create test books
      for (int i = 1; i <= 5; i++) {
        createTestBook("Book " + i, "Author " + i, user);
      }

      mockMvc.perform(get(API_BASE + "/" + user.id() + "/books")
          .param("page", "0")
          .param("size", "3"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.page.totalElements").value(5))
          .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    @Test
    @DisplayName("Should return empty page when user has no books")
    void shouldReturnEmptyPageWhenNoBooks() throws Exception {
      UserDao user = createTestUser("No", "Books", "nobooks@example.com");

      mockMvc.perform(get(API_BASE + "/" + user.id() + "/books"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    @DisplayName("Should return empty page when user not found")
    void shouldReturnEmptyPageWhenUserNotFoundForBooks() throws Exception {
      // Use a valid 24-character hex string format that doesn't exist
      // API returns empty page instead of 404 for non-existent users
      mockMvc.perform(get(API_BASE + "/000000000000000000000000/books"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.page.totalElements").value(0));
    }
  }

  @Nested
  @DisplayName("Favourite Books Tests")
  class FavouriteBooksTests {

    @Test
    @DisplayName("Should return 400 when user ID is missing")
    void shouldReturn400WhenUserIdMissing() throws Exception {
      AddFavouriteBookRequest request = new AddFavouriteBookRequest();
      request.setBookId("book-id");

      mockMvc.perform(post(API_BASE + "/favourite-books")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when book ID is missing")
    void shouldReturn400WhenBookIdMissing() throws Exception {
      AddFavouriteBookRequest request = new AddFavouriteBookRequest();
      request.setUserId("user-id");

      mockMvc.perform(post(API_BASE + "/favourite-books")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("Password Management Tests")
  class PasswordManagementTests {

    @Test
    @DisplayName("Should return 400 when passwords do not match for change password")
    void shouldReturn400WhenPasswordsMismatchForChangePassword() throws Exception {
      ChangePasswordRequest request = new ChangePasswordRequest();
      request.setCurrentPassword("currentPassword");
      request.setNewPassword("newPassword");
      request.setConfirmPassword("differentPassword");

      mockMvc.perform(post(API_BASE + "/change-password/test@example.com")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when passwords do not match for reset password")
    void shouldReturn400WhenPasswordsMismatchForResetPassword() throws Exception {
      ResetPasswordRequest request = new ResetPasswordRequest();
      request.setNewPassword("newPassword");
      request.setConfirmPassword("differentPassword");

      mockMvc.perform(post(API_BASE + "/reset-password/test@example.com")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }

  // Helper methods
  private UserDao createTestUser(String firstName, String lastName, String email) {
    UserDao user = UserDao.builder()
        .firstName(firstName)
        .lastName(lastName)
        .email(email)
        .password("password")
        .salt("salt")
        .isEmailVerified(true)
        .build();
    return userRepository.save(user);
  }

  private BookDao createTestBook(String title, String author, UserDao owner) {
    BookDao book = BookDao.builder()
        .title(title)
        .author(author)
        .condition("Good")
        .language("English")
        .owner(owner)
        .coverPhotos(List.of())
        .genres(List.of())
        .swapCondition(SwapConditionDao.builder()
            .swapType("GiveAway")
            .giveAway(true)
            .build())
        .build();
    return bookRepository.save(book);
  }
}
