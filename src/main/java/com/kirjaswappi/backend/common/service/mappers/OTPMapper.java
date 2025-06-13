/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service.mappers;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.kirjaswappi.backend.common.jpa.daos.OTPDao;
import com.kirjaswappi.backend.common.service.entities.OTP;

@Component
public class OTPMapper {
  public OTP toEntity(OTPDao dao) {
    var entity = new OTP();
    entity.setEmail(dao.getEmail());
    entity.setOtp(dao.getOtp());
    entity.setCreatedAt(dao.getCreatedAt());
    return entity;
  }

  public OTPDao toDao(OTP entity) {
    var dao = new OTPDao();
    dao.setEmail(entity.getEmail());
    dao.setOtp(entity.getOtp());
    dao.setCreatedAt(Instant.now());
    return dao;
  }
}
