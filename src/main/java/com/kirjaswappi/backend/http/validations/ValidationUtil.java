/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.validations;

import java.io.IOException;
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

    // Magic-byte sniffing. Client-supplied content type and filename can lie;
    // verify the leading bytes match an image format we accept.
    if (!hasImageMagicBytes(image)) {
      throw new UnsupportedMediaTypeException(image);
    }
  }

  /**
   * Returns true if the first bytes of {@code image} match a known image format
   * header (JPEG, PNG, GIF, BMP, WEBP, HEIC/HEIF).
   */
  private static boolean hasImageMagicBytes(MultipartFile image) {
    if (image == null || image.isEmpty()) {
      return false;
    }
    byte[] head;
    try (var in = image.getInputStream()) {
      head = in.readNBytes(16);
    } catch (IOException e) {
      return false;
    }
    if (head == null || head.length < 4) {
      return false;
    }

    // JPEG: FF D8 FF
    if ((head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
      return true;
    }
    // PNG: 89 50 4E 47 0D 0A 1A 0A
    if (head.length >= 8
        && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G'
        && (head[4] & 0xFF) == 0x0D && (head[5] & 0xFF) == 0x0A
        && (head[6] & 0xFF) == 0x1A && (head[7] & 0xFF) == 0x0A) {
      return true;
    }
    // GIF: "GIF87a" or "GIF89a"
    if (head.length >= 6 && head[0] == 'G' && head[1] == 'I' && head[2] == 'F'
        && head[3] == '8' && (head[4] == '7' || head[4] == '9') && head[5] == 'a') {
      return true;
    }
    // BMP: "BM"
    if (head[0] == 'B' && head[1] == 'M') {
      return true;
    }
    // WEBP: "RIFF" .... "WEBP"
    if (head.length >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
        && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
      return true;
    }
    // HEIC/HEIF: "....ftypheic" / "....ftypmif1" / "....ftypmsf1"
    if (head.length >= 12 && head[4] == 'f' && head[5] == 't' && head[6] == 'y' && head[7] == 'p') {
      return true;
    }
    return false;
  }

  public static UUID validateUUID(String id) {
    try {
      return UUID.fromString(id);
    } catch (Exception e) {
      throw new BadRequestException("invalidUUID", id);
    }
  }
}
