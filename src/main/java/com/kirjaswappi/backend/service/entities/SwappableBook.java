/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service.entities;

import java.io.Serializable;

import lombok.*;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwappableBook implements Serializable {
  private String id;
  private String title;
  private String author;
  private String coverPhoto;
  @JsonIgnore
  private MultipartFile coverPhotoFile;
  private boolean isDeleted;
}
