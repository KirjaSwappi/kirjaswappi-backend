package com.kirjaswappi.backend.http.dtos.requests;

/**
 * DTO for Google login requests.
 * <p>
 * Schema:
 * {
 *   "idToken": "string (Google ID token)"
 * }
 * </p>
 */
public record LoginWithGoogleRequest(String idToken) {
}
