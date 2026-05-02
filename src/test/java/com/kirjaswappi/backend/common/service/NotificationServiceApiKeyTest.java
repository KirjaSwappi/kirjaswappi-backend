/*
 * Copyright (c) 2026 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicReference;

import io.grpc.CallCredentials;
import io.grpc.Metadata;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contract test for the API-key metadata that the backend's gRPC client must
 * attach when calling the Go notification service.
 *
 * <p>
 * This used to be a silent launch blocker: the notification service rejects
 * calls without {@code x-api-key} when {@code API_KEY} is set, but the Java
 * client previously issued un-tagged requests and relied on the (then-empty)
 * server-side default to "succeed".
 */
class NotificationServiceApiKeyTest {

  private static final Metadata.Key<String> API_KEY = Metadata.Key.of("x-api-key",
      Metadata.ASCII_STRING_MARSHALLER);

  private static CallCredentials invokeFactory(String key) {
    return NotificationService.apiKeyCallCredentials(key);
  }

  @Test
  @DisplayName("apiKeyCallCredentials applies x-api-key metadata header")
  void appliesApiKeyHeader() throws Exception {
    CallCredentials creds = invokeFactory("super-secret");
    AtomicReference<Metadata> applied = new AtomicReference<>();

    creds.applyRequestMetadata(null, Runnable::run, new CallCredentials.MetadataApplier() {
      @Override
      public void apply(Metadata headers) {
        applied.set(headers);
      }

      @Override
      public void fail(io.grpc.Status status) {
        throw new AssertionError("Should not fail: " + status);
      }
    });

    Metadata headers = applied.get();
    assertNotNull(headers, "MetadataApplier must be invoked synchronously");
    assertEquals("super-secret", headers.get(API_KEY),
        "x-api-key metadata must be present and equal to the configured key");
  }

  @Test
  @DisplayName("Empty API key still produces credentials but no metadata leaks unrelated headers")
  void differentKeysDoNotLeakAcross() throws Exception {
    CallCredentials a = invokeFactory("alpha");
    CallCredentials b = invokeFactory("bravo");
    AtomicReference<Metadata> ha = new AtomicReference<>();
    AtomicReference<Metadata> hb = new AtomicReference<>();

    a.applyRequestMetadata(null, Runnable::run, new CallCredentials.MetadataApplier() {
      @Override
      public void apply(Metadata headers) {
        ha.set(headers);
      }

      @Override
      public void fail(io.grpc.Status status) {
        throw new AssertionError(status.toString());
      }
    });
    b.applyRequestMetadata(null, Runnable::run, new CallCredentials.MetadataApplier() {
      @Override
      public void apply(Metadata headers) {
        hb.set(headers);
      }

      @Override
      public void fail(io.grpc.Status status) {
        throw new AssertionError(status.toString());
      }
    });

    assertEquals("alpha", ha.get().get(API_KEY));
    assertEquals("bravo", hb.get().get(API_KEY));
    assertNull(ha.get().get(Metadata.Key.of("x-other-key", Metadata.ASCII_STRING_MARSHALLER)));
  }
}
