[![Main](https://github.com/kirjaswappi/kirjaswappi-backend/actions/workflows/main.yml/badge.svg?branch=main)](https://github.com/kirjaswappi/kirjaswappi-backend/actions/workflows/main.yml)

# KirjaSwappi Backend Service

This repository contains the `kirjaswappi backend` service for the KirjaSwappi Platform. The KirjaSwappi backend service is a [Spring Boot](https://spring.io/projects/spring-boot) Application.

## Local Development Setup

To start developing, you first need to set up your local machine with JDK 25.

To build the application and run all tests, execute

```console
mvn clean package
```

To run the application, you can run

```console
mvn spring-boot:run
```

To format the source code, you can run

```console
mvn spotless:apply
```

## API documentation

API documentation is available on: [Swagger](https://api.kirjaswappi.fi/swagger-ui/index.html) and [Bump](https://bump.sh/kirjaswappi/doc/backend-api).
