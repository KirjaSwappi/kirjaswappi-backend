/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service;

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
import org.springframework.web.multipart.MultipartFile;

import com.kirjaswappi.backend.common.service.ImageService;
import com.kirjaswappi.backend.jpa.daos.PhotoDao;
import com.kirjaswappi.backend.jpa.daos.UserDao;
import com.kirjaswappi.backend.jpa.repositories.PhotoRepository;
import com.kirjaswappi.backend.jpa.repositories.UserRepository;
import com.kirjaswappi.backend.service.entities.Photo;
import com.kirjaswappi.backend.service.exceptions.ImageDeletionFailureException;
import com.kirjaswappi.backend.service.exceptions.ImageUploadFailureException;
import com.kirjaswappi.backend.service.exceptions.PhotoNotFoundException;
import com.kirjaswappi.backend.service.exceptions.ResourceNotFoundException;
import com.kirjaswappi.backend.service.exceptions.UserNotFoundException;

@DisplayName("PhotoService Tests")
class PhotoServiceTest {
  @Mock
  private ImageService imageService;
  @Mock
  private UserRepository userRepository;
  @Mock
  private PhotoRepository photoRepository;
  @InjectMocks
  private PhotoService photoService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  // addProfilePhoto tests
  @Test
  @DisplayName("Should add profile photo successfully when user exists")
  void addProfilePhotoSuccess() {
    String userId = "user-123";
    MultipartFile file = mock(MultipartFile.class);
    UserDao userDao = new UserDao();
    userDao.setId(userId);
    userDao.setEmailVerified(true);
    String expectedUrl = "http://example.com/profile.jpg";

    when(userRepository.findByIdAndIsEmailVerifiedTrue(userId)).thenReturn(Optional.of(userDao));
    doNothing().when(imageService).uploadImage(any(MultipartFile.class), anyString());
    when(imageService.getDownloadUrl(anyString())).thenReturn(expectedUrl);
    when(userRepository.save(any(UserDao.class))).thenReturn(userDao);

    String result = photoService.addProfilePhoto(userId, file);

    assertEquals(expectedUrl, result);
    assertEquals(userId + "-ProfilePhoto", userDao.getProfilePhoto());
    verify(imageService).uploadImage(file, userId + "-ProfilePhoto");
    verify(userRepository).save(userDao);
  }

  @Test
  @DisplayName("Should throw UserNotFoundException when adding profile photo for non-existent user")
  void addProfilePhotoThrowsWhenUserNotFound() {
    String userId = "non-existent";
    MultipartFile file = mock(MultipartFile.class);

    when(userRepository.findByIdAndIsEmailVerifiedTrue(userId)).thenReturn(Optional.empty());

    assertThrows(UserNotFoundException.class, () -> photoService.addProfilePhoto(userId, file));
    verify(imageService, never()).uploadImage(any(), any());
  }

  @Test
  @DisplayName("Should throw ImageUploadFailureException when image upload fails for profile photo")
  void addProfilePhotoThrowsWhenUploadFails() {
    String userId = "user-123";
    MultipartFile file = mock(MultipartFile.class);
    UserDao userDao = new UserDao();
    userDao.setId(userId);
    userDao.setEmailVerified(true);

    when(userRepository.findByIdAndIsEmailVerifiedTrue(userId)).thenReturn(Optional.of(userDao));
    doThrow(new ImageUploadFailureException()).when(imageService).uploadImage(any(MultipartFile.class), anyString());

    assertThrows(ImageUploadFailureException.class, () -> photoService.addProfilePhoto(userId, file));
  }

