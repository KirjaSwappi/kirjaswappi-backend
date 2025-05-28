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
import com.kirjaswappi.backend.common.service.mappers.AdminUserMapper;
import com.kirjaswappi.backend.common.utils.JwtUtil;
import com.kirjaswappi.backend.service.exceptions.UserAlreadyExistsException;
import com.kirjaswappi.backend.service.exceptions.UserNotFoundException;

class AdminUserServiceTest {
  @Mock
  private AdminUserRepository adminUserRepository;
  @Mock
  private AdminUserMapper mapper;
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
    when(mapper.toEntity(adminUserDao)).thenReturn(adminUser);
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
    when(mapper.toDao(adminUser)).thenReturn(adminUserDao);
    when(adminUserRepository.save(adminUserDao)).thenReturn(adminUserDao);
    when(mapper.toEntity(adminUserDao)).thenReturn(adminUser);
    assertEquals(adminUser, adminUserService.addUser(adminUser));
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
    when(mapper.toEntity(adminUserDao)).thenReturn(adminUser);
    List<AdminUser> result = adminUserService.getAdminUsers();
    assertEquals(1, result.size());
    assertEquals(adminUser, result.get(0));
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
    when(mapper.toEntity(adminUserDao)).thenReturn(adminUser);
    AdminUser input = new AdminUser("admin", "pass", Role.ADMIN);
    assertEquals(adminUser, adminUserService.verifyUser(input));
  }

  @Test
  @DisplayName("Should throw InvalidCredentials when password does not match")
  void verifyUserThrowsOnPasswordMismatch() {
    AdminUser dbUser = new AdminUser("admin", "otherpass", Role.ADMIN);
    when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(adminUserDao));
    when(mapper.toEntity(adminUserDao)).thenReturn(dbUser);
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
