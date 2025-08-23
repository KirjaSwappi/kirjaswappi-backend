/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kirjaswappi.backend.common.jpa.daos.AdminUserDao;
import com.kirjaswappi.backend.common.jpa.repositories.AdminUserRepository;
import com.kirjaswappi.backend.common.service.entities.AdminUser;
import com.kirjaswappi.backend.common.service.exceptions.InvalidCredentials;
import com.kirjaswappi.backend.common.service.mappers.AdminUserMapper;
import com.kirjaswappi.backend.service.exceptions.UserAlreadyExistsException;
import com.kirjaswappi.backend.service.exceptions.UserNotFoundException;

@Service
@Transactional
public class AdminUserService {
  @Autowired
  private AdminUserRepository adminUserRepository;
  @Autowired
  private AdminUserMapper mapper;

  public AdminUser getAdminUserInfo(String username) {
    return mapper.toEntity(adminUserRepository.findByUsername(username)
        .orElseThrow(() -> new UserNotFoundException(username)));
  }

  public AdminUser addUser(AdminUser adminUser) {
    // validate username exists:
    if (adminUserRepository.findByUsername(adminUser.getUsername()).isPresent()) {
      throw new UserAlreadyExistsException(adminUser.getUsername());
    }
    AdminUserDao dao = mapper.toDao(adminUser);
    return mapper.toEntity(adminUserRepository.save(dao));
  }

  public List<AdminUser> getAdminUsers() {
    return adminUserRepository.findAll().stream().map(mapper::toEntity).toList();
  }

  public void deleteUser(String username) {
    adminUserRepository.findByUsername(username)
        .orElseThrow(() -> new UserNotFoundException(username));
    adminUserRepository.deleteByUsername(username);
  }

  public AdminUser verifyUser(AdminUser user) {
    AdminUser adminUserFromDB = getAdminUserInfo(user.getUsername());
    if (adminUserFromDB.getPassword().equals(user.getPassword())) {
      return adminUserFromDB;
    }
    throw new InvalidCredentials(user.getPassword());
  }
}
