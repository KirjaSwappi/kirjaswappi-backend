/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.requests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SendMessageRequestTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  @DisplayName("Should pass validation with valid message")
  void shouldPassValidationWithValidMessage() {
    // Given
    SendMessageRequest request = new SendMessageRequest();
    request.setMessage("Hello, is this book still available?");

    // When
    Set<ConstraintViolation<SendMessageRequest>> violations = validator.validate(request);

    // Then
    assertTrue(violations.isEmpty());
    assertEquals("Hello, is this book still available?", request.getMessage());
  }

  @Test
  @DisplayName("Should fail validation with blank message")
  void shouldFailValidationWithBlankMessage() {
    // Given
    SendMessageRequest request = new SendMessageRequest();
    request.setMessage("");

    // When
    Set<ConstraintViolation<SendMessageRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty());
    assertEquals(1, violations.size());
    ConstraintViolation<SendMessageRequest> violation = violations.iterator().next();
    assertEquals("Message cannot be blank", violation.getMessage());
    assertEquals("message", violation.getPropertyPath().toString());
  }

  @Test
  @DisplayName("Should fail validation with null message")
  void shouldFailValidationWithNullMessage() {
    // Given
    SendMessageRequest request = new SendMessageRequest();
    request.setMessage(null);

    // When
    Set<ConstraintViolation<SendMessageRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty());
    assertEquals(1, violations.size());
    ConstraintViolation<SendMessageRequest> violation = violations.iterator().next();
    assertEquals("Message cannot be blank", violation.getMessage());
    assertEquals("message", violation.getPropertyPath().toString());
  }

  @Test
  @DisplayName("Should fail validation with whitespace-only message")
  void shouldFailValidationWithWhitespaceOnlyMessage() {
    // Given
    SendMessageRequest request = new SendMessageRequest();
    request.setMessage("   ");

    // When
    Set<ConstraintViolation<SendMessageRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty());
    assertEquals(1, violations.size());
    ConstraintViolation<SendMessageRequest> violation = violations.iterator().next();
    assertEquals("Message cannot be blank", violation.getMessage());
    assertEquals("message", violation.getPropertyPath().toString());
  }

  @Test
  @DisplayName("Should fail validation with message exceeding 1000 characters")
  void shouldFailValidationWithMessageExceeding1000Characters() {
    // Given
    SendMessageRequest request = new SendMessageRequest();
    String longMessage = "a".repeat(1001); // 1001 characters
    request.setMessage(longMessage);

    // When
    Set<ConstraintViolation<SendMessageRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty());
    assertEquals(1, violations.size());
    ConstraintViolation<SendMessageRequest> violation = violations.iterator().next();
    assertEquals("Message cannot exceed 1000 characters", violation.getMessage());
    assertEquals("message", violation.getPropertyPath().toString());
  }

  @Test
  @DisplayName("Should pass validation with message exactly 1000 characters")
  void shouldPassValidationWithMessageExactly1000Characters() {
    // Given
    SendMessageRequest request = new SendMessageRequest();
    String maxMessage = "a".repeat(1000); // Exactly 1000 characters
    request.setMessage(maxMessage);

    // When
    Set<ConstraintViolation<SendMessageRequest>> violations = validator.validate(request);

    // Then
    assertTrue(violations.isEmpty());
    assertEquals(maxMessage, request.getMessage());
  }
}