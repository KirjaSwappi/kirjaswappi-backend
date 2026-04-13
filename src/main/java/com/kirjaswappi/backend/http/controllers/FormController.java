/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.controllers;

import static com.kirjaswappi.backend.common.utils.Constants.*;

import java.util.Set;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.kirjaswappi.backend.common.service.EmailService;
import com.kirjaswappi.backend.http.dtos.requests.FormSubmissionRequest;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;

@RestController
@RequestMapping(API_BASE + FORMS)
@Validated
@Tag(name = "Forms", description = "API for public form submissions (contact, collaboration, donation, feedback, volunteer)")
public class FormController {
  private static final Set<String> VALID_FORM_TYPES = Set.of(
      "contact", "collaboration", "donation", "feedback", "volunteer");

  @Autowired
  private EmailService emailService;

  @PostMapping("/{type}")
  @Operation(summary = "Submit a form", description = "Submit a public form of the given type. Sends the form data to the admin email.", responses = {
      @ApiResponse(responseCode = "200", description = "Form submitted successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid form type or missing required fields")
  })
  public ResponseEntity<Void> submitForm(
      @Parameter(description = "Form type: contact, collaboration, donation, feedback, or volunteer") @PathVariable String type,
      @RequestBody FormSubmissionRequest request) {
    if (!VALID_FORM_TYPES.contains(type.toLowerCase())) {
      throw new BadRequestException("invalidFormType", type);
    }
    request.validate();
    emailService.sendFormSubmission(type.toLowerCase(), request.getName(), request.getEmail(),
        request.getSubject(), request.getMessage(), request.getAmount());
    return ResponseEntity.ok().build();
  }
}
