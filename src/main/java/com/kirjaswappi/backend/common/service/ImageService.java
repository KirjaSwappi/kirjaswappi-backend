/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.kirjaswappi.backend.service.exceptions.*;

@Service
@Transactional
@RequiredArgsConstructor
public class ImageService {
  private final Logger logger = LoggerFactory.getLogger(ImageService.class);

  @Value("${s3.bucket}")
  private String bucketName;

  private final MinioClient minioClient;

  public void uploadImage(MultipartFile file, String uniqueId) {
    try {
      try (InputStream inputStream = file.getInputStream()) {
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .object(uniqueId)
                .stream(inputStream, file.getSize(), -1)
                .contentType(file.getContentType())
                .build());
      }
    } catch (Exception e) {
      logger.error("Image upload failed {}", e.getMessage());
      throw new ImageUploadFailureException(uniqueId);
    }
  }

  @Cacheable(value = "imageUrls", key = "#uniqueId")
  public String getDownloadUrl(String uniqueId) {
    try {
      return minioClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .bucket(bucketName)
              .object(uniqueId)
              .method(Method.GET)
              .expiry(7, TimeUnit.DAYS)
              .build());
    } catch (Exception e) {
      logger.error("Image fetch failed {}", e.getMessage());
      throw new ImageUrlFetchFailureException(uniqueId);
    }
  }

  @CacheEvict(value = "imageUrls", key = "#uniqueId")
  public void deleteImage(String uniqueId) {
    try {
      minioClient.removeObject(
          RemoveObjectArgs.builder()
              .bucket(bucketName)
              .object(uniqueId)
              .build());
    } catch (Exception e) {
      logger.error("Image deletion failed {}", e.getMessage());
      throw new ImageDeletionFailureException(uniqueId);
    }
  }
}
