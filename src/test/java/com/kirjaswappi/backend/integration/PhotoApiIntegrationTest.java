/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kirjaswappi.backend.common.http.controllers.mockMvc.config.CustomMockMvcConfiguration;
import com.kirjaswappi.backend.http.controllers.PhotoController;
import com.kirjaswappi.backend.service.PhotoService;
import com.kirjaswappi.backend.service.entities.Photo;
import com.kirjaswappi.backend.service.exceptions.UserNotFoundException;

/**
 * Comprehensive tests for Photo API endpoints. Tests profile photo, cover
 * photo, and supported cover photo management.
 */
@WebMvcTest(PhotoController.class)
@Import(CustomMockMvcConfiguration.class)
class PhotoApiIntegrationTest {

  private static final String API_BASE = "/api/v1/photos";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private PhotoService photoService;

  private final String userId = "user-123";
  private final String email = "test@example.com";
  private final String photoUrl = "https://storage.example.com/photos/test.jpg";
  private final MockMultipartFile imageFile = new MockMultipartFile(
      "image",
      "photo.jpg",
      MediaType.IMAGE_JPEG_VALUE,
      "test image content".getBytes());
  private final MockMultipartFile supportedCoverPhotoFile = new MockMultipartFile(
      "coverPhoto",
      "cover.jpg",
      MediaType.IMAGE_JPEG_VALUE,
      "cover image content".getBytes());

  @Nested
  @DisplayName("Profile Photo Tests")
  class ProfilePhotoTests {

