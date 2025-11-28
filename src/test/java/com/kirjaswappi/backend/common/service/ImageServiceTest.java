/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.kirjaswappi.backend.service.exceptions.ImageDeletionFailureException;
import com.kirjaswappi.backend.service.exceptions.ImageUploadFailureException;
import com.kirjaswappi.backend.service.exceptions.ImageUrlFetchFailureException;

@DisplayName("ImageService Tests")
class ImageServiceTest {
  @Mock
  private MinioClient minioClient;

  @InjectMocks
  private ImageService imageService;

  private static final String TEST_BUCKET = "test-bucket";
  private static final String TEST_UNIQUE_ID = "test-image-123";
  private static final String TEST_URL = "https://example.com/test-image-123";

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    ReflectionTestUtils.setField(imageService, "bucketName", TEST_BUCKET);
  }

  @Test
  @DisplayName("Should upload image successfully with valid file")
  void shouldUploadImageSuccessfullyWithValidFile() throws Exception {
    // Given
    byte[] content = "test image content".getBytes();
    MultipartFile file = new MockMultipartFile(
        "image",
        "test.jpg",
        "image/jpeg",
        content);

    // MinioClient.putObject returns ObjectWriteResponse, not void
    when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

    // When
    assertDoesNotThrow(() -> imageService.uploadImage(file, TEST_UNIQUE_ID));

    // Then
    verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
  }

  @Test
  @DisplayName("Should throw ImageUploadFailureException when MinioClient throws exception")
  void shouldThrowImageUploadFailureExceptionWhenMinioClientThrowsException() throws Exception {
    // Given
    byte[] content = "test image content".getBytes();
    MultipartFile file = new MockMultipartFile(
        "image",
        "test.jpg",
        "image/jpeg",
        content);

    doThrow(new RuntimeException("Minio error")).when(minioClient).putObject(any(PutObjectArgs.class));

    // When & Then
    ImageUploadFailureException exception = assertThrows(
        ImageUploadFailureException.class,
        () -> imageService.uploadImage(file, TEST_UNIQUE_ID));

    assertEquals("imageUploadFailed", exception.getCode());
    assertNotNull(exception.getParams());
    assertEquals(TEST_UNIQUE_ID, exception.getParams()[0]);
    verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
  }

  @Test
  @DisplayName("Should throw ImageUploadFailureException when file input stream fails")
  void shouldThrowImageUploadFailureExceptionWhenFileInputStreamFails() throws Exception {
    // Given
    MultipartFile file = mock(MultipartFile.class);
    when(file.getInputStream()).thenThrow(new RuntimeException("Stream error"));

    // When & Then
    ImageUploadFailureException exception = assertThrows(
        ImageUploadFailureException.class,
        () -> imageService.uploadImage(file, TEST_UNIQUE_ID));

    assertEquals("imageUploadFailed", exception.getCode());
    verify(minioClient, never()).putObject(any(PutObjectArgs.class));
  }

  @Test
  @DisplayName("Should get download URL successfully")
  void shouldGetDownloadUrlSuccessfully() throws Exception {
    // Given
    when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
        .thenReturn(TEST_URL);

    // When
    String result = imageService.getDownloadUrl(TEST_UNIQUE_ID);

    // Then
    assertNotNull(result);
    assertEquals(TEST_URL, result);
    verify(minioClient, times(1)).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
  }

  @Test
  @DisplayName("Should throw ImageUrlFetchFailureException when MinioClient fails to get URL")
  void shouldThrowImageUrlFetchFailureExceptionWhenMinioClientFailsToGetUrl() throws Exception {
    // Given
    when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
        .thenThrow(new RuntimeException("Minio error"));

    // When & Then
    ImageUrlFetchFailureException exception = assertThrows(
        ImageUrlFetchFailureException.class,
        () -> imageService.getDownloadUrl(TEST_UNIQUE_ID));

    assertEquals("imageUrlFetchFailed", exception.getCode());
    assertNotNull(exception.getParams());
    assertEquals(TEST_UNIQUE_ID, exception.getParams()[0]);
    verify(minioClient, times(1)).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
  }

  @Test
  @DisplayName("Should delete image successfully")
  void shouldDeleteImageSuccessfully() throws Exception {
    // Given
    doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));

    // When
    assertDoesNotThrow(() -> imageService.deleteImage(TEST_UNIQUE_ID));

    // Then
    verify(minioClient, times(1)).removeObject(any(RemoveObjectArgs.class));
  }

  @Test
  @DisplayName("Should throw ImageDeletionFailureException when MinioClient fails to delete")
  void shouldThrowImageDeletionFailureExceptionWhenMinioClientFailsToDelete() throws Exception {
    // Given
    doThrow(new RuntimeException("Minio error")).when(minioClient).removeObject(any(RemoveObjectArgs.class));

    // When & Then
    ImageDeletionFailureException exception = assertThrows(
        ImageDeletionFailureException.class,
        () -> imageService.deleteImage(TEST_UNIQUE_ID));

    assertEquals("imageDeletionFailed", exception.getCode());
    assertNotNull(exception.getParams());
    assertEquals(TEST_UNIQUE_ID, exception.getParams()[0]);
    verify(minioClient, times(1)).removeObject(any(RemoveObjectArgs.class));
  }

  @Test
  @DisplayName("Should throw exception when unique ID is null in getDownloadUrl")
  void shouldThrowExceptionWhenUniqueIdIsNullInGetDownloadUrl() throws Exception {
    // Given - MinioClient will throw exception for null object name
    when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
        .thenThrow(new IllegalArgumentException("object name must not be null."));

    // When & Then
    ImageUrlFetchFailureException exception = assertThrows(
        ImageUrlFetchFailureException.class,
        () -> imageService.getDownloadUrl(null));

    assertEquals("imageUrlFetchFailed", exception.getCode());
  }

  @Test
  @DisplayName("Should throw exception when unique ID is null in deleteImage")
  void shouldThrowExceptionWhenUniqueIdIsNullInDeleteImage() throws Exception {
    // Given - MinioClient will throw exception for null object name
    doThrow(new IllegalArgumentException("object name must not be null."))
        .when(minioClient).removeObject(any(RemoveObjectArgs.class));

    // When & Then
    ImageDeletionFailureException exception = assertThrows(
        ImageDeletionFailureException.class,
        () -> imageService.deleteImage(null));

    assertEquals("imageDeletionFailed", exception.getCode());
  }

  @Test
  @DisplayName("Should upload image with different content types")
  void shouldUploadImageWithDifferentContentTypes() throws Exception {
    // Given
    byte[] content = "test image content".getBytes();
    MultipartFile pngFile = new MockMultipartFile(
        "image",
        "test.png",
        "image/png",
        content);

    when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

    // When
    assertDoesNotThrow(() -> imageService.uploadImage(pngFile, TEST_UNIQUE_ID));

    // Then
    verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
  }

  @Test
  @DisplayName("Should upload empty file")
  void shouldUploadEmptyFile() throws Exception {
    // Given
    MultipartFile emptyFile = new MockMultipartFile(
        "image",
        "empty.jpg",
        "image/jpeg",
        new byte[0]);

    when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

    // When
    assertDoesNotThrow(() -> imageService.uploadImage(emptyFile, TEST_UNIQUE_ID));

    // Then
    verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
  }

  @Test
  @DisplayName("Should handle large file upload")
  void shouldHandleLargeFileUpload() throws Exception {
    // Given
    byte[] largeContent = new byte[10 * 1024 * 1024]; // 10MB
    MultipartFile largeFile = new MockMultipartFile(
        "image",
        "large.jpg",
        "image/jpeg",
        largeContent);

    when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

    // When
    assertDoesNotThrow(() -> imageService.uploadImage(largeFile, TEST_UNIQUE_ID));

    // Then
    verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
  }
}
