/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.http.controllers;

import static com.kirjaswappi.backend.common.utils.Constants.*;

import java.io.IOException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "OTP Management", description = "APIs for OTP (One-Time Password) generation and verification")
public class OTPController {
  @Autowired
  private OTPService otpService;

  @PostMapping(SEND_OTP)
  @Operation(summary = "Send OTP to user email", description = "Generates and sends a one-time password (OTP) to the specified email address.", responses = {
      @ApiResponse(responseCode = "200", description = "OTP sent successfully", content = @Content(schema = @Schema(implementation = SendOtpResponse.class))),
      @ApiResponse(responseCode = "400", description = "Invalid email format"),
      @ApiResponse(responseCode = "500", description = "Failed to send OTP email or internal server error")
  })
  public ResponseEntity<SendOtpResponse> sendOTP(@RequestBody SendOtpRequest request) throws IOException {
    String userEmail = otpService.saveAndSendOTP(request.toEntity());
    return ResponseEntity.status(HttpStatus.OK).body(new SendOtpResponse(userEmail));
  }

  @PostMapping(VERIFY_OTP)
  @Operation(summary = "Verify OTP", description = "Verifies the one-time password (OTP) sent to the user's email address.", responses = {
      @ApiResponse(responseCode = "200", description = "OTP verified successfully", content = @Content(schema = @Schema(implementation = VerifyOtpResponse.class))),
      @ApiResponse(responseCode = "400", description = "Invalid or expired OTP"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public ResponseEntity<VerifyOtpResponse> verifyOTP(@RequestBody VerifyOtpRequest request) {
    String email = otpService.verifyOTPByEmail(request.toEntity());
    return ResponseEntity.status(HttpStatus.OK).body(new VerifyOtpResponse(email));
  }
}
