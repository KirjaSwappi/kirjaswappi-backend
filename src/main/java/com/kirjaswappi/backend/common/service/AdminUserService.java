/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service;

import java.util.List;

import lombok.RequiredArgsConstructor;

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
@RequiredArgsConstructor
public class AdminUserService {

  private final AdminUserRepository adminUserRepository;

  public AdminUser getAdminUserInfo(String username) {
    var adminUsers = adminUserRepository.findAll();
    var adminUser = adminUserRepository.findByUsername(username);
    return AdminUserMapper.toEntity(adminUserRepository.findByUsername(username)
        .orElseThrow(() -> new UserNotFoundException(username)));
  }

  public AdminUser addUser(AdminUser adminUser) {
    // validate username exists:
    if (adminUserRepository.findByUsername(adminUser.username()).isPresent()) {
      throw new UserAlreadyExistsException(adminUser.username());
    }
    AdminUserDao dao = AdminUserMapper.toDao(adminUser);
    return AdminUserMapper.toEntity(adminUserRepository.save(dao));
  }

  public List<AdminUser> getAdminUsers() {
    return adminUserRepository.findAll().stream().map(AdminUserMapper::toEntity).toList();
  }

  public void deleteUser(String username) {
    adminUserRepository.findByUsername(username)
        .orElseThrow(() -> new UserNotFoundException(username));
    adminUserRepository.deleteByUsername(username);
  }

  public AdminUser verifyUser(AdminUser user) {
    AdminUser adminUserFromDB = getAdminUserInfo(user.username());
    if (adminUserFromDB.password().equals(user.password())) {
      return adminUserFromDB;
    }
    throw new InvalidCredentials(user.password());
  }
}
