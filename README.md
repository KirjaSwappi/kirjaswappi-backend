# KirjaSwappi Backend

[![CI](https://github.com/KirjaSwappi/kirjaswappi-backend/actions/workflows/main.yml/badge.svg)](https://github.com/KirjaSwappi/kirjaswappi-backend/actions/workflows/main.yml)
[![CodeQL](https://github.com/KirjaSwappi/kirjaswappi-backend/actions/workflows/github-code-scanning/codeql/badge.svg)](https://github.com/KirjaSwappi/kirjaswappi-backend/security/code-scanning)

REST API for [KirjaSwappi](https://kirjaswappi.fi) — a Finnish book exchange platform where users list books, negotiate swaps, and communicate in real time.

## Tech Stack

| Layer | Technology |
| ----- | ---------- |
| Language | Java 25 |
| Framework | Spring Boot 4.0 |
| Primary database | MongoDB + Spring Data |
| Cache | Redis |
| Messaging | RabbitMQ (STOMP / WebSocket relay) |
| Object storage | MinIO (S3-compatible) |
| Notification | gRPC (kirjaswappi-notification service) |
| Security | Spring Security, JWT, TOTP 2FA |
| Feature flags | Unleash |

## Getting Started

**Prerequisites:** JDK 25, Docker

```bash
git clone https://github.com/KirjaSwappi/kirjaswappi-backend.git
cd kirjaswappi-backend

# Copy and configure local settings
cp src/main/resources/application-local.yaml.example src/main/resources/application-local.yaml

# Start backing services (MongoDB, Redis, RabbitMQ, MinIO)
docker compose up -d

# Build and run
mvn clean package
mvn spring-boot:run
```

## Commands

| Command | Description |
| ------- | ----------- |
| `mvn spring-boot:run` | Start the API locally |
| `mvn test` | Run all tests (requires Docker for TestContainers) |
| `mvn spotless:apply` | Format code |
| `mvn clean package` | Build JAR |

## API Documentation

- Local: `http://localhost:8080/swagger-ui/index.html`
- Production: [api.kirjaswappi.fi/swagger-ui/index.html](https://api.kirjaswappi.fi/swagger-ui/index.html)

## Architecture

```text
HTTP client
    │
    ▼
Spring Security (JWT + 2FA)
    │
    ▼
Controllers → Services → Repositories (MongoDB)
                │
                ├── Redis (cache, sessions, rate limiting)
                ├── MinIO (photo storage)
                ├── RabbitMQ (STOMP WebSocket relay)
                └── gRPC → kirjaswappi-notification
```

## Related Repositories

| Repo | Description |
| ---- | ----------- |
| [kirjaswappi-frontend](https://github.com/KirjaSwappi/kirjaswappi-frontend) | React TypeScript SPA |
| [kirjaswappi-notification](https://github.com/KirjaSwappi/kirjaswappi-notification) | Go notification service |
| [kirjaswappi-infra](https://github.com/KirjaSwappi/kirjaswappi-infra) | Infrastructure & deployment |

## Links

- **Production API:** <https://api.kirjaswappi.fi>
- **Web app:** <https://kirjaswappi.fi>

---

© 2024–2026 KirjaSwappi. All rights reserved. See [LICENSE](LICENSE) for terms.
