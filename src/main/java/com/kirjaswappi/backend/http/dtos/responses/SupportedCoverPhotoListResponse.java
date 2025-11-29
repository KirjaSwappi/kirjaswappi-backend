/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.responses;

import lombok.Getter;
import lombok.Setter;

import com.kirjaswappi.backend.service.entities.Photo;

@Getter
@Setter
public class SupportedCoverPhotoListResponse {
  private String id;
  private String coverPhotoUrl;

  public SupportedCoverPhotoListResponse(Photo entity) {
    this.id = entity.id();
    this.coverPhotoUrl = entity.coverPhoto();
  }
}
