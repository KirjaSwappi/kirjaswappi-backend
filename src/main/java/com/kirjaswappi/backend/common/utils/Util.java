/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.function.Function;

import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItem;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.multipart.MultipartFile;

public final class Util {

  private Util() {
    throw new IllegalStateException("Utility class should not be instantiated");
  }

  public static String generateSalt() {
    return BCrypt.gensalt();
  }

  public static String hashPassword(String password, String salt) {
    return BCrypt.hashpw(password, salt);
  }

  public static MultipartFile convertBase64ImageToMultipartFile(String base64Image, String fileName, String contentType)
      throws IOException {
    byte[] decodedBytes = Base64.getDecoder().decode(base64Image);
    if (decodedBytes == null || decodedBytes.length == 0) {
      return null;
    }

    FileItem fileItem = new DiskFileItem("file", contentType, true, fileName, decodedBytes.length, null);
    try (OutputStream outputStream = fileItem.getOutputStream()) {
      outputStream.write(decodedBytes);
    }
    return new FileItemMultipartFile(fileItem);
  }

  public static <T, R> R mapIfNotNull(T obj, Function<T, R> mapper) {
    if (obj == null) {
      return null;
    }
    return mapper.apply(obj);
  }

  public static <T> T defaultIfNull(T obj, T defaultValue) {
    return obj != null ? obj : defaultValue;
  }
}
