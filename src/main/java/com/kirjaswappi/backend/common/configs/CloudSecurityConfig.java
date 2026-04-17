/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.configs;

import static com.kirjaswappi.backend.common.configs.CloudSecurityConfig.Scopes.ADMIN;
import static com.kirjaswappi.backend.common.configs.CloudSecurityConfig.Scopes.USER;
import static com.kirjaswappi.backend.common.utils.Constants.*;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.kirjaswappi.backend.common.components.FilterApiRequest;

@Configuration
@EnableWebSecurity
@Profile("cloud")
public class CloudSecurityConfig {

  private static final Logger logger = LoggerFactory.getLogger(CloudSecurityConfig.class);

  @Value("${cors.allowed-origins:https://kirjaswappi.fi,https://www.kirjaswappi.fi,https://canary.kirjaswappi.fi}")
  private String[] allowedOrigins;

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of(allowedOrigins));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    configuration.setAllowCredentials(true);
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-User-Id", "Accept"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public SecurityFilterChain defaultSecurityFilterChain(FilterApiRequest filterApiRequest, HttpSecurity http)
      throws Exception {
    return http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(csrf -> csrf.disable()) // Disable CSRF protection
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(HEALTH, API_BASE + AUTHENTICATE,
                API_BASE + AUTHENTICATE + REFRESH, "/ws/**")
            .permitAll()
            // User auth endpoints (no JWT required)
            .requestMatchers(POST, API_BASE + USERS + LOGIN).permitAll()
            .requestMatchers(POST, API_BASE + USERS + LOGIN_WITH_GOOGLE).permitAll()
            .requestMatchers(POST, API_BASE + USERS + SIGNUP).permitAll()
            .requestMatchers(POST, API_BASE + USERS + VERIFY_EMAIL).permitAll()
            .requestMatchers(POST, API_BASE + USERS + "/refresh-token").permitAll()
            .requestMatchers(POST, API_BASE + SEND_OTP).permitAll()
            .requestMatchers(POST, API_BASE + VERIFY_OTP).permitAll()
            // Public form submissions (contact, feedback, etc.) — no auth required
            .requestMatchers(POST, API_BASE + FORMS + "/**").permitAll()
            // Public read-only endpoints (no auth required)
            .requestMatchers(GET, API_BASE + BOOKS + "/**").permitAll()
            .requestMatchers(GET, API_BASE + GENRES).permitAll()
            .requestMatchers(GET, API_BASE + CITIES).permitAll()
            .requestMatchers(GET, API_BASE + USERS + ID).permitAll()
            .requestMatchers(GET, API_BASE + USERS + ID + BOOKS).permitAll()
            .requestMatchers(GET, API_BASE + PHOTOS + PROFILE_PHOTO + BY_ID + "/**").permitAll()
            .requestMatchers(GET, API_BASE + PHOTOS + COVER_PHOTO + BY_ID + "/**").permitAll()
            .requestMatchers(GET, API_BASE + PHOTOS + SUPPORTED_COVER_PHOTOS).permitAll()
            // Password reset (unauthenticated by nature)
            .requestMatchers(POST, API_BASE + USERS + RESET_PASSWORD + "/**").permitAll()
            .requestMatchers(POST, API_BASE + ADMIN_USERS).hasAuthority(ADMIN)
            .requestMatchers(GET, API_BASE + ADMIN_USERS).hasAnyAuthority(ADMIN, USER)
            .requestMatchers(DELETE, API_BASE + ADMIN_USERS + USERNAME).hasAuthority(ADMIN)
            .requestMatchers(DELETE, API_BASE + SWAP_REQUESTS).hasAuthority(ADMIN)
            .requestMatchers(DELETE, API_BASE + BOOKS).hasAuthority(ADMIN)
            .requestMatchers(API_DOCS, SWAGGER_UI, "/error").permitAll()
            .anyRequest().hasAnyAuthority(ADMIN, USER))
        .addFilterBefore(filterApiRequest, UsernamePasswordAuthenticationFilter.class)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .exceptionHandling(ex -> ex
            .accessDeniedHandler((request, response, accessDeniedException) -> {
              logger.warn("Access denied: {} {} — auth: {}, reason: {}",
                  request.getMethod(), request.getRequestURI(),
                  request.getHeader("Authorization") != null ? "present" : "missing",
                  accessDeniedException.getMessage());
              response.setStatus(HttpStatus.FORBIDDEN.value());
            })
            .authenticationEntryPoint((request, response, authException) -> {
              logger.warn("Unauthenticated: {} {} — reason: {}",
                  request.getMethod(), request.getRequestURI(),
                  authException.getMessage());
              response.setStatus(HttpStatus.UNAUTHORIZED.value());
            }))
        .build();
  }

  static class Scopes {
    public static final String ADMIN = "ADMIN";
    public static final String USER = "USER";
  }
}