  // addCoverPhoto tests
  @Test
  @DisplayName("Should add cover photo successfully when user exists")
  void addCoverPhotoSuccess() {
    String userId = "user-123";
    MultipartFile file = mock(MultipartFile.class);
    UserDao userDao = new UserDao();
    userDao.setId(userId);
    userDao.setEmailVerified(true);
    String expectedUrl = "http://example.com/cover.jpg";

    when(userRepository.findByIdAndIsEmailVerifiedTrue(userId)).thenReturn(Optional.of(userDao));
    doNothing().when(imageService).uploadImage(any(MultipartFile.class), anyString());
    when(imageService.getDownloadUrl(anyString())).thenReturn(expectedUrl);
    when(userRepository.save(any(UserDao.class))).thenReturn(userDao);

    String result = photoService.addCoverPhoto(userId, file);

    assertEquals(expectedUrl, result);
    assertEquals(userId + "-CoverPhoto", userDao.getCoverPhoto());
    verify(imageService).uploadImage(file, userId + "-CoverPhoto");
    verify(userRepository).save(userDao);
  }

  @Test
  @DisplayName("Should throw UserNotFoundException when adding cover photo for non-existent user")
  void addCoverPhotoThrowsWhenUserNotFound() {
    String userId = "non-existent";
    MultipartFile file = mock(MultipartFile.class);

    when(userRepository.findByIdAndIsEmailVerifiedTrue(userId)).thenReturn(Optional.empty());

    assertThrows(UserNotFoundException.class, () -> photoService.addCoverPhoto(userId, file));
    verify(imageService, never()).uploadImage(any(), any());
  }

  @Test
  @DisplayName("Should throw ImageUploadFailureException when image upload fails for cover photo")
  void addCoverPhotoThrowsWhenUploadFails() {
    String userId = "user-123";
    MultipartFile file = mock(MultipartFile.class);
    UserDao userDao = new UserDao();
    userDao.setId(userId);
    userDao.setEmailVerified(true);

    when(userRepository.findByIdAndIsEmailVerifiedTrue(userId)).thenReturn(Optional.of(userDao));
    doThrow(new ImageUploadFailureException()).when(imageService).uploadImage(any(MultipartFile.class), anyString());

    assertThrows(ImageUploadFailureException.class, () -> photoService.addCoverPhoto(userId, file));
  }

  // deleteProfilePhoto tests
  @Test
  @DisplayName("Should delete profile photo successfully when user and photo exist")
  void deleteProfilePhotoSuccess() {
    String userId = "user-123";
    String photoId = "user-123-ProfilePhoto";
    UserDao userDao = new UserDao();
    userDao.setId(userId);
    userDao.setEmailVerified(true);
    userDao.setProfilePhoto(photoId);

    when(userRepository.findByIdAndIsEmailVerifiedTrue(userId)).thenReturn(Optional.of(userDao));
    doNothing().when(imageService).deleteImage(photoId);
    when(userRepository.save(any(UserDao.class))).thenReturn(userDao);

    photoService.deleteProfilePhoto(userId);

    assertNull(userDao.getProfilePhoto());
    verify(imageService).deleteImage(photoId);
    verify(userRepository).save(userDao);
  }

  @Test
  @DisplayName("Should throw UserNotFoundException when deleting profile photo for non-existent user")
  void deleteProfilePhotoThrowsWhenUserNotFound() {
    String userId = "non-existent";

    when(userRepository.findByIdAndIsEmailVerifiedTrue(userId)).thenReturn(Optional.empty());

    assertThrows(UserNotFoundException.class, () -> photoService.deleteProfilePhoto(userId));
    verify(imageService, never()).deleteImage(any());
  }

  @Test
  @DisplayName("Should throw PhotoNotFoundException when deleting non-existent profile photo")
  void deleteProfilePhotoThrowsWhenPhotoNotFound() {
    String userId = "user-123";
    UserDao userDao = new UserDao();
    userDao.setId(userId);
    userDao.setEmailVerified(true);
    userDao.setProfilePhoto(null);

    when(userRepository.findByIdAndIsEmailVerifiedTrue(userId)).thenReturn(Optional.of(userDao));

    assertThrows(PhotoNotFoundException.class, () -> photoService.deleteProfilePhoto(userId));
    verify(imageService, never()).deleteImage(any());
  }

