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
class BookLocationMapperTest {

  @Test
  void testToDao_WithValidEntity_ShouldMapCorrectly() {
    // Arrange
    var entity = BookLocation.builder()
        .latitude(60.1699)
        .longitude(24.9384)
        .address("Mannerheimintie 12")
        .city("Helsinki")
        .country("Finland")
        .postalCode("00100")
        .radiusKm(50)
        .build();

    // Act
    BookLocationDao dao = BookLocationMapper.toDao(entity);

    // Assert
    assertNotNull(dao);
    assertEquals(60.1699, dao.latitude());
    assertEquals(24.9384, dao.longitude());
    assertEquals("Mannerheimintie 12", dao.address());
    assertEquals("Helsinki", dao.city());
    assertEquals("Finland", dao.country());
    assertEquals("00100", dao.postalCode());
    assertEquals(50, dao.radiusKm());
  }

  @Test
  void testToDao_WithNullEntity_ShouldReturnNull() {
    // Act
    BookLocationDao dao = BookLocationMapper.toDao(null);

    // Assert
    assertNull(dao);
  }

  @Test
  void testToEntity_WithValidDao_ShouldMapCorrectly() {
    // Arrange
    var dao = BookLocationDao.builder()
        .latitude(60.1699)
        .longitude(24.9384)
        .address("Mannerheimintie 12")
        .city("Helsinki")
        .country("Finland")
        .postalCode("00100")
        .radiusKm(50)
        .build();

    // Act
    var entity = BookLocationMapper.toEntity(dao);

    // Assert
    assertNotNull(entity);
    assertEquals(60.1699, entity.latitude());
    assertEquals(24.9384, entity.longitude());
    assertEquals("Mannerheimintie 12", entity.address());
    assertEquals("Helsinki", entity.city());
    assertEquals("Finland", entity.country());
    assertEquals("00100", entity.postalCode());
    assertEquals(50, entity.radiusKm());
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
    BookLocation location = BookLocation.builder().build();

    // Test hasCoordinates - should be false initially
    assertFalse(location.hasCoordinates());

    // Set valid coordinates
    location = location.withLatitude(60.1699).withLongitude(24.9384);
    assertTrue(location.hasCoordinates());

    // Test hasAddress - should be false initially
    assertFalse(location.hasAddress());

    // Set address
    location = location.withAddress("Mannerheimintie 12");
    assertTrue(location.hasAddress());

    // Test with empty address
    location = location.withAddress("");
    assertFalse(location.hasAddress());

    // Test with whitespace-only address
    location = location.withAddress("   ");
    assertFalse(location.hasAddress());
  }

  @Test
  public void testBookLocation_CoordinateValidation() {
    // Test valid coordinates
    assertTrue(BookLocation.isValidLatitude(60.1699));
    assertTrue(BookLocation.isValidLatitude(-60.1699));
    assertTrue(BookLocation.isValidLatitude(0.0));
    assertTrue(BookLocation.isValidLatitude(85.0));
    assertTrue(BookLocation.isValidLatitude(-85.0));

    assertTrue(BookLocation.isValidLongitude(24.9384));
    assertTrue(BookLocation.isValidLongitude(-24.9384));
    assertTrue(BookLocation.isValidLongitude(0.0));
    assertTrue(BookLocation.isValidLongitude(180.0));
    assertTrue(BookLocation.isValidLongitude(-180.0));

    // Test invalid coordinates
    assertFalse(BookLocation.isValidLatitude(null));
    assertFalse(BookLocation.isValidLatitude(90.0));
    assertFalse(BookLocation.isValidLatitude(-90.0));
    assertFalse(BookLocation.isValidLatitude(100.0));

    assertFalse(BookLocation.isValidLongitude(null));
    assertFalse(BookLocation.isValidLongitude(181.0));
    assertFalse(BookLocation.isValidLongitude(-181.0));
    assertFalse(BookLocation.isValidLongitude(200.0));

    // Test hasCoordinates with invalid coordinates
    var location = BookLocation.builder()
        .latitude(90.0) // Invalid - too close to pole
        .longitude(24.9384)
        .build();
    assertFalse(location.hasCoordinates());

    location = location
        .withLatitude(60.1699)
        .withLongitude(181.0); // Invalid - out of range
    assertFalse(location.hasCoordinates());
  }
}
