/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.kirjaswappi.backend.common.service.ImageService;
import com.kirjaswappi.backend.jpa.daos.PhotoDao;
import com.kirjaswappi.backend.jpa.repositories.PhotoRepository;
import com.kirjaswappi.backend.jpa.repositories.UserRepository;
import com.kirjaswappi.backend.mapper.PhotoMapper;
import com.kirjaswappi.backend.service.entities.Photo;
import com.kirjaswappi.backend.service.exceptions.PhotoNotFoundException;
import com.kirjaswappi.backend.service.exceptions.ResourceNotFoundException;
import com.kirjaswappi.backend.service.exceptions.UserNotFoundException;

@Service
@Transactional
public class PhotoService {
  private final Logger logger = LoggerFactory.getLogger(PhotoService.class);

  @Autowired
  private ImageService imageService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PhotoRepository photoRepository;

  public String addProfilePhoto(String userId, MultipartFile file) {
    logger.debug("Adding profile photo for user: {}", userId);
    return addUserPhoto(userId, file, true);
  }

  public String addCoverPhoto(String userId, MultipartFile file) {
    logger.debug("Adding cover photo for user: {}", userId);
    return addUserPhoto(userId, file, false);
  }

  public void deleteProfilePhoto(String userId) {
    logger.debug("Deleting profile photo for user: {}", userId);
    deleteUserPhoto(userId, true);
  }

  public void deleteCoverPhoto(String userId) {
    logger.debug("Deleting cover photo for user: {}", userId);
    deleteUserPhoto(userId, false);
  }

  public String getPhotoByUserEmail(String email, boolean isProfilePhoto) {
    var user = userRepository.findByEmailAndIsEmailVerified(email, true).orElseThrow(UserNotFoundException::new);
    var uniqueId = isProfilePhoto ? user.getProfilePhoto() : user.getCoverPhoto();
    if (uniqueId == null)
      throw new PhotoNotFoundException();
    return imageService.getDownloadUrl(uniqueId);
  }

  public String getPhotoByUserId(String userId, boolean isProfilePhoto) {
    var user = userRepository.findByIdAndIsEmailVerifiedTrue(userId).orElseThrow(UserNotFoundException::new);
    var uniqueId = isProfilePhoto ? user.getProfilePhoto() : user.getCoverPhoto();
    if (uniqueId == null)
      throw new PhotoNotFoundException();
    return imageService.getDownloadUrl(uniqueId);
  }

  public void addBookCoverPhoto(MultipartFile file, String uniqueId) {
    logger.debug("Adding cover photo for book with uniqueId: {}", uniqueId);
    imageService.uploadImage(file, uniqueId);
  }

  public void deleteBookCoverPhoto(String uniqueId) {
    logger.debug("Deleting cover photo for book with uniqueId: {}", uniqueId);
    imageService.deleteImage(uniqueId);
  }

  public String getBookCoverPhoto(String uniqueId) {
    logger.debug("Fetching cover photo URL for book with uniqueId: {}", uniqueId);
    if (uniqueId == null)
      throw new PhotoNotFoundException();
    return imageService.getDownloadUrl(uniqueId);
  }

  private String addUserPhoto(String userId, MultipartFile file, boolean isProfilePhoto) {
    var userDao = userRepository.findByIdAndIsEmailVerifiedTrue(userId).orElseThrow(UserNotFoundException::new);
    var uniqueId = userDao.getId() + "-" + (isProfilePhoto ? "ProfilePhoto" : "CoverPhoto");
    imageService.uploadImage(file, uniqueId);
    if (isProfilePhoto) {
      userDao.setProfilePhoto(uniqueId);
    } else {
      userDao.setCoverPhoto(uniqueId);
    }
    userRepository.save(userDao);
    return imageService.getDownloadUrl(uniqueId);
  }

  private void deleteUserPhoto(String userId, boolean isProfilePhoto) {
    var userDao = userRepository.findByIdAndIsEmailVerifiedTrue(userId).orElseThrow(UserNotFoundException::new);
    var uniqueId = isProfilePhoto ? userDao.getProfilePhoto() : userDao.getCoverPhoto();
    if (uniqueId == null)
      throw new PhotoNotFoundException();
    imageService.deleteImage(uniqueId);
    if (isProfilePhoto) {
      userDao.setProfilePhoto(null);
    } else {
      userDao.setCoverPhoto(null);
    }
    userRepository.save(userDao);
  }

  public void deleteSupportedCoverPhoto(String coverPhotoId) {
    var dao = photoRepository.findById(coverPhotoId)
        .orElseThrow(() -> new ResourceNotFoundException("coverPhotoNotFound", coverPhotoId));
    imageService.deleteImage(dao.getCoverPhoto());
    photoRepository.delete(dao);
  }

  public String addSupportedCoverPhoto(MultipartFile file) {
    var id = UUID.randomUUID().toString();
    var uniqueId = id + "-Supported-Cover-Photo";
    imageService.uploadImage(file, uniqueId);

    var photo = new PhotoDao(id, uniqueId);
    photoRepository.save(photo);

    return imageService.getDownloadUrl(uniqueId);
  }

  public List<Photo> findSupportedCoverPhoto() {
    List<Photo> supportedPhotos = new ArrayList<>();
    for (PhotoDao supportedPhoto : photoRepository.findAll()) {
      if (supportedPhoto.getCoverPhoto() == null)
        throw new PhotoNotFoundException();
      supportedPhotos.add(PhotoMapper.toEntity(supportedPhoto.getId(),
          imageService.getDownloadUrl(supportedPhoto.getCoverPhoto())));
    }
    return supportedPhotos;
  }
}