  @Test
  @DisplayName("Should throw ImageDeletionFailureException when image deletion fails for profile photo")
  void deleteProfilePhotoThrowsWhenDeletionFails() {
    String userId = "user-123";
    String photoId = "user-123-ProfilePhoto";
    UserDao userDao = new UserDao();
    userDao.setId(userId);
    userDao.setEmailVerified(true);
    userDao.setProfilePhoto(photoId);

    when(userRepository.findByIdAndIsEmailVerifiedTrue(userId)).thenReturn(Optional.of(userDao));
    doThrow(new ImageDeletionFailureException()).when(imageService).deleteImage(photoId);

    assertThrows(ImageDeletionFailureException.class, () -> photoService.deleteProfilePhoto(userId));
  }

  // deleteCoverPhoto tests
  @Test
  @DisplayName("Should delete cover photo successfully when user and photo exist")
  void deleteCoverPhotoSuccess() {
    String userId = "user-123";
    String photoId = "user-123-CoverPhoto";
    UserDao userDao = new UserDao();
    userDao.setId(userId);
    userDao.setEmailVerified(true);
    userDao.setCoverPhoto(photoId);

    when(userRepository.findByIdAndIsEmailVerifiedTrue(userId)).thenReturn(Optional.of(userDao));
    doNothing().when(imageService).deleteImage(photoId);
    when(userRepository.save(any(UserDao.class))).thenReturn(userDao);

    photoService.deleteCoverPhoto(userId);

    assertNull(userDao.getCoverPhoto());
    verify(imageService).deleteImage(photoId);
    verify(userRepository).save(userDao);
  }

  @Test
  @DisplayName("Should throw UserNotFoundException when deleting cover photo for non-existent user")
  void deleteCoverPhotoThrowsWhenUserNotFound() {
    String userId = "non-existent";

    when(userRepository.findByIdAndIsEmailVerifiedTrue(userId)).thenReturn(Optional.empty());

    assertThrows(UserNotFoundException.class, () -> photoService.deleteCoverPhoto(userId));
    verify(imageService, never()).deleteImage(any());
  }

  @Test
  @DisplayName("Should throw PhotoNotFoundException when deleting non-existent cover photo")
  void deleteCoverPhotoThrowsWhenPhotoNotFound() {
    String userId = "user-123";
    UserDao userDao = new UserDao();
    userDao.setId(userId);
    userDao.setEmailVerified(true);
    userDao.setCoverPhoto(null);

    when(userRepository.findByIdAndIsEmailVerifiedTrue(userId)).thenReturn(Optional.of(userDao));

    assertThrows(PhotoNotFoundException.class, () -> photoService.deleteCoverPhoto(userId));
    verify(imageService, never()).deleteImage(any());
  }

  @Test
  @DisplayName("Should throw ImageDeletionFailureException when image deletion fails for cover photo")
  void deleteCoverPhotoThrowsWhenDeletionFails() {
    String userId = "user-123";
    String photoId = "user-123-CoverPhoto";
    UserDao userDao = new UserDao();
    userDao.setId(userId);
    userDao.setEmailVerified(true);
    userDao.setCoverPhoto(photoId);

    when(userRepository.findByIdAndIsEmailVerifiedTrue(userId)).thenReturn(Optional.of(userDao));
    doThrow(new ImageDeletionFailureException()).when(imageService).deleteImage(photoId);

    assertThrows(ImageDeletionFailureException.class, () -> photoService.deleteCoverPhoto(userId));
  }

  // getPhotoByUserEmail tests
  @Test
  @DisplayName("Should return profile photo URL when getting by email")
  void getPhotoByUserEmailReturnsProfilePhotoUrl() {
    String email = "test@example.com";
    String photoId = "user-123-ProfilePhoto";
    String expectedUrl = "http://example.com/profile.jpg";
    UserDao userDao = new UserDao();
    userDao.setEmail(email);
    userDao.setEmailVerified(true);
    userDao.setProfilePhoto(photoId);

    when(userRepository.findByEmailAndIsEmailVerified(email, true)).thenReturn(Optional.of(userDao));
    when(imageService.getDownloadUrl(photoId)).thenReturn(expectedUrl);

    String result = photoService.getPhotoByUserEmail(email, true);

    assertEquals(expectedUrl, result);
    verify(imageService).getDownloadUrl(photoId);
  }

