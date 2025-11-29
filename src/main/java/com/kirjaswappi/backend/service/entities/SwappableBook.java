/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service.entities;

import lombok.*;

import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwappableBook {
  private String id;
  private String title;
  private String author;
  private String coverPhoto;
  private MultipartFile coverPhotoFile;
  private boolean isDeleted;
}
