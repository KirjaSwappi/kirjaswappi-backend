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

import com.kirjaswappi.backend.common.utils.JwtUtil;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

  @Autowired
  private JwtUtil jwtUtil;

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(new ChannelInterceptor() {
      @Override
      public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
          // Extract JWT token from Authorization header
          String authHeader = accessor.getFirstNativeHeader("Authorization");
          if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header with Bearer token is required");
          }

          String jwt = authHeader.substring(7);

          try {
            // Only end-user JWTs may connect to the application WebSocket.
            // Admin JWTs are not allowed: principals on STOMP destinations
            // are derived from the token subject, so trusting a client-supplied
            // userId header from an admin token enables impersonation of any user.
            if (!jwtUtil.isUserToken(jwt)) {
              throw new IllegalArgumentException("Admin JWT cannot be used for application WebSocket");
            }
            if (!jwtUtil.validateUserToken(jwt)) {
              throw new IllegalArgumentException("Invalid user JWT token");
            }
            String userId = jwtUtil.extractUserId(jwt);
            String role = jwtUtil.extractRole(jwt);
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
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
