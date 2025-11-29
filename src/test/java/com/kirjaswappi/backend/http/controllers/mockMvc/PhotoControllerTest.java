/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.controllers.mockMvc;

import static com.kirjaswappi.backend.common.utils.Constants.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
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

@WebMvcTest(PhotoController.class)
@Import(CustomMockMvcConfiguration.class)
class PhotoControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private PhotoService photoService;

  private final String userId = "user123";
  private final String email = "test@example.com";
  private final String photoUrl = "http://example.com/photo.jpg";
  private final MockMultipartFile file = new MockMultipartFile("image", "photo.jpg", MediaType.IMAGE_JPEG_VALUE,
      "dummy".getBytes());
  private final MockMultipartFile supportedCoverPhoto = new MockMultipartFile("coverPhoto", "photo.jpg",
      MediaType.IMAGE_JPEG_VALUE, "dummy".getBytes());

  @Test
  @DisplayName("Should upload profile photo")
  void shouldUploadProfilePhoto() throws Exception {
    when(photoService.addProfilePhoto(userId, file)).thenReturn(photoUrl);

    mockMvc.perform(multipart(API_BASE + PHOTOS + PROFILE_PHOTO)
        .file(file)
        .param("userId", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.imageUrl").value(photoUrl));
  }

  @Test
  @DisplayName("Should upload cover photo")
  void shouldUploadCoverPhoto() throws Exception {
    when(photoService.addCoverPhoto(userId, file)).thenReturn(photoUrl);

    mockMvc.perform(multipart(API_BASE + PHOTOS + COVER_PHOTO)
        .file(file)
        .param("userId", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.imageUrl").value(photoUrl));
  }

  @Test
  @DisplayName("Should delete profile photo")
  void shouldDeleteProfilePhoto() throws Exception {
    doNothing().when(photoService).deleteProfilePhoto(userId);

    mockMvc.perform(delete(API_BASE + PHOTOS + PROFILE_PHOTO + "/" + userId))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("Should delete cover photo")
  void shouldDeleteCoverPhoto() throws Exception {
    doNothing().when(photoService).deleteCoverPhoto(userId);

    mockMvc.perform(delete(API_BASE + PHOTOS + COVER_PHOTO + "/" + userId))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("Should get profile photo by email")
  void shouldGetProfilePhotoByEmail() throws Exception {
    when(photoService.getPhotoByUserEmail(email, true)).thenReturn(photoUrl);

    mockMvc.perform(get(API_BASE + PHOTOS + PROFILE_PHOTO + BY_EMAIL + "/" + email))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.imageUrl").value(photoUrl));
  }

  @Test
  @DisplayName("Should get cover photo by email")
  void shouldGetCoverPhotoByEmail() throws Exception {
    when(photoService.getPhotoByUserEmail(email, false)).thenReturn(photoUrl);

    mockMvc.perform(get(API_BASE + PHOTOS + COVER_PHOTO + BY_EMAIL + "/" + email))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.imageUrl").value(photoUrl));
  }

  @Test
  @DisplayName("Should get profile photo by ID")
  void shouldGetProfilePhotoById() throws Exception {
    when(photoService.getPhotoByUserId(userId, true)).thenReturn(photoUrl);

    mockMvc.perform(get(API_BASE + PHOTOS + PROFILE_PHOTO + BY_ID + "/" + userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.imageUrl").value(photoUrl));
  }

  @Test
  @DisplayName("Should get cover photo by ID")
  void shouldGetCoverPhotoById() throws Exception {
    when(photoService.getPhotoByUserId(userId, false)).thenReturn(photoUrl);

    mockMvc.perform(get(API_BASE + PHOTOS + COVER_PHOTO + BY_ID + "/" + userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.imageUrl").value(photoUrl));
  }

  @Test
  @DisplayName("Should upload supported cover photo")
  void shouldUploadSupportedCoverPhoto() throws Exception {
    when(photoService.addSupportedCoverPhoto(supportedCoverPhoto)).thenReturn(photoUrl);

    mockMvc.perform(multipart(API_BASE + PHOTOS + SUPPORTED_COVER_PHOTOS)
        .file(supportedCoverPhoto))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.imageUrl").value(photoUrl));
  }

  @Test
  @DisplayName("Should get all supported cover photos")
  void shouldGetAllSupportedCoverPhotos() throws Exception {
    String photoId = "1";
    var listResponse = List.of(new Photo(photoId, photoUrl));
    when(photoService.findSupportedCoverPhoto()).thenReturn(listResponse);

    mockMvc.perform(get(API_BASE + PHOTOS + SUPPORTED_COVER_PHOTOS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].coverPhotoUrl").value(photoUrl));
  }

  @Test
  @DisplayName("Should delete supported cover photo")
  void shouldDeleteSupportedCoverPhoto() throws Exception {
    doNothing().when(photoService).deleteSupportedCoverPhoto(userId);

    mockMvc.perform(delete(API_BASE + PHOTOS + SUPPORTED_COVER_PHOTOS + "/" + userId))
        .andExpect(status().isNoContent());
  }
}
