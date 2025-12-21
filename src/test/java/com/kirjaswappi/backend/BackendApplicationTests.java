/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.kirjaswappi.backend.config.TestContainersConfig;
import com.kirjaswappi.backend.config.TestMailConfig;

@SpringBootTest(classes = { BackendApplication.class, TestContainersConfig.class, TestMailConfig.class })
@ActiveProfiles("test")
class BackendApplicationTests {

  @Test
  void contextLoads() {
  }

}
