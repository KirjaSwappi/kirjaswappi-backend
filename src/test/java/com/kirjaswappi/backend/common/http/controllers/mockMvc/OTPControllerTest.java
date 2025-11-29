/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.http.controllers.mockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
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

@WebMvcTest(OTPController.class)
@Import(CustomMockMvcConfiguration.class)
class OTPControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private OTPService otpService;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @DisplayName("Should send OTP successfully")
  void shouldSendOtpSuccessfully() throws Exception {
    SendOtpRequest request = new SendOtpRequest();
    request.setEmail("test@example.com");

    when(otpService.saveAndSendOTP(any())).thenReturn("test@example.com");

    mockMvc.perform(post("/api/v1/send-otp")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("Should verify OTP successfully")
  void shouldVerifyOtpSuccessfully() throws Exception {
    VerifyOtpRequest request = new VerifyOtpRequest();
    request.setEmail("test@example.com");
    request.setOtp("123456");

    when(otpService.verifyOTPByEmail(any())).thenReturn("test@example.com");

    mockMvc.perform(post("/api/v1/verify-otp")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }
}
