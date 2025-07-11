/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.requests;

/**
 * DTO for Google login requests.
 * <p>
 * Schema: { "idToken": "string (Google ID token)" }
 * </p>
 */
public record LoginWithGoogleRequest(String idToken) {
}
