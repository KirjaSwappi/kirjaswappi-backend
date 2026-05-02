/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.controllers;

import static com.kirjaswappi.backend.common.utils.Constants.*;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.kirjaswappi.backend.common.http.ErrorResponse;
import com.kirjaswappi.backend.common.service.OTPService;
import com.kirjaswappi.backend.common.service.RateLimiterService;
import com.kirjaswappi.backend.common.utils.JwtUtil;
import com.kirjaswappi.backend.common.utils.LinkBuilder;
import com.kirjaswappi.backend.http.dtos.requests.*;
import com.kirjaswappi.backend.http.dtos.responses.*;
import com.kirjaswappi.backend.service.BookService;
import com.kirjaswappi.backend.service.UserService;
import com.kirjaswappi.backend.service.entities.Book;
import com.kirjaswappi.backend.service.entities.User;
import com.kirjaswappi.backend.service.exceptions.AccessDeniedException;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;
import com.kirjaswappi.backend.service.filters.FindAllBooksFilter;

@RestController
@RequestMapping(API_BASE + USERS)
@Validated
@Tag(name = "Users", description = "API for managing user accounts and authentication")
public class UserController {
  @Autowired
  private UserService userService;

  @Autowired
  private BookService bookService;

  @Autowired
  private OTPService otpService;

  @Autowired
  private GoogleIdTokenVerifier googleIdTokenVerifier;

  @Autowired
  private JwtUtil jwtUtil;

  @Autowired
  private RateLimiterService rateLimiterService;

  private static final int MAX_LOGIN_ATTEMPTS = 10;
  private static final Duration LOGIN_RATE_LIMIT_WINDOW = Duration.ofMinutes(15);
  private static final String LOGIN_RATE_LIMIT_PREFIX = "ratelimit:login:";

  private static final int MAX_CHANGE_PW_ATTEMPTS = 5;
  private static final Duration CHANGE_PW_RATE_LIMIT_WINDOW = Duration.ofMinutes(15);
  private static final String CHANGE_PW_RATE_LIMIT_PREFIX = "ratelimit:change-password:";

  private static final int MAX_RESET_PW_ATTEMPTS = 5;
  private static final Duration RESET_PW_RATE_LIMIT_WINDOW = Duration.ofMinutes(15);
  private static final String RESET_PW_RATE_LIMIT_PREFIX = "ratelimit:reset-password:";

  private static final int MAX_REFRESH_TOKEN_ATTEMPTS = 30;
  private static final Duration REFRESH_TOKEN_RATE_LIMIT_WINDOW = Duration.ofMinutes(15);
  private static final String REFRESH_TOKEN_RATE_LIMIT_PREFIX = "ratelimit:refresh-token:";

  private static final int MAX_GOOGLE_LOGIN_ATTEMPTS = 20;
  private static final Duration GOOGLE_LOGIN_RATE_LIMIT_WINDOW = Duration.ofMinutes(15);
  private static final String GOOGLE_LOGIN_RATE_LIMIT_PREFIX = "ratelimit:google-login:";

  @PostMapping(SIGNUP)
  @Operation(summary = "Create user.", responses = {
      @ApiResponse(responseCode = "201", description = "User created.") })
  public ResponseEntity<CreateUserResponse> createUser(@Valid @RequestBody CreateUserRequest user) throws IOException {
    User savedUser = userService.addUser(user.toEntity());
    otpService.saveAndSendOTP(savedUser.email());
    return ResponseEntity.status(HttpStatus.CREATED).body(new CreateUserResponse(savedUser));
  }