  @Test
  @DisplayName("Should return cover photo URL when getting by email")
  void getPhotoByUserEmailReturnsCoverPhotoUrl() {
    String email = "test@example.com";
    String photoId = "user-123-CoverPhoto";
    String expectedUrl = "http://example.com/cover.jpg";
    UserDao userDao = new UserDao();
    userDao.setEmail(email);
    userDao.setEmailVerified(true);
    userDao.setCoverPhoto(photoId);

    when(userRepository.findByEmailAndIsEmailVerified(email, true)).thenReturn(Optional.of(userDao));
    when(imageService.getDownloadUrl(photoId)).thenReturn(expectedUrl);

    String result = photoService.getPhotoByUserEmail(email, false);

    assertEquals(expectedUrl, result);
    verify(imageService).getDownloadUrl(photoId);
  }

  @Test
  @DisplayName("Should throw UserNotFoundException when getting photo by non-existent email")
  void getPhotoByUserEmailThrowsWhenUserNotFound() {
    String email = "nonexistent@example.com";

    when(userRepository.findByEmailAndIsEmailVerified(email, true)).thenReturn(Optional.empty());

    assertThrows(UserNotFoundException.class, () -> photoService.getPhotoByUserEmail(email, true));
  }

  @Test
  @DisplayName("Should throw PhotoNotFoundException when user has no profile photo")
  void getPhotoByUserEmailThrowsWhenPhotoNotFound() {
    String email = "test@example.com";
    UserDao userDao = new UserDao();
    userDao.setEmail(email);
    userDao.setEmailVerified(true);
    userDao.setProfilePhoto(null);

    when(userRepository.findByEmailAndIsEmailVerified(email, true)).thenReturn(Optional.of(userDao));

    assertThrows(PhotoNotFoundException.class, () -> photoService.getPhotoByUserEmail(email, true));
  }

  // getPhotoByUserId tests
  @Test
  @DisplayName("Should return profile photo URL when getting by user ID")
  void getPhotoByUserIdReturnsProfilePhotoUrl() {
    String userId = "user-123";
    String photoId = "user-123-ProfilePhoto";
    String expectedUrl = "http://example.com/profile.jpg";
    UserDao userDao = new UserDao();
    userDao.setId(userId);
    userDao.setEmailVerified(true);
    userDao.setProfilePhoto(photoId);

    when(userRepository.findByIdAndIsEmailVerifiedTrue(userId)).thenReturn(Optional.of(userDao));
    when(imageService.getDownloadUrl(photoId)).thenReturn(expectedUrl);

    String result = photoService.getPhotoByUserId(userId, true);

    assertEquals(expectedUrl, result);
    verify(imageService).getDownloadUrl(photoId);
  }

  @Test
  @DisplayName("Should return cover photo URL when getting by user ID")
  void getPhotoByUserIdReturnsCoverPhotoUrl() {
    String userId = "user-123";
    String photoId = "user-123-CoverPhoto";
    String expectedUrl = "http://example.com/cover.jpg";
    UserDao userDao = new UserDao();
    userDao.setId(userId);
    userDao.setEmailVerified(true);
    userDao.setCoverPhoto(photoId);

    when(userRepository.findByIdAndIsEmailVerifiedTrue(userId)).thenReturn(Optional.of(userDao));
    when(imageService.getDownloadUrl(photoId)).thenReturn(expectedUrl);

    String result = photoService.getPhotoByUserId(userId, false);

    assertEquals(expectedUrl, result);
    verify(imageService).getDownloadUrl(photoId);
  }

  @Test
  @DisplayName("Should throw UserNotFoundException when getting photo by non-existent user ID")
  void getPhotoByUserIdThrowsWhenUserNotFound() {
    String userId = "non-existent";

    when(userRepository.findByIdAndIsEmailVerifiedTrue(userId)).thenReturn(Optional.empty());

    assertThrows(UserNotFoundException.class, () -> photoService.getPhotoByUserId(userId, true));
  }

