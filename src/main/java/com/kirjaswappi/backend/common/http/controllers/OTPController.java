/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.http.controllers;

import static com.kirjaswappi.backend.common.utils.Constants.*;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kirjaswappi.backend.common.http.dtos.requests.SendOtpRequest;
import com.kirjaswappi.backend.common.http.dtos.requests.VerifyOtpRequest;
import com.kirjaswappi.backend.common.http.dtos.responses.SendOtpResponse;
import com.kirjaswappi.backend.common.http.dtos.responses.VerifyOtpResponse;
import com.kirjaswappi.backend.common.service.OTPService;

@RestController
@RequestMapping(API_BASE)
public class OTPController {
  @Autowired
  private OTPService otpService;

  @PostMapping(SEND_OTP)
  public ResponseEntity<SendOtpResponse> sendOTP(@RequestBody SendOtpRequest request) throws IOException {
    String userEmail = otpService.saveAndSendOTP(request.toEntity());
    return ResponseEntity.status(HttpStatus.OK).body(new SendOtpResponse(userEmail));
  }

  @PostMapping(VERIFY_OTP)
  public ResponseEntity<VerifyOtpResponse> verifyOTP(@RequestBody VerifyOtpRequest request) {
    String email = otpService.verifyOTPByEmail(request.toEntity());
    return ResponseEntity.status(HttpStatus.OK).body(new VerifyOtpResponse(email));
  }
}