  @PostMapping(VERIFY_EMAIL)
  @Operation(summary = "Verify email.", responses = {
      @ApiResponse(responseCode = "200", description = "Email verified.") })
  public ResponseEntity<VerifyEmailResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
    String email = otpService.verifyOTPByEmail(request.toEntity());
    String verifiedEmail = userService.verifyEmail(email);
    return ResponseEntity.status(HttpStatus.OK).body(new VerifyEmailResponse(verifiedEmail));
  }

  @PostMapping(FAVOURITE_BOOKS)
  @Operation(summary = "Add a favourite book to a user.", responses = {
      @ApiResponse(responseCode = "200", description = "Book added to favourite list.") })
  public ResponseEntity<UserResponse> addFavouriteBook(@Valid @RequestBody AddFavouriteBookRequest request) {
    verifyUserIdentity(request.toEntity().id());
    User entity = request.toEntity();
    User updatedUser = userService.addFavouriteBook(entity);
    return ResponseEntity.status(HttpStatus.OK).body(new UserResponse(updatedUser));
  }

  @DeleteMapping(FAVOURITE_BOOKS + BOOK_ID)
  @Operation(summary = "Remove a favourite book from a user.", responses = {
      @ApiResponse(responseCode = "204", description = "Book removed from favourite list.") })
  public ResponseEntity<Void> removeFavouriteBook(
      @Parameter(description = "Book ID.") @PathVariable String bookId) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    String userId = authentication.getName();
    userService.removeFavouriteBook(userId, bookId);
    return ResponseEntity.noContent().build();
  }

  @PutMapping(ID)
  @Operation(summary = "Update user.", responses = {
      @ApiResponse(responseCode = "200", description = "User updated."),
      @ApiResponse(responseCode = "400", description = "Invalid request or ID mismatch."),
      @ApiResponse(responseCode = "403", description = "Not the account owner."),
      @ApiResponse(responseCode = "404", description = "User not found.") })
  public ResponseEntity<UpdateUserResponse> updateUser(@Parameter(description = "User ID.") @PathVariable String id,
      @Valid @RequestBody UpdateUserRequest user) {
    // validate id:
    if (!id.equals(user.getId())) {
      throw new BadRequestException("idMismatch", id, user.getId());
    }
    verifyUserIdentity(id);
    User updatedUser = userService.updateUser(user.toEntity());
    return ResponseEntity.status(HttpStatus.OK).body(new UpdateUserResponse(updatedUser));
  }

  @GetMapping(ID)
  @Operation(summary = "Find user by User ID.", description = "Returns the full profile when the authenticated user requests their own record; returns a minimal public profile otherwise.", responses = {
      @ApiResponse(responseCode = "200", description = "User found."),
      @ApiResponse(responseCode = "401", description = "Unauthenticated."),
      @ApiResponse(responseCode = "404", description = "User not found.") })
  public ResponseEntity<?> getUser(@Parameter(description = "User ID.") @PathVariable String id) {
    User user = userService.getUser(id);
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    String authenticatedUserId = authentication != null ? authentication.getName() : null;
    if (authenticatedUserId != null && authenticatedUserId.equals(id)) {
      return ResponseEntity.ok(new UserResponse(user));
    }
    return ResponseEntity.ok(new PublicUserResponse(user));
  }

  @GetMapping
  @Operation(summary = "Find all users (admin).", responses = {
      @ApiResponse(responseCode = "200", description = "List of users."),
      @ApiResponse(responseCode = "403", description = "Not an admin.") })
  public ResponseEntity<List<UserResponse>> getUsers() {
    List<User> users = userService.getUsers();
    return ResponseEntity.status(HttpStatus.OK).body(users.stream().map(UserResponse::new).toList());
  }

  @DeleteMapping(ID)
  @Operation(summary = "Delete user.", responses = {
      @ApiResponse(responseCode = "204", description = "User deleted."),
      @ApiResponse(responseCode = "403", description = "Not the account owner."),
      @ApiResponse(responseCode = "404", description = "User not found.") })
  public ResponseEntity<Void> deleteUser(@Parameter(description = "User ID.") @PathVariable String id) {
    verifyUserIdentity(id);
    userService.deleteUser(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping(LOGIN)
  @Operation(summary = "Login user.", responses = {
      @ApiResponse(responseCode = "200", description = "User logged in.") })
  public ResponseEntity<UserLoginResponse> login(@Valid @RequestBody AuthenticateUserRequest authenticateUserRequest) {
    String email = authenticateUserRequest.getEmail();
    String rateLimitKey = email != null ? LOGIN_RATE_LIMIT_PREFIX + email.toLowerCase() : null;
    if (rateLimitKey != null
        && rateLimiterService.isRateLimitedFailClosed(rateLimitKey, MAX_LOGIN_ATTEMPTS)) {
      throw new BadRequestException("tooManyLoginAttempts", email);
    }
    try {
      User user = userService.verifyLogin(authenticateUserRequest.toEntity());
      if (rateLimitKey != null) {
        rateLimiterService.clearAttempts(rateLimitKey);
      }
      String userToken = jwtUtil.generateUserToken(user.id(), user.email());
      String userRefreshToken = jwtUtil.generateUserRefreshToken(user.id(), user.email());
      return ResponseEntity.status(HttpStatus.OK).body(new UserLoginResponse(user, userToken, userRefreshToken));
    } catch (Exception e) {
      if (rateLimitKey != null) {
        rateLimiterService.recordAttempt(rateLimitKey, LOGIN_RATE_LIMIT_WINDOW);
      }
      throw e;
    }
  }

  @PostMapping(LOGIN_WITH_GOOGLE)
  @Operation(summary = "Login with Google.", responses = {
      @ApiResponse(responseCode = "200", description = "User logged in with Google."),
      @ApiResponse(responseCode = "401", description = "Invalid token.") })
  public ResponseEntity<?> loginWithGoogle(@RequestBody LoginWithGoogleRequest request,
      jakarta.servlet.http.HttpServletRequest httpRequest) {
    String rateLimitKey = GOOGLE_LOGIN_RATE_LIMIT_PREFIX + httpRequest.getRemoteAddr();
    if (rateLimiterService.isRateLimitedFailClosed(rateLimitKey, MAX_GOOGLE_LOGIN_ATTEMPTS)) {
      throw new BadRequestException("tooManyLoginAttempts", "");
    }
    GoogleIdToken idToken;
    try {
      idToken = googleIdTokenVerifier.verify(request.idToken());
    } catch (Exception e) {
      rateLimiterService.recordAttempt(rateLimitKey, GOOGLE_LOGIN_RATE_LIMIT_WINDOW);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new ErrorResponse(new ErrorResponse.Error("invalidGoogleToken", "Invalid Google token")));
    }
    if (idToken != null) {
      GoogleIdToken.Payload payload = idToken.getPayload();
      String email = payload.getEmail();
      String firstName = (String) payload.get("given_name");
      String lastName = (String) payload.get("family_name");
      String googleSub = payload.getSubject();

      // Find or create user
      User user = userService.findOrCreateGoogleUser(email, firstName, lastName, googleSub);
      String userToken = jwtUtil.generateUserToken(user.id(), user.email());
      String userRefreshToken = jwtUtil.generateUserRefreshToken(user.id(), user.email());
      rateLimiterService.clearAttempts(rateLimitKey);
      return ResponseEntity.status(HttpStatus.OK).body(new UserLoginResponse(user, userToken, userRefreshToken));
    }
    rateLimiterService.recordAttempt(rateLimitKey, GOOGLE_LOGIN_RATE_LIMIT_WINDOW);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new ErrorResponse(new ErrorResponse.Error("invalidIdToken", "Invalid ID token")));
  }

  @PostMapping(CHANGE_PASSWORD + EMAIL)
  @Operation(summary = "Change password.", responses = {
      @ApiResponse(responseCode = "200", description = "Password changed."),
      @ApiResponse(responseCode = "403", description = "Path email does not match authenticated user.") })
  public ResponseEntity<ChangePasswordResponse> changePassword(
      @Parameter(description = "User email.") @PathVariable String email,
      @Valid @RequestBody ChangePasswordRequest request) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getName() == null) {
      throw new AccessDeniedException("notAccountOwner", email);
    }
    User authenticated = userService.getUser(authentication.getName());
    if (!email.equalsIgnoreCase(authenticated.email())) {
      throw new AccessDeniedException("notAccountOwner", email);
    }
    String rateLimitKey = CHANGE_PW_RATE_LIMIT_PREFIX + email.toLowerCase();
    if (rateLimiterService.isRateLimitedFailClosed(rateLimitKey, MAX_CHANGE_PW_ATTEMPTS)) {
      throw new BadRequestException("tooManyChangePasswordAttempts", email);
    }
    try {
      userService.verifyCurrentPassword(request.toVerifyPasswordEntity(email));
      String userEmail = userService.changePassword(request.toChangePasswordEntity(email));
      if (request.getUserRefreshToken() != null && !request.getUserRefreshToken().isBlank()) {
        jwtUtil.revokeUserRefreshToken(request.getUserRefreshToken());
      }
      rateLimiterService.clearAttempts(rateLimitKey);
      return ResponseEntity.status(HttpStatus.OK).body(new ChangePasswordResponse(userEmail));
    } catch (Exception e) {
      rateLimiterService.recordAttempt(rateLimitKey, CHANGE_PW_RATE_LIMIT_WINDOW);
      throw e;
    }
  }

  @PostMapping(RESET_PASSWORD + EMAIL)
  @Operation(summary = "Reset password.", responses = {
      @ApiResponse(responseCode = "200", description = "Password reset successful."),
      @ApiResponse(responseCode = "400", description = "Invalid or expired reset token.") })
  public ResponseEntity<ResetPasswordResponse> resetPassword(
      @Parameter(description = "User email.") @PathVariable String email,
      @Valid @RequestBody ResetPasswordRequest request) {
    String rateLimitKey = RESET_PW_RATE_LIMIT_PREFIX + email.toLowerCase();
    if (rateLimiterService.isRateLimitedFailClosed(rateLimitKey, MAX_RESET_PW_ATTEMPTS)) {
      throw new BadRequestException("tooManyResetPasswordAttempts", email);
    }
    try {
      // Atomically validate + consume — prevents TOCTOU replay
      if (!jwtUtil.validateAndConsumePasswordResetToken(request.getResetToken())) {
        rateLimiterService.recordAttempt(rateLimitKey, RESET_PW_RATE_LIMIT_WINDOW);
        throw new BadRequestException("invalidOrExpiredResetToken", email);
      }
      String tokenEmail = jwtUtil.extractEmailFromResetToken(request.getResetToken());
      if (!email.equalsIgnoreCase(tokenEmail)) {
        rateLimiterService.recordAttempt(rateLimitKey, RESET_PW_RATE_LIMIT_WINDOW);
        throw new BadRequestException("resetTokenEmailMismatch", email);
      }
      String userEmail = userService.changePassword(request.toUserEntity(email));
      rateLimiterService.clearAttempts(rateLimitKey);
      return ResponseEntity.status(HttpStatus.OK).body(new ResetPasswordResponse(userEmail));
    } catch (BadRequestException e) {
      throw e;
    } catch (Exception e) {
      rateLimiterService.recordAttempt(rateLimitKey, RESET_PW_RATE_LIMIT_WINDOW);
      throw e;
    }
  }

  @PostMapping("/logout")
  @Operation(summary = "Log out the current user.", description = "Revokes the supplied refresh token so it cannot be used again. Access tokens expire naturally within minutes.", responses = {
      @ApiResponse(responseCode = "204", description = "Logged out.") })
  public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
    String refreshToken = request.getUserRefreshToken();
    if (refreshToken != null && !refreshToken.isBlank()) {
      var authentication = SecurityContextHolder.getContext().getAuthentication();
      String tokenSubject = jwtUtil.extractUserId(refreshToken);
      if (authentication != null && authentication.getName().equals(tokenSubject)) {
        jwtUtil.revokeUserRefreshToken(refreshToken);
      }
    }
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/refresh-token")
  @Operation(summary = "Refresh user token.", description = "Returns a fresh access token AND a rotated refresh token; the previous refresh token is revoked.", responses = {
      @ApiResponse(responseCode = "200", description = "Token refreshed."),
      @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token."),
      @ApiResponse(responseCode = "404", description = "User not found.") })
  public ResponseEntity<?> refreshUserToken(@Valid @RequestBody RefreshTokenRequest request,
      jakarta.servlet.http.HttpServletRequest httpRequest) {
    String rateLimitKey = REFRESH_TOKEN_RATE_LIMIT_PREFIX + httpRequest.getRemoteAddr();
    if (rateLimiterService.isRateLimitedFailClosed(rateLimitKey, MAX_REFRESH_TOKEN_ATTEMPTS)) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Too many refresh attempts");
    }
    String refreshToken = request.getUserRefreshToken();
    try {
      if (!jwtUtil.validateUserRefreshToken(refreshToken)) {
        rateLimiterService.recordAttempt(rateLimitKey, REFRESH_TOKEN_RATE_LIMIT_WINDOW);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired refresh token");
      }
    } catch (Exception e) {
      rateLimiterService.recordAttempt(rateLimitKey, REFRESH_TOKEN_RATE_LIMIT_WINDOW);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired refresh token");
    }
    String userId = jwtUtil.extractUserId(refreshToken);
    User user = userService.getUser(userId);
    // Rotation: issue a new refresh token AND revoke the one we just consumed.
    // If a stolen-token attacker tries to reuse the old refresh token they will
    // be denied by the revocation check, even before its 7-day expiry.
    String newAccessToken = jwtUtil.generateUserToken(user.id(), user.email());
    String newRefreshToken = jwtUtil.generateUserRefreshToken(user.id(), user.email());
    jwtUtil.revokeUserRefreshToken(refreshToken);
    return ResponseEntity.ok(Map.of(
        "userToken", newAccessToken,
        "userRefreshToken", newRefreshToken));
  }

  @GetMapping(ID + BOOKS)
  @Operation(summary = "Find user books with (optional) filter properties.", responses = {
      @ApiResponse(responseCode = "200", description = "List of Books.") })
  public ResponseEntity<PagedModel<BookListResponse>> findUserBooks(
      @Parameter(description = "User ID.") @PathVariable String id,
      @Valid @ParameterObject FindAllBooksFilter filter,
      @PageableDefault() Pageable pageable) {
    Page<Book> books = bookService.getUserBooksByFilter(id, filter, pageable);
    Page<BookListResponse> response = books.map(BookListResponse::new);
    return ResponseEntity.status(HttpStatus.OK).body(LinkBuilder.forPage(response, API_BASE + USERS + ID + BOOKS));
  }

  private void verifyUserIdentity(String targetUserId) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      throw new AccessDeniedException("notAccountOwner", targetUserId);
    }
    String authenticatedUserId = authentication.getName();
    if (!authenticatedUserId.equals(targetUserId)) {
      throw new AccessDeniedException("notAccountOwner", targetUserId);
    }
  }
}
