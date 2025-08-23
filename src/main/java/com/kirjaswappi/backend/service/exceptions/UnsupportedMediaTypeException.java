/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service.exceptions;

import com.kirjaswappi.backend.common.exceptions.BusinessException;

public class UnsupportedMediaTypeException extends BusinessException {
  public UnsupportedMediaTypeException(Object... params) {
    super("unsupportedMediaType", params);
  }
}