  @Test
  @DisplayName("Should throw PhotoNotFoundException when user has no cover photo")
  void getPhotoByUserIdThrowsWhenPhotoNotFound() {
    String userId = "user-123";
    UserDao userDao = new UserDao();
    userDao.setId(userId);
    userDao.setEmailVerified(true);
    userDao.setCoverPhoto(null);

    when(userRepository.findByIdAndIsEmailVerifiedTrue(userId)).thenReturn(Optional.of(userDao));

    assertThrows(PhotoNotFoundException.class, () -> photoService.getPhotoByUserId(userId, false));
  }

  // addBookCoverPhoto tests
  @Test
  @DisplayName("Should add book cover photo successfully")
  void addBookCoverPhotoSuccess() {
    MultipartFile file = mock(MultipartFile.class);
    String uniqueId = "book-123-cover";

    doNothing().when(imageService).uploadImage(file, uniqueId);

    assertDoesNotThrow(() -> photoService.addBookCoverPhoto(file, uniqueId));
    verify(imageService).uploadImage(file, uniqueId);
  }

  @Test
  @DisplayName("Should throw ImageUploadFailureException when image upload fails for book cover photo")
  void addBookCoverPhotoThrowsOnFailure() {
    MultipartFile file = mock(MultipartFile.class);
    String uniqueId = "book-123-cover";

    doThrow(new ImageUploadFailureException()).when(imageService).uploadImage(file, uniqueId);

    assertThrows(ImageUploadFailureException.class, () -> photoService.addBookCoverPhoto(file, uniqueId));
  }

  // deleteBookCoverPhoto tests
  @Test
  @DisplayName("Should delete book cover photo successfully")
  void deleteBookCoverPhotoSuccess() {
    String uniqueId = "book-123-cover";

    doNothing().when(imageService).deleteImage(uniqueId);

    photoService.deleteBookCoverPhoto(uniqueId);

    verify(imageService).deleteImage(uniqueId);
  }

  @Test
  @DisplayName("Should throw ImageDeletionFailureException when image deletion fails for book cover photo")
  void deleteBookCoverPhotoThrowsIfImageServiceFails() {
    String uniqueId = "book-123-cover";

    doThrow(new ImageDeletionFailureException()).when(imageService).deleteImage(uniqueId);

    assertThrows(ImageDeletionFailureException.class, () -> photoService.deleteBookCoverPhoto(uniqueId));
  }

  // getBookCoverPhoto tests
  @Test
  @DisplayName("Should return URL for book cover photo")
  void getBookCoverPhotoReturnsUrl() {
    String uniqueId = "book-123-cover";
    String expectedUrl = "http://example.com/book-cover.jpg";

    when(imageService.getDownloadUrl(uniqueId)).thenReturn(expectedUrl);

    String result = photoService.getBookCoverPhoto(uniqueId);

    assertEquals(expectedUrl, result);
    verify(imageService).getDownloadUrl(uniqueId);
  }

  @Test
  @DisplayName("Should throw PhotoNotFoundException when book cover photo uniqueId is null")
  void getBookCoverPhotoThrowsWhenUniqueIdIsNull() {
    assertThrows(PhotoNotFoundException.class, () -> photoService.getBookCoverPhoto(null));
    verify(imageService, never()).getDownloadUrl(any());
  }

  // addSupportedCoverPhoto tests
  @Test
  @DisplayName("Should add supported cover photo successfully")
  void addSupportedCoverPhotoSuccess() {
    MultipartFile file = mock(MultipartFile.class);
    String expectedUrl = "http://example.com/supported-cover.jpg";

    doNothing().when(imageService).uploadImage(any(MultipartFile.class), anyString());
    when(imageService.getDownloadUrl(anyString())).thenReturn(expectedUrl);
    when(photoRepository.save(any(PhotoDao.class))).thenAnswer(invocation -> invocation.getArgument(0));

    String result = photoService.addSupportedCoverPhoto(file);

    assertEquals(expectedUrl, result);
    verify(imageService).uploadImage(eq(file), contains("-Supported-Cover-Photo"));
    verify(photoRepository).save(any(PhotoDao.class));
  }

