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

import com.kirjaswappi.backend.service.exceptions.BadRequestException;

class UpdateSwapStatusRequestTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  @DisplayName("Should pass validation and custom validation with valid status")
  void shouldPassValidationWithValidStatus() {
    // Given
    UpdateSwapStatusRequest request = new UpdateSwapStatusRequest();
    request.setStatus("Accepted");

    // When
    Set<ConstraintViolation<UpdateSwapStatusRequest>> violations = validator.validate(request);

    // Then
    assertTrue(violations.isEmpty());
    assertEquals("Accepted", request.getStatus());

    // Should not throw exception on custom validation
    assertDoesNotThrow(() -> request.validate());
  }

  @Test
  @DisplayName("Should fail validation with blank status")
  void shouldFailValidationWithBlankStatus() {
    // Given
    UpdateSwapStatusRequest request = new UpdateSwapStatusRequest();
    request.setStatus("");

    // When
    Set<ConstraintViolation<UpdateSwapStatusRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty());
    assertEquals(1, violations.size());
    ConstraintViolation<UpdateSwapStatusRequest> violation = violations.iterator().next();
    assertEquals("Status cannot be blank", violation.getMessage());
    assertEquals("status", violation.getPropertyPath().toString());
  }

  @Test
  @DisplayName("Should fail validation with null status")
  void shouldFailValidationWithNullStatus() {
    // Given
    UpdateSwapStatusRequest request = new UpdateSwapStatusRequest();
    request.setStatus(null);

    // When
    Set<ConstraintViolation<UpdateSwapStatusRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty());
    assertEquals(1, violations.size());
    ConstraintViolation<UpdateSwapStatusRequest> violation = violations.iterator().next();
    assertEquals("Status cannot be blank", violation.getMessage());
    assertEquals("status", violation.getPropertyPath().toString());
  }

  @Test
  @DisplayName("Should fail custom validation with invalid status")
  void shouldFailCustomValidationWithInvalidStatus() {
    // Given
    UpdateSwapStatusRequest request = new UpdateSwapStatusRequest();
    request.setStatus("InvalidStatus");

    // When & Then
    BadRequestException exception = assertThrows(BadRequestException.class, () -> request.validate());
    // The exception message will be from SwapStatus.fromCode()
    assertNotNull(exception);
  }

  @Test
  @DisplayName("Should fail custom validation with blank status")
  void shouldFailCustomValidationWithBlankStatus() {
    // Given
    UpdateSwapStatusRequest request = new UpdateSwapStatusRequest();
    request.setStatus("");

    // When & Then
    BadRequestException exception = assertThrows(BadRequestException.class, () -> request.validate());
    assertNotNull(exception);
  }

  @Test
  @DisplayName("Should fail custom validation with null status")
  void shouldFailCustomValidationWithNullStatus() {
    // Given
    UpdateSwapStatusRequest request = new UpdateSwapStatusRequest();
    request.setStatus(null);

    // When & Then
    BadRequestException exception = assertThrows(BadRequestException.class, () -> request.validate());
    assertNotNull(exception);
  }

  @Test
  @DisplayName("Should pass validation with all valid SwapStatus values")
  void shouldPassValidationWithAllValidSwapStatusValues() {
    String[] validStatuses = { "Pending", "Accepted", "Reserved", "Rejected", "Expired" };

    for (String status : validStatuses) {
      // Given
      UpdateSwapStatusRequest request = new UpdateSwapStatusRequest();
      request.setStatus(status);

      // When
      Set<ConstraintViolation<UpdateSwapStatusRequest>> violations = validator.validate(request);

      // Then
      assertTrue(violations.isEmpty(), "Status " + status + " should be valid");
      assertEquals(status, request.getStatus());

      // Should not throw exception on custom validation
      assertDoesNotThrow(() -> request.validate(), "Status " + status + " should pass custom validation");
    }
  }
}