    @Test
    @DisplayName("Should upload profile photo successfully")
    void shouldUploadProfilePhoto() throws Exception {
      when(photoService.addProfilePhoto(anyString(), any())).thenReturn(photoUrl);

      mockMvc.perform(multipart(API_BASE + "/profile")
          .file(imageFile)
          .param("userId", userId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.imageUrl").value(photoUrl));
    }

    @Test
    @DisplayName("Should return 400 when userId is missing for profile photo")
    void shouldReturn400WhenUserIdMissing() throws Exception {
      mockMvc.perform(multipart(API_BASE + "/profile")
          .file(imageFile))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when image file is missing")
    void shouldReturn400WhenImageFileMissing() throws Exception {
      mockMvc.perform(multipart(API_BASE + "/profile")
          .param("userId", userId))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should delete profile photo successfully")
    void shouldDeleteProfilePhoto() throws Exception {
      doNothing().when(photoService).deleteProfilePhoto(userId);

      mockMvc.perform(delete(API_BASE + "/profile/" + userId))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should get profile photo by email")
    void shouldGetProfilePhotoByEmail() throws Exception {
      when(photoService.getPhotoByUserEmail(email, true)).thenReturn(photoUrl);

      mockMvc.perform(get(API_BASE + "/profile/by-email/" + email))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.imageUrl").value(photoUrl));
    }

    @Test
    @DisplayName("Should get profile photo by user ID")
    void shouldGetProfilePhotoByUserId() throws Exception {
      when(photoService.getPhotoByUserId(userId, true)).thenReturn(photoUrl);

      mockMvc.perform(get(API_BASE + "/profile/by-id/" + userId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.imageUrl").value(photoUrl));
    }

    @Test
    @DisplayName("Should return 404 when user not found for profile photo")
    void shouldReturn404WhenUserNotFoundForProfilePhoto() throws Exception {
      when(photoService.getPhotoByUserId("nonexistent", true))
          .thenThrow(new UserNotFoundException());

      mockMvc.perform(get(API_BASE + "/profile/by-id/nonexistent"))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("Cover Photo Tests")
  class CoverPhotoTests {

    @Test
    @DisplayName("Should upload cover photo successfully")
    void shouldUploadCoverPhoto() throws Exception {
      when(photoService.addCoverPhoto(anyString(), any())).thenReturn(photoUrl);

      mockMvc.perform(multipart(API_BASE + "/cover")
          .file(imageFile)
          .param("userId", userId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.imageUrl").value(photoUrl));
    }

    @Test
    @DisplayName("Should return 400 when userId is missing for cover photo")
    void shouldReturn400WhenUserIdMissingForCover() throws Exception {
      mockMvc.perform(multipart(API_BASE + "/cover")
          .file(imageFile))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should delete cover photo successfully")
    void shouldDeleteCoverPhoto() throws Exception {
      doNothing().when(photoService).deleteCoverPhoto(userId);

      mockMvc.perform(delete(API_BASE + "/cover/" + userId))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should get cover photo by email")
    void shouldGetCoverPhotoByEmail() throws Exception {
      when(photoService.getPhotoByUserEmail(email, false)).thenReturn(photoUrl);

      mockMvc.perform(get(API_BASE + "/cover/by-email/" + email))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.imageUrl").value(photoUrl));
    }

    @Test
    @DisplayName("Should get cover photo by user ID")
    void shouldGetCoverPhotoByUserId() throws Exception {
      when(photoService.getPhotoByUserId(userId, false)).thenReturn(photoUrl);

      mockMvc.perform(get(API_BASE + "/cover/by-id/" + userId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.imageUrl").value(photoUrl));
    }

    @Test
    @DisplayName("Should return 404 when user not found for cover photo")
    void shouldReturn404WhenUserNotFoundForCoverPhoto() throws Exception {
      when(photoService.getPhotoByUserId("nonexistent", false))
          .thenThrow(new UserNotFoundException());

      mockMvc.perform(get(API_BASE + "/cover/by-id/nonexistent"))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("Supported Cover Photo Tests")
  class SupportedCoverPhotoTests {

    @Test
    @DisplayName("Should upload supported cover photo successfully")
    void shouldUploadSupportedCoverPhoto() throws Exception {
      when(photoService.addSupportedCoverPhoto(any())).thenReturn(photoUrl);

      mockMvc.perform(multipart(API_BASE + "/supported-cover-photos")
          .file(supportedCoverPhotoFile))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.imageUrl").value(photoUrl));
    }

    @Test
    @DisplayName("Should return 400 when cover photo file is missing")
    void shouldReturn400WhenCoverPhotoFileMissing() throws Exception {
      mockMvc.perform(multipart(API_BASE + "/supported-cover-photos"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get all supported cover photos")
    void shouldGetAllSupportedCoverPhotos() throws Exception {
      List<Photo> photos = List.of(
          new Photo("1", "https://example.com/photo1.jpg"),
          new Photo("2", "https://example.com/photo2.jpg"),
          new Photo("3", "https://example.com/photo3.jpg"));

      when(photoService.findSupportedCoverPhoto()).thenReturn(photos);

      mockMvc.perform(get(API_BASE + "/supported-cover-photos"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(3))
          .andExpect(jsonPath("$[0].coverPhotoUrl").value("https://example.com/photo1.jpg"))
          .andExpect(jsonPath("$[1].coverPhotoUrl").value("https://example.com/photo2.jpg"));
    }

    @Test
    @DisplayName("Should return empty list when no supported cover photos exist")
    void shouldReturnEmptyListWhenNoSupportedCoverPhotos() throws Exception {
      when(photoService.findSupportedCoverPhoto()).thenReturn(List.of());

      mockMvc.perform(get(API_BASE + "/supported-cover-photos"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Should delete supported cover photo successfully")
    void shouldDeleteSupportedCoverPhoto() throws Exception {
      doNothing().when(photoService).deleteSupportedCoverPhoto("photo-id");

      mockMvc.perform(delete(API_BASE + "/supported-cover-photos/photo-id"))
          .andExpect(status().isNoContent());
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should handle service exception for profile photo upload")
    void shouldHandleServiceExceptionForProfilePhotoUpload() throws Exception {
      when(photoService.addProfilePhoto(anyString(), any()))
          .thenThrow(new UserNotFoundException());

      mockMvc.perform(multipart(API_BASE + "/profile")
          .file(imageFile)
          .param("userId", "nonexistent"))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should handle service exception for cover photo upload")
    void shouldHandleServiceExceptionForCoverPhotoUpload() throws Exception {
      when(photoService.addCoverPhoto(anyString(), any()))
          .thenThrow(new UserNotFoundException());

      mockMvc.perform(multipart(API_BASE + "/cover")
          .file(imageFile)
          .param("userId", "nonexistent"))
          .andExpect(status().isNotFound());
    }
  }
}
