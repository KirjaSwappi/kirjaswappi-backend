/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.utils;

import java.io.IOException;
import java.io.InputStream;

import org.apache.tomcat.util.http.fileupload.FileItem;
import org.springframework.web.multipart.MultipartFile;

public class FileItemMultipartFile implements MultipartFile {
  private final FileItem fileItem;

  public FileItemMultipartFile(FileItem fileItem) {
    this.fileItem = fileItem;
  }

  @Override
  public String getName() {
    return fileItem.getFieldName();
  }

  @Override
  public String getOriginalFilename() {
    return fileItem.getName();
  }

  @Override
  public String getContentType() {
    return fileItem.getContentType();
  }

  @Override
  public boolean isEmpty() {
    return fileItem.getSize() == 0;
  }

  @Override
  public long getSize() {
    return fileItem.getSize();
  }

  @Override
  public byte[] getBytes() throws IOException {
    return fileItem.get();
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return fileItem.getInputStream();
  }

  @Override
  public void transferTo(java.io.File dest) throws IOException {
    try {
      fileItem.write(dest);
    } catch (Exception e) {
      throw new IOException("Failed to transfer file to destination", e);
    }
  }
}
