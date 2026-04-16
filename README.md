[![Main](https://github.com/kirjaswappi/kirjaswappi-backend/actions/workflows/main.yml/badge.svg?branch=main)](https://github.com/kirjaswappi/kirjaswappi-backend/actions/workflows/main.yml)

# KirjaSwappi Backend

REST API for [KirjaSwappi](https://kirjaswappi.fi) — a book exchange platform. Built with Java 25 and Spring Boot 4.0.

## Tech Stack

- Java 25 + Spring Boot 4.0
- MongoDB + Spring Data
- Redis (caching)
- RabbitMQ (WebSocket/STOMP messaging)
- MinIO (S3-compatible photo storage)
- gRPC (notification service integration)

## Getting Started

**Prerequisites:** JDK 25

```bash
# Build and run tests
mvn clean package

# Run locally
mvn spring-boot:run

# Format code
mvn spotless:apply
```

Configure via `src/main/resources/application-local.yaml` for local development.

## API Documentation

[Swagger UI](https://api.kirjaswappi.fi/swagger-ui/index.html)

## Links

- **Production API:** <https://api.kirjaswappi.fi>
- **Frontend:** <https://kirjaswappi.fi>

© 2025 KirjaSwappi. All rights reserved.
