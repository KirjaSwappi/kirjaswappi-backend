/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.validations;

import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;

import com.kirjaswappi.backend.service.exceptions.BadRequestException;
import com.kirjaswappi.backend.service.exceptions.UnsupportedMediaTypeException;

public class ValidationUtil {
  public static boolean validateNotBlank(String input) {
    return input != null && !input.trim().isEmpty();
  }

  public static boolean validateEmail(String emailAddress) {
    if (emailAddress == null || emailAddress.trim().isEmpty()) {
      return false;
    }
    String regexPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    return Pattern.compile(regexPattern).matcher(emailAddress).matches();
  }

  public static void validatePassword(String password, String fieldName) {
    if (password == null || password.trim().isEmpty()) {
      throw new BadRequestException(fieldName + "CannotBeBlank", fieldName);
    }
    if (password.length() < 8) {
      throw new BadRequestException("passwordTooShort", fieldName);
    }
    if (!Pattern.compile("[A-Z]").matcher(password).find()) {
      throw new BadRequestException("passwordMissingUppercase", fieldName);
    }
    if (!Pattern.compile("[a-z]").matcher(password).find()) {
      throw new BadRequestException("passwordMissingLowercase", fieldName);
    }
    if (!Pattern.compile("[0-9]").matcher(password).find()) {
      throw new BadRequestException("passwordMissingDigit", fieldName);
    }
  }

  public static boolean validateOtp(String otp) {
    return otp == null
        || otp.trim().isEmpty()
        || otp.length() != 6
        || !otp.chars().allMatch(Character::isDigit);
  }

  public static void validateMediaType(MultipartFile image) {
    String[] allowedExtensions = { "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic" };
    String contentType = image.getContentType();
    String fileExtension = FilenameUtils.getExtension(image.getOriginalFilename());

    if (contentType == null || !contentType.startsWith("image/") ||
        fileExtension == null || !Arrays.asList(allowedExtensions).contains(fileExtension.toLowerCase())) {
      throw new UnsupportedMediaTypeException(image);
    }
  }

  public static UUID validateUUID(String id) {
    try {
      return UUID.fromString(id);
    } catch (Exception e) {
      throw new BadRequestException("invalidUUID", id);
    }
  }
}
