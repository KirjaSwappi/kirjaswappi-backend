/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.kirjaswappi.backend.common.jpa.daos.OTPDao;
import com.kirjaswappi.backend.common.jpa.repositories.OTPRepository;
import com.kirjaswappi.backend.common.service.entities.OTP;
import com.kirjaswappi.backend.service.UserService;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;
import com.kirjaswappi.backend.service.exceptions.ResourceNotFoundException;
import com.kirjaswappi.backend.service.exceptions.UserNotFoundException;

class OTPServiceTest {
  @Mock
  private OTPRepository otpRepository;
  @Mock
  private UserService userService;
  @Mock
  private EmailService emailService;
  @InjectMocks
  private OTPService otpService;

  private final String email = "test@example.com";
  private final String otpValue = "123456";
  private OTP otp;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    otp = OTP.builder().email(email).otp(otpValue).createdAt(Instant.now()).build();
  }

  @Test
  @DisplayName("Should throw exception when OTP not found")
  void verifyOTPThrowsWhenNotFound() {
    when(otpRepository.findByEmail(email)).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> otpService.verifyOTPByEmail(otp));
  }

  @Test
  @DisplayName("Should throw exception when OTP does not match")
  void verifyOTPThrowsOnInvalid() {
    OTPDao dao = new OTPDao(email, "654321", Instant.now());
    when(otpRepository.findByEmail(email)).thenReturn(Optional.of(dao));
    assertThrows(BadRequestException.class, () -> otpService.verifyOTPByEmail(otp));
  }

  @Test
  @DisplayName("Should throw exception when OTP is expired")
  void verifyOTPThrowsOnExpired() {
    OTPDao dao = new OTPDao(email, otpValue, Instant.now().minus(Duration.ofMinutes(16)));
    when(otpRepository.findByEmail(email)).thenReturn(Optional.of(dao));
    assertThrows(BadRequestException.class, () -> otpService.verifyOTPByEmail(otp));
  }

  @Test
  @DisplayName("Should verify OTP successfully")
  void verifyOTPSuccess() {
    OTPDao dao = new OTPDao(email, otpValue, Instant.now());
    when(otpRepository.findByEmail(email)).thenReturn(Optional.of(dao));
    doNothing().when(otpRepository).deleteAllByEmail(email);
    assertEquals(email, otpService.verifyOTPByEmail(otp));
  }

  @Test
  @DisplayName("Should throw NPE when OTP is null (service does not handle null)")
  void verifyOTPThrowsNPEOnNull() {
    assertThrows(NullPointerException.class, () -> otpService.verifyOTPByEmail(null));
  }

  @Test
  @DisplayName("Should throw exception when email is null in OTP")
  void verifyOTPThrowsOnNullEmail() {
    OTP nullEmailOtp = OTP.builder().email(null).otp(otpValue).createdAt(Instant.now()).build();
    assertThrows(ResourceNotFoundException.class, () -> otpService.verifyOTPByEmail(nullEmailOtp));
  }

  @Test
  @DisplayName("Should throw exception when saveAndSendOTP called with null email")
  void saveAndSendOTPThrowsOnNullEmail() {
    assertThrows(UserNotFoundException.class, () -> otpService.saveAndSendOTP(null));
  }

  @Test
  @DisplayName("Should throw exception when OTPRepository throws on findByEmail")
  void verifyOTPThrowsWhenRepositoryThrows() {
    when(otpRepository.findByEmail(email)).thenThrow(new RuntimeException("db error"));
    assertThrows(RuntimeException.class, () -> otpService.verifyOTPByEmail(otp));
  }

  @Test
  @DisplayName("Should throw exception when OTPRepository throws on saveAndSendOTP")
  void saveAndSendOTPThrowsWhenRepositoryThrows() {
    when(userService.checkIfUserExists(email)).thenReturn(true);
    doThrow(new RuntimeException("db error")).when(otpRepository).deleteAllByEmail(email);
    assertThrows(RuntimeException.class, () -> otpService.saveAndSendOTP(email));
  }

  @Test
  @DisplayName("Should throw exception when EmailService throws on sendOTPByEmail")
  void saveAndSendOTPThrowsWhenEmailServiceFails() throws Exception {
    when(userService.checkIfUserExists(email)).thenReturn(true);
    doNothing().when(otpRepository).deleteAllByEmail(email);
    OTPDao dao = new OTPDao(email, otpValue, Instant.now());
    when(otpRepository.save(any(OTPDao.class))).thenReturn(dao);
    doThrow(new RuntimeException("email fail")).when(emailService).sendOTPByEmail(any(), any());
    assertThrows(RuntimeException.class, () -> otpService.saveAndSendOTP(email));
  }

  @Test
  @DisplayName("Should throw exception when user not found for OTP")
  void saveAndSendOTPThrowsWhenUserNotFound() {
    when(userService.checkIfUserExists(email)).thenReturn(false);
    assertThrows(UserNotFoundException.class, () -> otpService.saveAndSendOTP(email));
  }

  @Test
  @DisplayName("Should save and send OTP successfully")
  void saveAndSendOTPSuccess() throws Exception {
    when(userService.checkIfUserExists(email)).thenReturn(true);
    doNothing().when(otpRepository).deleteAllByEmail(email);
    OTPDao dao = new OTPDao(email, otpValue, Instant.now());
    when(otpRepository.save(any(OTPDao.class))).thenReturn(dao);
    doNothing().when(emailService).sendOTPByEmail(anyString(), anyString());
    assertEquals(email, otpService.saveAndSendOTP(email));
  }
}
