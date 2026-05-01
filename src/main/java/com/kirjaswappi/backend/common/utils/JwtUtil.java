/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.utils;

import static com.kirjaswappi.backend.common.utils.Constants.ROLE;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.SecretKey;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.kirjaswappi.backend.common.service.entities.AdminUser;

@Component
public class JwtUtil {
  private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

  @Value("${jwt.secret}")
  private String SECRET_STRING;
  @Value("${jwt.expiration}")
  private long TOKEN_EXPIRATION_MS;
  @Value("${jwt.refresh-expiration:604800000}")
  private long REFRESH_TOKEN_EXPIRATION_MS; // default 7 days

  @Autowired(required = false)
  private StringRedisTemplate redisTemplate;

  private SecretKey SECRET_KEY;

  private static final String RESET_TOKEN_USED_PREFIX = "jwt:reset:used:";
  private static final String REFRESH_TOKEN_REVOKED_PREFIX = "jwt:refresh:revoked:";

  @PostConstruct
  public void init() {
    byte[] SECRET_KEY_BYTES = SECRET_STRING.getBytes(StandardCharsets.UTF_8);
    SECRET_KEY = Keys.hmacShaKeyFor(SECRET_KEY_BYTES);
  }

  private static final String TOKEN_TYPE = "jwtToken";
  private static final String USER_TOKEN_TYPE = "userToken";
  private static final String TOKEN_PURPOSE = "tokenPurpose";
  private static final String ACCESS_PURPOSE = "access";
  private static final String REFRESH_PURPOSE = "refresh";
  private static final String EMAIL_CLAIM = "email";

  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  private Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser()
        .verifyWith(SECRET_KEY)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  private boolean isTokenValid(String token) {
    return !extractExpiration(token)
        .before(new Date());
  }

  private boolean isValidUser(String token, AdminUser adminUser) {
    final String username = extractUsername(token);
    return username.equals(adminUser.username());
  }

  public String generateJwtToken(AdminUser adminUser) {
    Map<String, Object> claims = new HashMap<>();
    claims.put(ROLE, adminUser.role());
    claims.put(TOKEN_TYPE, true);
    return createJwtToken(claims, adminUser.username());
  }

