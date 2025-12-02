/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirjaswappi.backend.common.http.controllers.OTPController;
import com.kirjaswappi.backend.common.http.controllers.mockMvc.config.CustomMockMvcConfiguration;
import com.kirjaswappi.backend.common.http.dtos.requests.SendOtpRequest;
import com.kirjaswappi.backend.common.http.dtos.requests.VerifyOtpRequest;
import com.kirjaswappi.backend.common.service.OTPService;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;

/**
 * Comprehensive tests for OTP API endpoints. Tests sending and verifying OTP
 * for email verification and password reset flows.
 */
@WebMvcTest(OTPController.class)
@Import(CustomMockMvcConfiguration.class)
class OtpApiIntegrationTest {

  private static final String API_BASE = "/api/v1";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private OTPService otpService;

  @Autowired
  private ObjectMapper objectMapper;

  @Nested
  @DisplayName("Send OTP Tests")
  class SendOtpTests {

    @Test
    @DisplayName("Should send OTP successfully")
    void shouldSendOtpSuccessfully() throws Exception {
      SendOtpRequest request = new SendOtpRequest();
      request.setEmail("test@example.com");

      when(otpService.saveAndSendOTP(any())).thenReturn("test@example.com");

      mockMvc.perform(post(API_BASE + "/send-otp")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("OTP sent to email: test@example.com"));
    }

    @Test
    @DisplayName("Should return 400 when email is missing")
    void shouldReturn400WhenEmailMissing() throws Exception {
      SendOtpRequest request = new SendOtpRequest();
      // Null email now properly returns 400 after bug fix

      mockMvc.perform(post(API_BASE + "/send-otp")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when email format is invalid")
    void shouldReturn400WhenEmailFormatInvalid() throws Exception {
      SendOtpRequest request = new SendOtpRequest();
      request.setEmail("invalid-email-format");

      mockMvc.perform(post(API_BASE + "/send-otp")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should send OTP to different valid email formats")
    void shouldSendOtpToValidEmailFormats() throws Exception {
      String[] validEmails = {
          "user@domain.com",
          "user.name@domain.com",
          "user+tag@domain.com",
          "user@subdomain.domain.com"
      };

      for (String email : validEmails) {
        SendOtpRequest request = new SendOtpRequest();
        request.setEmail(email);

        when(otpService.saveAndSendOTP(any())).thenReturn(email);

        mockMvc.perform(post(API_BASE + "/send-otp")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("OTP sent to email: " + email));
      }
    }
  }

  @Nested
  @DisplayName("Verify OTP Tests")
  class VerifyOtpTests {

    @Test
    @DisplayName("Should verify OTP successfully")
    void shouldVerifyOtpSuccessfully() throws Exception {
      VerifyOtpRequest request = new VerifyOtpRequest();
      request.setEmail("test@example.com");
      request.setOtp("123456");

      when(otpService.verifyOTPByEmail(any())).thenReturn("test@example.com");

      mockMvc.perform(post(API_BASE + "/verify-otp")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("OTP verified for test@example.com successfully."));
    }

    @Test
    @DisplayName("Should return 400 when email is missing")
    void shouldReturn400WhenEmailMissingForVerify() throws Exception {
      VerifyOtpRequest request = new VerifyOtpRequest();
      request.setOtp("123456");
      // Null email now properly returns 400 after bug fix

      mockMvc.perform(post(API_BASE + "/verify-otp")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when OTP is missing")
    void shouldReturn400WhenOtpMissing() throws Exception {
      VerifyOtpRequest request = new VerifyOtpRequest();
      request.setEmail("test@example.com");

      mockMvc.perform(post(API_BASE + "/verify-otp")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when OTP is invalid")
    void shouldReturn400WhenOtpInvalid() throws Exception {
      VerifyOtpRequest request = new VerifyOtpRequest();
      request.setEmail("test@example.com");
      request.setOtp("000000");

      when(otpService.verifyOTPByEmail(any()))
          .thenThrow(new BadRequestException("otpDoesNotMatch", "000000"));

      mockMvc.perform(post(API_BASE + "/verify-otp")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when OTP is expired")
    void shouldReturn400WhenOtpExpired() throws Exception {
      VerifyOtpRequest request = new VerifyOtpRequest();
      request.setEmail("test@example.com");
      request.setOtp("123456");

      when(otpService.verifyOTPByEmail(any()))
          .thenThrow(new BadRequestException("otpExpired", "test@example.com"));

      mockMvc.perform(post(API_BASE + "/verify-otp")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when OTP not found for email")
    void shouldReturn400WhenOtpNotFound() throws Exception {
      VerifyOtpRequest request = new VerifyOtpRequest();
      request.setEmail("unknown@example.com");
      request.setOtp("123456");

      when(otpService.verifyOTPByEmail(any()))
          .thenThrow(new BadRequestException("otpNotFound", "unknown@example.com"));

      mockMvc.perform(post(API_BASE + "/verify-otp")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when all fields are missing")
    void shouldReturn400WhenAllFieldsMissing() throws Exception {
      VerifyOtpRequest request = new VerifyOtpRequest();
      // Null email now properly returns 400 after bug fix

      mockMvc.perform(post(API_BASE + "/verify-otp")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("Request Body Validation Tests")
  class RequestBodyValidationTests {

    @Test
    @DisplayName("Should return 400 for empty request body in send OTP")
    void shouldReturn400ForEmptyRequestBodyInSendOtp() throws Exception {
      // Empty JSON results in null email, now properly returns 400 after bug fix
      mockMvc.perform(post(API_BASE + "/send-otp")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 for empty request body in verify OTP")
    void shouldReturn400ForEmptyRequestBodyInVerifyOtp() throws Exception {
      // Empty JSON results in null fields, now properly returns 400 after bug fix
      mockMvc.perform(post(API_BASE + "/verify-otp")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 for malformed JSON in send OTP")
    void shouldReturn400ForMalformedJsonInSendOtp() throws Exception {
      mockMvc.perform(post(API_BASE + "/send-otp")
          .contentType(MediaType.APPLICATION_JSON)
          .content("invalid json"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 for malformed JSON in verify OTP")
    void shouldReturn400ForMalformedJsonInVerifyOtp() throws Exception {
      mockMvc.perform(post(API_BASE + "/verify-otp")
          .contentType(MediaType.APPLICATION_JSON)
          .content("invalid json"))
          .andExpect(status().isBadRequest());
    }
  }
}
