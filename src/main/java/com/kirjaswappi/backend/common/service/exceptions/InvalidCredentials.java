/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service.exceptions;

import com.kirjaswappi.backend.common.exceptions.BusinessException;

public class InvalidCredentials extends BusinessException {
  public InvalidCredentials(Object... params) {
    super("invalidCredentials", params);
  }
}
