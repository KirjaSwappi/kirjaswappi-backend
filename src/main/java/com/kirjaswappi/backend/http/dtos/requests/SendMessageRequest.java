/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessageRequest {
  @NotBlank(message = "Message cannot be blank")
  @Size(max = 1000, message = "Message cannot exceed 1000 characters")
  @Schema(description = "The message content to send", example = "Hello, is this book still available?")
  private String message;
}
