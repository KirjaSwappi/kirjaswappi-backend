/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.configs;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.kirjaswappi.backend.common.service.AdminUserService;
import com.kirjaswappi.backend.common.service.entities.AdminUser;
import com.kirjaswappi.backend.common.utils.JwtUtil;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

  @Autowired
  private JwtUtil jwtUtil;

  @Autowired
  private AdminUserService adminUserService;

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(new ChannelInterceptor() {
      @Override
      public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
          // Extract JWT token from Authorization header (validates platform/client)
          String authHeader = accessor.getFirstNativeHeader("Authorization");
          if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header with Bearer token is required");
          }

          String jwt = authHeader.substring(7);

          // Extract userId from connection headers (identifies end user)
          String userId = accessor.getFirstNativeHeader("userId");
          if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("userId header is required for WebSocket connection");
          }

          try {
            // Validate JWT token (platform authentication)
            String username = jwtUtil.extractUsername(jwt);
            AdminUser adminUser = adminUserService.getAdminUserInfo(username);

            if (!jwtUtil.validateJwtToken(jwt, adminUser)) {
              throw new IllegalArgumentException("Invalid JWT token");
            }

            // Extract role from JWT
            String role = jwtUtil.extractRole(jwt);
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));

            // Create authentication with userId as principal (for chat routing)
            // The userId identifies which end user is connecting
            Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            accessor.setUser(auth);

          } catch (Exception e) {
            throw new IllegalArgumentException("WebSocket authentication failed: " + e.getMessage(), e);
          }
        }

        return message;
      }
    });
  }
}
