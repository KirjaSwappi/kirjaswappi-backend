/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.requests;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import com.kirjaswappi.backend.http.validations.ValidationUtil;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;

@Getter
@Setter
public class FormSubmissionRequest {
  @Schema(description = "The name of the person submitting the form.", example = "John Doe", requiredMode = REQUIRED)
  private String name;

  @Schema(description = "The email address of the person submitting the form.", example = "john@example.com", requiredMode = REQUIRED)
  private String email;

  @Schema(description = "The subject of the form submission.", example = "Book donation inquiry", requiredMode = NOT_REQUIRED)
  private String subject;

  @Schema(description = "The message content of the form submission.", example = "I would like to donate some books.", requiredMode = REQUIRED)
  private String message;

  @Schema(description = "The amount for donation or collaboration forms.", example = "50", requiredMode = NOT_REQUIRED)
  private String amount;

  public void validate() {
    if (!ValidationUtil.validateNotBlank(this.name)) {
      throw new BadRequestException("nameCannotBeBlank", this.name);
    }
    if (!ValidationUtil.validateEmail(this.email)) {
      throw new BadRequestException("invalidEmailAddress", this.email);
    }
    if (!ValidationUtil.validateNotBlank(this.message)) {
      throw new BadRequestException("messageCannotBeBlank", this.message);
    }
  }
}
