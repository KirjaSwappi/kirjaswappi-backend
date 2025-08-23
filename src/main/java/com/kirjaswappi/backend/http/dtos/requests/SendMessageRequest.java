/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.requests;

import java.util.List;

import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter
@Setter
public class SendMessageRequest {
  @Size(max = 1000, message = "Message cannot exceed 1000 characters")
  @Schema(description = "The message content to send", example = "Hello, is this book still available?")
  private String message;

  @Schema(description = "Images to attach to the message (optional)", nullable = true)
  private List<MultipartFile> images;

  // Custom validation method
  @JsonIgnore
  public boolean isValid() {
    return (message != null && !message.trim().isEmpty()) ||
        (images != null && !images.isEmpty());
  }
}
