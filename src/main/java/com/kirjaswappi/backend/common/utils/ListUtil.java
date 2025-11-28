/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.utils;

import java.util.List;
import java.util.Objects;

public final class ListUtil {

  private ListUtil() {
    throw new IllegalStateException("Utility class  should not be instantiated");
  }

  public static <T> List<T> emptyIfNull(List<T> list) {
    return Objects.requireNonNullElseGet(list, List::of);
  }
}
