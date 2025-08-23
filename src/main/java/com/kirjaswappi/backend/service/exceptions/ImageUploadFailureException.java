/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service.exceptions;

import com.kirjaswappi.backend.common.exceptions.SystemException;

public class ImageUploadFailureException extends SystemException {
  private final Object[] params;

  public ImageUploadFailureException(Object... params) {
    super("imageUploadFailed");
    this.params = params;
  }

  @Override
  public String getCode() {
    return "imageUploadFailed";
  }

  @Override
  public Object[] getParams() {
    return params;
  }
}
