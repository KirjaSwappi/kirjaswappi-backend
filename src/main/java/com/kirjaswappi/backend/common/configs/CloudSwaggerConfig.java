/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.configs;

import static com.kirjaswappi.backend.common.utils.Constants.APP_NAME;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("cloud")
public class CloudSwaggerConfig {
  @Bean
  public GroupedOpenApi publicApis() {
    return GroupedOpenApi.builder()
        .group("Public APIs")
        .pathsToMatch("/api/public/**")
        .build();
  }

  @Bean
  public GroupedOpenApi internalApis() {
    return GroupedOpenApi.builder()
        .group("Internal APIs")
        .pathsToExclude("/api/public/**")
        .build();
  }

  @Bean
  public OpenAPI openAPI() {
    final String securitySchemeName = "bearerAuth";
    return new OpenAPI().info(new Info().title(APP_NAME)
        .description("This API documentation enables you to perform operations on KirjaSwappi platform.")
        .version("v1.0.0"))
        // the following security scheme definition is needed for the API access
        // validation
        .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
        .components(new Components()
            .addSecuritySchemes(securitySchemeName,
                new SecurityScheme()
                    .bearerFormat("JWT")
                    .description("This security scheme definition is needed for API validation.")
                    .scheme("bearer")
                    .type(SecurityScheme.Type.HTTP)));
  }
}
