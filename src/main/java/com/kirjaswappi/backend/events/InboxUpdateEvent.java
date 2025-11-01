/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.events;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain event that represents an inbox update for a user. This event is
 * published when inbox-related changes occur that require real-time
 * notifications.
 */
public class InboxUpdateEvent {
  private final String userId;
  private final String swapRequestId;
  private final String eventType;
  private final Instant timestamp;

  public InboxUpdateEvent(String userId, String swapRequestId, String eventType) {
    this.userId = Objects.requireNonNull(userId, "userId cannot be null");
    this.swapRequestId = Objects.requireNonNull(swapRequestId, "swapRequestId cannot be null");
    this.eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
    this.timestamp = Instant.now();
  }

  public String getUserId() {
    return userId;
  }

  public String getSwapRequestId() {
    return swapRequestId;
  }

  public String getEventType() {
    return eventType;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    InboxUpdateEvent that = (InboxUpdateEvent) o;
    return Objects.equals(userId, that.userId) &&
        Objects.equals(swapRequestId, that.swapRequestId) &&
        Objects.equals(eventType, that.eventType) &&
        Objects.equals(timestamp, that.timestamp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, swapRequestId, eventType, timestamp);
  }

  @Override
  public String toString() {
    return "InboxUpdateEvent{" +
        "userId='" + userId + '\'' +
        ", swapRequestId='" + swapRequestId + '\'' +
        ", eventType='" + eventType + '\'' +
        ", timestamp=" + timestamp +
        '}';
  }

  // Event type constants
  public static final String STATUS_CHANGE = "STATUS_CHANGE";
  public static final String NEW_MESSAGE = "NEW_MESSAGE";
}