  private String createJwtToken(Map<String, Object> claims, String subject) {
    return Jwts.builder()
        .claims(claims)
        .subject(subject)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + TOKEN_EXPIRATION_MS))
        .signWith(SECRET_KEY)
        .compact();
  }

  public boolean validateJwtToken(String token, AdminUser adminUser) {
    return isValidUser(token, adminUser) && isTokenValid(token) && isValidTokenType(token);
  }

  public boolean validateRefreshToken(String token, AdminUser adminUser) {
    return isValidUser(token, adminUser) && isTokenValid(token) && !isValidTokenType(token);
  }

  private boolean isValidTokenType(String token) {
    Claims claims = extractAllClaims(token);
    return claims.get(TOKEN_TYPE, Boolean.class);
  }

  public String extractRole(String token) {
    Claims claims = extractAllClaims(token);
    return claims.get(ROLE, String.class);
  }

  public String generateRefreshToken(AdminUser adminUser) {
    Map<String, Object> claims = new HashMap<>();
    claims.put(ROLE, adminUser.role());
    claims.put(TOKEN_TYPE, false);
    return createRefreshToken(claims, adminUser.username());
  }

  private String createRefreshToken(Map<String, Object> claims, String subject) {
    return Jwts.builder()
        .claims(claims)
        .subject(subject)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION_MS))
        .signWith(SECRET_KEY)
        .compact();
  }

  public static String extractJwtToken(HttpServletRequest request) {
    final String authorizationHeader = request.getHeader("Authorization");
    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
      return authorizationHeader.substring(7);
    }
    return null;
  }

  // =========== User Token Methods ===========

  public String generateUserToken(String userId, String email) {
    Map<String, Object> claims = new HashMap<>();
    claims.put(ROLE, "USER");
    claims.put(TOKEN_TYPE, USER_TOKEN_TYPE);
    claims.put(TOKEN_PURPOSE, ACCESS_PURPOSE);
    claims.put(EMAIL_CLAIM, email);
    return Jwts.builder()
        .claims(claims)
        .subject(userId)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + TOKEN_EXPIRATION_MS))
        .signWith(SECRET_KEY)
        .compact();
  }

  public String generateUserRefreshToken(String userId, String email) {
    Map<String, Object> claims = new HashMap<>();
    claims.put(ROLE, "USER");
    claims.put(TOKEN_TYPE, USER_TOKEN_TYPE);
    claims.put(TOKEN_PURPOSE, REFRESH_PURPOSE);
    claims.put(EMAIL_CLAIM, email);
    return Jwts.builder()
        .claims(claims)
        .id(UUID.randomUUID().toString())
        .subject(userId)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION_MS))
        .signWith(SECRET_KEY)
        .compact();
  }

  /**
   * Adds the refresh token's jti to a Redis denylist so it cannot be used to mint
   * new access tokens. Used on logout and password change.
   */
  public void revokeUserRefreshToken(String token) {
    if (redisTemplate == null) {
      return;
    }
    try {
      Claims claims = extractAllClaims(token);
      String jti = claims.getId();
      if (jti == null || jti.isBlank()) {
        return;
      }
      Date exp = claims.getExpiration();
      Duration ttl = exp != null
          ? Duration.between(Instant.now(), exp.toInstant()).plusMinutes(1)
          : Duration.ofDays(7).plusMinutes(1);
      if (ttl.isNegative() || ttl.isZero()) {
        ttl = Duration.ofMinutes(1);
      }
      redisTemplate.opsForValue().set(REFRESH_TOKEN_REVOKED_PREFIX + jti, "1", ttl);
    } catch (Exception e) {
      logger.warn("Failed to revoke refresh token: {}", e.getMessage());
    }
  }

  private boolean isRefreshTokenRevoked(String token) {
    if (redisTemplate == null) {
      return false;
    }
    try {
      String jti = extractAllClaims(token).getId();
      if (jti == null || jti.isBlank()) {
        return false;
      }
      return Boolean.TRUE.equals(redisTemplate.hasKey(REFRESH_TOKEN_REVOKED_PREFIX + jti));
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isUserToken(String token) {
    Claims claims = extractAllClaims(token);
    Object type = claims.get(TOKEN_TYPE);
    return type instanceof String && USER_TOKEN_TYPE.equals(type);
  }

  public boolean validateUserToken(String token) {
    if (!isUserToken(token) || !isTokenValid(token))
      return false;
    Claims claims = extractAllClaims(token);
    return ACCESS_PURPOSE.equals(claims.get(TOKEN_PURPOSE));
  }

  public boolean validateUserRefreshToken(String token) {
    if (!isUserToken(token) || !isTokenValid(token))
      return false;
    Claims claims = extractAllClaims(token);
    if (!REFRESH_PURPOSE.equals(claims.get(TOKEN_PURPOSE))) {
      return false;
    }
    return !isRefreshTokenRevoked(token);
  }

  public String extractUserId(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  // =========== Password Reset Token Methods ===========

  private static final String RESET_TOKEN_PURPOSE = "passwordReset";
  private static final long RESET_TOKEN_EXPIRATION_MS = 900000; // 15 minutes

  public String generatePasswordResetToken(String email) {
    Map<String, Object> claims = new HashMap<>();
    claims.put(TOKEN_PURPOSE, RESET_TOKEN_PURPOSE);
    return Jwts.builder()
        .claims(claims)
        .id(UUID.randomUUID().toString())
        .subject(email)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + RESET_TOKEN_EXPIRATION_MS))
        .signWith(SECRET_KEY)
        .compact();
  }

  public boolean validatePasswordResetToken(String token) {
    try {
      Claims claims = extractAllClaims(token);
      if (!RESET_TOKEN_PURPOSE.equals(claims.get(TOKEN_PURPOSE)) || !isTokenValid(token)) {
        return false;
      }
      // Reject if jti is already in the consumed set.
      if (redisTemplate != null) {
        String jti = claims.getId();
        if (jti != null && Boolean.TRUE.equals(redisTemplate.hasKey(RESET_TOKEN_USED_PREFIX + jti))) {
          return false;
        }
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Mark the reset token's jti as consumed so it cannot be replayed within its
   * expiry window. Stores until the JWT exp + a small buffer.
   */
  public void consumePasswordResetToken(String token) {
    if (redisTemplate == null) {
      return; // best-effort when Redis is unavailable
    }
    try {
      Claims claims = extractAllClaims(token);
      String jti = claims.getId();
      if (jti == null || jti.isBlank()) {
        return;
      }
      Date exp = claims.getExpiration();
      Duration ttl = exp != null
          ? Duration.between(Instant.now(), exp.toInstant()).plusMinutes(1)
          : Duration.ofMinutes(20);
      if (ttl.isNegative() || ttl.isZero()) {
        ttl = Duration.ofMinutes(1);
      }
      redisTemplate.opsForValue().set(RESET_TOKEN_USED_PREFIX + jti, "1", ttl);
    } catch (Exception e) {
      logger.warn("Failed to mark reset token as consumed: {}", e.getMessage());
    }
  }

  public String extractEmailFromResetToken(String token) {
    return extractClaim(token, Claims::getSubject);
  }
}
