/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service.exceptions;

import com.kirjaswappi.backend.common.exceptions.SystemException;

public class ImageUrlFetchFailureException extends SystemException {
  private final Object[] params;

  public ImageUrlFetchFailureException(Object... params) {
    super("imageUrlFetchFailed");
    this.params = params;
  }

  @Override
  public String getCode() {
    return "imageUrlFetchFailed";
  }

  @Override
  public Object[] getParams() {
    return params;
  }
}