  @Test
  @DisplayName("Should throw ImageUploadFailureException when upload fails for supported cover photo")
  void addSupportedCoverPhotoThrowsWhenUploadFails() {
    MultipartFile file = mock(MultipartFile.class);

    doThrow(new ImageUploadFailureException()).when(imageService).uploadImage(any(MultipartFile.class), anyString());

    assertThrows(ImageUploadFailureException.class, () -> photoService.addSupportedCoverPhoto(file));
    verify(photoRepository, never()).save(any());
  }

  // deleteSupportedCoverPhoto tests
  @Test
  @DisplayName("Should delete supported cover photo successfully")
  void deleteSupportedCoverPhotoSuccess() {
    String coverPhotoId = "photo-123";
    String uniqueId = "photo-123-Supported-Cover-Photo";
    PhotoDao photoDao = new PhotoDao(coverPhotoId, uniqueId);

    when(photoRepository.findById(coverPhotoId)).thenReturn(Optional.of(photoDao));
    doNothing().when(imageService).deleteImage(uniqueId);
    doNothing().when(photoRepository).delete(photoDao);

    photoService.deleteSupportedCoverPhoto(coverPhotoId);

    verify(imageService).deleteImage(uniqueId);
    verify(photoRepository).delete(photoDao);
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when deleting non-existent supported cover photo")
  void deleteSupportedCoverPhotoThrowsWhenNotFound() {
    String coverPhotoId = "non-existent";

    when(photoRepository.findById(coverPhotoId)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> photoService.deleteSupportedCoverPhoto(coverPhotoId));
    verify(imageService, never()).deleteImage(any());
    verify(photoRepository, never()).delete(any());
  }

  @Test
  @DisplayName("Should throw ImageDeletionFailureException when image deletion fails for supported cover photo")
  void deleteSupportedCoverPhotoThrowsWhenDeletionFails() {
    String coverPhotoId = "photo-123";
    String uniqueId = "photo-123-Supported-Cover-Photo";
    PhotoDao photoDao = new PhotoDao(coverPhotoId, uniqueId);

    when(photoRepository.findById(coverPhotoId)).thenReturn(Optional.of(photoDao));
    doThrow(new ImageDeletionFailureException()).when(imageService).deleteImage(uniqueId);

    assertThrows(ImageDeletionFailureException.class, () -> photoService.deleteSupportedCoverPhoto(coverPhotoId));
    verify(photoRepository, never()).delete(any());
  }

  // findSupportedCoverPhoto tests
  @Test
  @DisplayName("Should return list of supported cover photos")
  void findSupportedCoverPhotoReturnsListOfPhotos() {
    PhotoDao photo1 = new PhotoDao("id1", "uniqueId1");
    PhotoDao photo2 = new PhotoDao("id2", "uniqueId2");
    String url1 = "http://example.com/photo1.jpg";
    String url2 = "http://example.com/photo2.jpg";

    when(photoRepository.findAll()).thenReturn(List.of(photo1, photo2));
    when(imageService.getDownloadUrl("uniqueId1")).thenReturn(url1);
    when(imageService.getDownloadUrl("uniqueId2")).thenReturn(url2);

    List<Photo> result = photoService.findSupportedCoverPhoto();

    assertEquals(2, result.size());
    assertEquals("id1", result.get(0).getId());
    assertEquals(url1, result.get(0).getCoverPhoto());
    assertEquals("id2", result.get(1).getId());
    assertEquals(url2, result.get(1).getCoverPhoto());
  }

  @Test
  @DisplayName("Should return empty list when no supported cover photos exist")
  void findSupportedCoverPhotoReturnsEmptyList() {
    when(photoRepository.findAll()).thenReturn(List.of());

    List<Photo> result = photoService.findSupportedCoverPhoto();

    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should throw PhotoNotFoundException when supported cover photo has null coverPhoto field")
  void findSupportedCoverPhotoThrowsWhenCoverPhotoIsNull() {
    PhotoDao photoDao = new PhotoDao("id1", null);

    when(photoRepository.findAll()).thenReturn(List.of(photoDao));

    assertThrows(PhotoNotFoundException.class, () -> photoService.findSupportedCoverPhoto());
  }
}
