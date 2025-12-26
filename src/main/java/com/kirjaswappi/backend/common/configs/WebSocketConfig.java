/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.configs;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final Environment env;

  public WebSocketConfig(Environment env) {
    this.env = env;
  }

  @org.springframework.beans.factory.annotation.Value("${spring.rabbitmq.host:localhost}")
  private String relayHost;

  @org.springframework.beans.factory.annotation.Value("${spring.rabbitmq.stomp-port:61613}")
  private int relayPort;

  @org.springframework.beans.factory.annotation.Value("${spring.rabbitmq.username:guest}")
  private String relayUser;

  @org.springframework.beans.factory.annotation.Value("${spring.rabbitmq.password:guest}")
  private String relayPass;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    List<String> activeProfiles = Arrays.asList(env.getActiveProfiles());

    if (activeProfiles.contains("cloud") || activeProfiles.contains("prod")) {
      // Use external RabbitMQ broker for horizontal scaling in production
      config.enableStompBrokerRelay("/topic", "/queue")
          .setRelayHost(relayHost)
          .setRelayPort(relayPort)
          .setClientLogin(relayUser)
          .setClientPasscode(relayPass)
          .setSystemLogin(relayUser)
          .setSystemPasscode(relayPass);
    } else {
      // Use in-memory broker for local development and testing
      config.enableSimpleBroker("/topic", "/queue");
    }

    // Set application destination prefix
    config.setApplicationDestinationPrefixes("/app");
    // Set user destination prefix for private messages
    config.setUserDestinationPrefix("/user");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    // Register STOMP endpoint with SockJS fallback
    registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("${FRONTEND_URL:*}")
        .withSockJS();
  }
}
