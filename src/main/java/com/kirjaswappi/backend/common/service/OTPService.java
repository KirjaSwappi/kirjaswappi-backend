/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service;

import static com.kirjaswappi.backend.common.utils.Constants.NUMERIC;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kirjaswappi.backend.common.jpa.repositories.OTPRepository;
import com.kirjaswappi.backend.common.service.entities.OTP;
import com.kirjaswappi.backend.common.service.mappers.OTPMapper;
import com.kirjaswappi.backend.service.UserService;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;
import com.kirjaswappi.backend.service.exceptions.ResourceNotFoundException;
import com.kirjaswappi.backend.service.exceptions.UserNotFoundException;

@Service
@Transactional
@RequiredArgsConstructor
public class OTPService {

  private final OTPRepository otpRepository;

  private final UserService userService;

  private final EmailService emailService;

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private static String generateOTP() {
    return SECURE_RANDOM.ints(6, 0, NUMERIC.length())
        .mapToObj(NUMERIC::charAt)
        .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
        .toString();
  }

  private OTP getOTP(String email) {
    return OTPMapper.toEntity(otpRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("otpNotFound", email)));
  }

  public String verifyOTPByEmail(OTP otp) {
    var otpEntity = this.getOTP(otp.email());

    // check provided OTP with the stored OTP:
    if (!otpEntity.otp().equals(otp.otp())) {
      throw new BadRequestException("otpDoesNotMatch", otp);
    }

    // Check if the OTP is older than 15 minutes
    if (otpEntity.createdAt().plus(Duration.ofMinutes(15)).isBefore(Instant.now())) {
      throw new BadRequestException("otpExpired", otp);
    }

    // Delete the OTP after verification:
    otpRepository.deleteAllByEmail(otp.email());
    return otpEntity.email();
  }

  private boolean checkUserExists(String email) {
    return userService.checkIfUserExists(email);
  }

  public String saveAndSendOTP(String email) throws IOException {
    // Check if the user exists:
    if (!checkUserExists(email)) {
      throw new UserNotFoundException(email);
    }

    // Delete all the previous OTPs:
    otpRepository.deleteAllByEmail(email);

    // Generate new OTP:
    var newOTP = OTP.builder().email(email).otp(generateOTP()).build();

    // Save the new OTP:
    var dao = OTPMapper.toDao(newOTP);
    otpRepository.save(dao);

    // Send OTP via email:
    emailService.sendOTPByEmail(dao.email(), newOTP.otp());
    return dao.email();
  }
}
