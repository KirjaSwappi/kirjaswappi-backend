/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.kirjaswappi.backend.jpa.daos.BookLocationDao;
import com.kirjaswappi.backend.service.entities.BookLocation;

/**
 * Unit tests for BookLocationMapper.
 */
public class BookLocationMapperTest {

  @Test
  public void testToDao_WithValidEntity_ShouldMapCorrectly() {
    // Arrange
    BookLocation entity = new BookLocation();
    entity.setLatitude(60.1699);
    entity.setLongitude(24.9384);
    entity.setAddress("Mannerheimintie 12");
    entity.setCity("Helsinki");
    entity.setCountry("Finland");
    entity.setPostalCode("00100");
    entity.setRadiusKm(50);

    // Act
    BookLocationDao dao = BookLocationMapper.toDao(entity);

    // Assert
    assertNotNull(dao);
    assertEquals(60.1699, dao.getLatitude());
    assertEquals(24.9384, dao.getLongitude());
    assertEquals("Mannerheimintie 12", dao.getAddress());
    assertEquals("Helsinki", dao.getCity());
    assertEquals("Finland", dao.getCountry());
    assertEquals("00100", dao.getPostalCode());
    assertEquals(50, dao.getRadiusKm());
  }

  @Test
  public void testToDao_WithNullEntity_ShouldReturnNull() {
    // Act
    BookLocationDao dao = BookLocationMapper.toDao(null);

    // Assert
    assertNull(dao);
  }

  @Test
  public void testToEntity_WithValidDao_ShouldMapCorrectly() {
    // Arrange
    BookLocationDao dao = new BookLocationDao();
    dao.setLatitude(60.1699);
    dao.setLongitude(24.9384);
    dao.setAddress("Mannerheimintie 12");
    dao.setCity("Helsinki");
    dao.setCountry("Finland");
    dao.setPostalCode("00100");
    dao.setRadiusKm(50);

    // Act
    BookLocation entity = BookLocationMapper.toEntity(dao);

    // Assert
    assertNotNull(entity);
    assertEquals(60.1699, entity.getLatitude());
    assertEquals(24.9384, entity.getLongitude());
    assertEquals("Mannerheimintie 12", entity.getAddress());
    assertEquals("Helsinki", entity.getCity());
    assertEquals("Finland", entity.getCountry());
    assertEquals("00100", entity.getPostalCode());
    assertEquals(50, entity.getRadiusKm());
  }

  @Test
  public void testToEntity_WithNullDao_ShouldReturnNull() {
    // Act
    BookLocation entity = BookLocationMapper.toEntity(null);

    // Assert
    assertNull(entity);
  }

  @Test
  public void testBookLocation_HelperMethods() {
    // Arrange
    BookLocation location = new BookLocation();

    // Test hasCoordinates - should be false initially
    assertFalse(location.hasCoordinates());

    // Set coordinates
    location.setLatitude(60.1699);
    location.setLongitude(24.9384);
    assertTrue(location.hasCoordinates());

    // Test hasAddress - should be false initially
    assertFalse(location.hasAddress());

    // Set address
    location.setAddress("Mannerheimintie 12");
    assertTrue(location.hasAddress());

    // Test with empty address
    location.setAddress("");
    assertFalse(location.hasAddress());

    // Test with whitespace-only address
    location.setAddress("   ");
    assertFalse(location.hasAddress());
  }
}
