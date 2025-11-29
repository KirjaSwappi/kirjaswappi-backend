/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.kirjaswappi.backend.common.jpa.daos.AdminUserDao;
import com.kirjaswappi.backend.common.jpa.repositories.AdminUserRepository;
import com.kirjaswappi.backend.common.service.entities.AdminUser;
import com.kirjaswappi.backend.common.service.enums.Role;
import com.kirjaswappi.backend.common.service.exceptions.InvalidCredentials;
import com.kirjaswappi.backend.common.utils.JwtUtil;
import com.kirjaswappi.backend.service.exceptions.UserAlreadyExistsException;
import com.kirjaswappi.backend.service.exceptions.UserNotFoundException;

class AdminUserServiceTest {
  @Mock
  private AdminUserRepository adminUserRepository;
  @Mock
  private JwtUtil jwtTokenUtil;
  @InjectMocks
  private AdminUserService adminUserService;

  private AdminUser adminUser;
  private AdminUserDao adminUserDao;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    adminUser = new AdminUser("admin", "pass", Role.ADMIN);
    adminUserDao = new AdminUserDao(null, "admin", "pass", "Admin");
  }

  @Test
  @DisplayName("Should return admin user info when found")
  void getAdminUserInfoSuccess() {
    when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(adminUserDao));
    assertEquals(adminUser, adminUserService.getAdminUserInfo("admin"));
  }

  @Test
  @DisplayName("Should throw UserNotFoundException when admin user not found")
  void getAdminUserInfoThrowsWhenNotFound() {
    when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.empty());
    assertThrows(UserNotFoundException.class, () -> adminUserService.getAdminUserInfo("admin"));
  }

  @Test
  @DisplayName("Should add user successfully when username does not exist")
  void addUserSuccess() {
    when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.empty());
    when(adminUserRepository.save(any(AdminUserDao.class))).thenReturn(adminUserDao);
    AdminUser result = adminUserService.addUser(adminUser);
    assertEquals(adminUser.username(), result.username());
    assertEquals(adminUser.role(), result.role());
  }

  @Test
  @DisplayName("Should throw UserAlreadyExistsException when username exists")
  void addUserThrowsWhenExists() {
    when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(adminUserDao));
    assertThrows(UserAlreadyExistsException.class, () -> adminUserService.addUser(adminUser));
  }

  @Test
  @DisplayName("Should return list of admin users")
  void getAdminUsersReturnsList() {
    when(adminUserRepository.findAll()).thenReturn(List.of(adminUserDao));
    List<AdminUser> result = adminUserService.getAdminUsers();
    assertEquals(1, result.size());
    assertEquals(adminUser.username(), result.get(0).username());
  }

  @Test
  @DisplayName("Should delete user successfully when found")
  void deleteUserSuccess() {
    when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(adminUserDao));
    doNothing().when(adminUserRepository).deleteByUsername("admin");
    assertDoesNotThrow(() -> adminUserService.deleteUser("admin"));
    verify(adminUserRepository).deleteByUsername("admin");
  }

  @Test
  @DisplayName("Should throw UserNotFoundException when deleting non-existent user")
  void deleteUserThrowsWhenNotFound() {
    when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.empty());
    assertThrows(UserNotFoundException.class, () -> adminUserService.deleteUser("admin"));
  }

  @Test
  @DisplayName("Should verify user successfully when password matches")
  void verifyUserSuccess() {
    when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(adminUserDao));
    AdminUser input = new AdminUser("admin", "pass", Role.ADMIN);
    AdminUser result = adminUserService.verifyUser(input);
    assertEquals(adminUser.username(), result.username());
    assertEquals(adminUser.password(), result.password());
  }

  @Test
  @DisplayName("Should throw InvalidCredentials when password does not match")
  void verifyUserThrowsOnPasswordMismatch() {
    AdminUserDao dbUserDao = new AdminUserDao(null, "admin", "otherpass", "Admin");
    when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(dbUserDao));
    AdminUser input = new AdminUser("admin", "pass", Role.ADMIN);
    assertThrows(InvalidCredentials.class, () -> adminUserService.verifyUser(input));
  }

  @Test
  @DisplayName("Should throw UserNotFoundException when verifying non-existent user")
  void verifyUserThrowsWhenNotFound() {
    when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.empty());
    AdminUser input = new AdminUser("admin", "pass", Role.ADMIN);
    assertThrows(UserNotFoundException.class, () -> adminUserService.verifyUser(input));
  }
}
