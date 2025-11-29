/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service.mappers;

import com.kirjaswappi.backend.common.jpa.daos.OTPDao;
import com.kirjaswappi.backend.common.service.entities.OTP;

public final class OTPMapper {

  private OTPMapper() {
    throw new IllegalStateException("Mapper class should not be instantiated");
  }

  public static OTP toEntity(OTPDao dao) {
    return OTP.builder()
        .email(dao.email())
        .otp(dao.otp())
        .createdAt(dao.createdAt())
        .build();
  }

  public static OTPDao toDao(OTP entity) {
    return OTPDao.builder()
        .email(entity.email())
        .otp(entity.otp())
        .createdAt(entity.createdAt())
        .build();
  }
}
