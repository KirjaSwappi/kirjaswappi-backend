/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.migrations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.kirjaswappi.backend.jpa.daos.CityDao;
import com.mongodb.client.MongoCollection;

class InsertFinlandCitiesTest {

  @Mock
  private MongoTemplate mongoTemplate;

  @Mock
  private MongoCollection mongoCollection;

  private InsertFinlandCities migration;

  @BeforeEach
  @DisplayName("Setup mocks and migration instance")
  void setUp() {
    MockitoAnnotations.openMocks(this);
    migration = new InsertFinlandCities(mongoTemplate);
  }

  @Test
  @DisplayName("Should insert all Finland cities when collection is empty")
  void shouldInsertAllFinlandCitiesWhenCollectionIsEmpty() {
    // Given
    when(mongoTemplate.getCollection("cities")).thenReturn(mongoCollection);
    when(mongoCollection.countDocuments()).thenReturn(0L);

    // When
    migration.executeMigration();

    // Then
    ArgumentCaptor<List<CityDao>> captor = ArgumentCaptor.forClass(List.class);
    verify(mongoTemplate).insertAll(captor.capture());

    List<CityDao> insertedCities = captor.getValue();
    assertEquals(30, insertedCities.size());

    // Verify some specific cities
    assertTrue(insertedCities.stream().anyMatch(city -> "Helsinki".equals(city.name())));
    assertTrue(insertedCities.stream().anyMatch(city -> "Tampere".equals(city.name())));
    assertTrue(insertedCities.stream().anyMatch(city -> "Turku".equals(city.name())));
    assertTrue(insertedCities.stream().anyMatch(city -> "Oulu".equals(city.name())));
  }

  @Test
  @DisplayName("Should not insert cities when collection is not empty")
  void shouldNotInsertCitiesWhenCollectionIsNotEmpty() {
    // Given
    when(mongoTemplate.getCollection("cities")).thenReturn(mongoCollection);
    when(mongoCollection.countDocuments()).thenReturn(5L);

    // When
    migration.executeMigration();

    // Then
    verify(mongoTemplate, never()).insertAll(any());
  }

  @Test
  @DisplayName("Should rollback by dropping cities collection")
  void shouldRollbackByDroppingCitiesCollection() {
    // When
    migration.rollback();

    // Then
    verify(mongoTemplate).dropCollection("cities");
  }
}
