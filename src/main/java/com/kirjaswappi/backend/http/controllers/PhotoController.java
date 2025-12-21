/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.controllers;

import static com.kirjaswappi.backend.common.utils.Constants.*;

import java.util.List;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kirjaswappi.backend.http.dtos.requests.CreatePhotoRequest;
import com.kirjaswappi.backend.http.dtos.requests.CreateSupportedCoverPhotoRequest;
import com.kirjaswappi.backend.http.dtos.responses.PhotoResponse;
import com.kirjaswappi.backend.http.dtos.responses.SupportedCoverPhotoListResponse;
import com.kirjaswappi.backend.service.PhotoService;

@RestController
@RequestMapping(API_BASE + PHOTOS)
@Validated
@Tag(name = "Photos", description = "API for managing user profile photos and book images")
public class PhotoController {
  @Autowired
  private PhotoService photoService;

  @PostMapping(PROFILE_PHOTO)
  @Operation(summary = "Add profile photo.", description = "Add profile photo to a user.", responses = {
      @ApiResponse(responseCode = "200", description = "Profile photo added.") })
  public ResponseEntity<PhotoResponse> addProfilePhoto(@Valid @ModelAttribute CreatePhotoRequest request) {
    var imageUrl = photoService.addProfilePhoto(request.getUserId(), request.getImage());
    return ResponseEntity.ok(new PhotoResponse(imageUrl));
  }

  @PostMapping(COVER_PHOTO)
  @Operation(summary = "Add cover photo.", description = "Add cover photo to a user.", responses = {
      @ApiResponse(responseCode = "200", description = "Cover photo added.") })
  public ResponseEntity<PhotoResponse> addCoverPhoto(@Valid @ModelAttribute CreatePhotoRequest request) {
    var imageUrl = photoService.addCoverPhoto(request.getUserId(), request.getImage());
    return ResponseEntity.ok(new PhotoResponse(imageUrl));
  }

  @DeleteMapping(PROFILE_PHOTO + ID)
  @Operation(summary = "Delete profile photo.", description = "Delete profile photo of a user.", responses = {
      @ApiResponse(responseCode = "204", description = "Profile photo deleted."),
      @ApiResponse(responseCode = "404", description = "User or photo not found.") })
  public ResponseEntity<Void> deleteProfilePhoto(@Parameter(description = "User ID.") @PathVariable String id) {
    photoService.deleteProfilePhoto(id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping(COVER_PHOTO + ID)
  @Operation(summary = "Delete cover photo.", description = "Delete cover photo of a user.", responses = {
      @ApiResponse(responseCode = "204", description = "Cover photo deleted."),
      @ApiResponse(responseCode = "404", description = "User or photo not found.") })
  public ResponseEntity<Void> deleteCoverPhoto(@Parameter(description = "User ID.") @PathVariable String id) {
    photoService.deleteCoverPhoto(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping(PROFILE_PHOTO + BY_EMAIL + EMAIL)
  @Operation(summary = "Get profile photo by email.", description = "Get profile photo of a user by email.", responses = {
      @ApiResponse(responseCode = "200", description = "Profile photo found."),
      @ApiResponse(responseCode = "404", description = "User or photo not found.") })
  public ResponseEntity<PhotoResponse> getProfilePhotoByEmail(
      @Parameter(description = "User email.") @PathVariable String email) {
    var imageUrl = photoService.getPhotoByUserEmail(email, true);
    return ResponseEntity.ok(new PhotoResponse(imageUrl));
  }

  @GetMapping(COVER_PHOTO + BY_EMAIL + EMAIL)
  @Operation(summary = "Get cover photo by email.", description = "Get cover photo of a user by email.", responses = {
      @ApiResponse(responseCode = "200", description = "Cover photo found."),
      @ApiResponse(responseCode = "404", description = "User or photo not found.") })
  public ResponseEntity<PhotoResponse> getCoverPhotoByEmail(
      @Parameter(description = "User email.") @PathVariable String email) {
    var imageUrl = photoService.getPhotoByUserEmail(email, false);
    return ResponseEntity.ok(new PhotoResponse(imageUrl));
  }

  @GetMapping(PROFILE_PHOTO + BY_ID + ID)
  @Operation(summary = "Get profile photo by user id.", description = "Get profile photo of a user by user ID.", responses = {
      @ApiResponse(responseCode = "200", description = "Profile photo found."),
      @ApiResponse(responseCode = "404", description = "User or photo not found.") })
  public ResponseEntity<PhotoResponse> getProfilePhotoById(
      @Parameter(description = "User ID.") @PathVariable String id) {
    var imageUrl = photoService.getPhotoByUserId(id, true);
    return ResponseEntity.ok(new PhotoResponse(imageUrl));
  }

  @GetMapping(COVER_PHOTO + BY_ID + ID)
  @Operation(summary = "Get cover photo by user id.", description = "Get cover photo of a user by user ID.", responses = {
      @ApiResponse(responseCode = "200", description = "Cover photo found."),
      @ApiResponse(responseCode = "404", description = "User or photo not found.") })
  public ResponseEntity<PhotoResponse> getCoverPhotoById(@Parameter(description = "User ID.") @PathVariable String id) {
    var imageUrl = photoService.getPhotoByUserId(id, false);
    return ResponseEntity.ok(new PhotoResponse(imageUrl));
  }

  @PostMapping(SUPPORTED_COVER_PHOTOS)
  @Operation(summary = "Add supported cover photo.", description = "Add supported cover photo.", responses = {
      @ApiResponse(responseCode = "200", description = "Supported cover photo added.") })
  public ResponseEntity<PhotoResponse> addSupportedCoverPhoto(
      @Valid @ModelAttribute CreateSupportedCoverPhotoRequest request) {
    var imageUrl = photoService.addSupportedCoverPhoto(request.getCoverPhoto());
    return ResponseEntity.ok(new PhotoResponse(imageUrl));
  }

  @GetMapping(SUPPORTED_COVER_PHOTOS)
  @Operation(summary = "Find all supported cover photos.", description = "Find all supported cover photos.", responses = {
      @ApiResponse(responseCode = "200", description = "List of all supported cover photos.") })
  public ResponseEntity<List<SupportedCoverPhotoListResponse>> findSupportedCoverPhotos() {
    var supportedCoverPhotos = photoService.findSupportedCoverPhoto();
    return ResponseEntity.ok(supportedCoverPhotos.stream().map(SupportedCoverPhotoListResponse::new).toList());
  }

  @DeleteMapping(SUPPORTED_COVER_PHOTOS + ID)
  @Operation(summary = "Delete a supported cover photo.", description = "Delete a supported cover photo.", responses = {
      @ApiResponse(responseCode = "204", description = "Cover photo deleted.") })
  public ResponseEntity<Void> deleteSupportedCoverPhoto(
      @Parameter(description = "Supported cover photo ID.") @PathVariable String id) {
    photoService.deleteSupportedCoverPhoto(id);
    return ResponseEntity.noContent().build();
  }
}
