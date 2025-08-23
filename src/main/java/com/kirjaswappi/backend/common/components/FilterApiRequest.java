/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.components;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirjaswappi.backend.common.exceptions.InvalidJwtTokenException;
import com.kirjaswappi.backend.common.http.ErrorUtils;
import com.kirjaswappi.backend.common.service.AdminUserService;
import com.kirjaswappi.backend.common.service.entities.AdminUser;
import com.kirjaswappi.backend.common.utils.JwtUtil;

@Component
@Profile("cloud")
public class FilterApiRequest extends OncePerRequestFilter {
  private static final Logger logger = LoggerFactory.getLogger(FilterApiRequest.class);

  @Autowired
  private AdminUserService adminUserService;
  @Autowired
  private ErrorUtils errorUtils;
  @Autowired
  private JwtUtil jwtUtil;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      // 1. Extract Token
      String jwt = JwtUtil.extractJwtToken(request);

      // 2. Early Exit if No Token
      if (jwt == null || SecurityContextHolder.getContext().getAuthentication() != null) {
        logger.warn("No JWT Token found in request");
        filterChain.doFilter(request, response);
        return;
      }

      // 3. Validate Token and User
      validateTokenAndUser(request, jwt);

      // 4. Continue Filter Chain
      filterChain.doFilter(request, response);
    } catch (InvalidJwtTokenException e) {
      sendInvalidJwtTokenExceptionResponse(response, e);
    }
  }

  private void sendInvalidJwtTokenExceptionResponse(HttpServletResponse response, InvalidJwtTokenException e)
      throws IOException {
    var errorResponse = errorUtils.buildErrorResponse(e);
    ObjectMapper objectMapper = new ObjectMapper();
    String jsonResponse = objectMapper.writeValueAsString(errorResponse.error());
    response.setHeader("Content-Type", "application/json");
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.getWriter().write(jsonResponse);
  }

  private void validateTokenAndUser(HttpServletRequest request, String jwt) {
    try {
      String username = jwtUtil.extractUsername(jwt);
      AdminUser userDetails = adminUserService.getAdminUserInfo(username);
      if (jwtUtil.validateJwtToken(jwt, userDetails))
        setAuthentication(request, jwt, userDetails);
    } catch (MalformedJwtException | ExpiredJwtException | AuthenticationException e) {
      logger.warn(e.getMessage());
      SecurityContextHolder.clearContext();
      throw new InvalidJwtTokenException(e.getMessage());
    }
  }

  private void setAuthentication(HttpServletRequest request, String jwt, AdminUser userDetails) {
    SecurityContextHolder.getContext().setAuthentication(
        createAuthenticationToken(jwt, userDetails, request));
  }

  private UsernamePasswordAuthenticationToken createAuthenticationToken(String jwt, AdminUser userDetails,
      HttpServletRequest request) {
    String role = jwtUtil.extractRole(jwt);
    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
        userDetails, null, authorities);
    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    return authToken;
  }

}
