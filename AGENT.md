# agent.md

## 1. Purpose of This File
This file is the **authoritative source of truth** for all AI agents working on the `kirjaswappi-backend` repository. It documents the mandatory architectural rules, coding conventions, and patterns that MUST be followed. Any code generated or modified by an agent MUST strictly adhere to these guidelines.

## 2. Repository Overview
- **Architecture**: Layered Monolith with strict separation of concerns (API -> Service -> Persistence).
- **Core Technologies**:
    - **Language**: Java 25 (uses `var`, Records, Pattern Matching).
    - **Framework**: Spring Boot 4.0.2.
    - **Database**: MongoDB (using explicit `*Dao` documents).
    - **Migration**: Mongock.
    - **Caching**: Redis.
    - **Communication**: gRPC, WebSocket.
    - **Security**: Spring Security + JWT.
    - **Infrastructure**: Testcontainers, MinIO.
- **Design Philosophy**:
    - **Immutability**: Domain entities are Java `record`s.
    - **Explict Mapping**: Manual mapping between layers (DTO <-> Domain <-> DAO).
    - **Type Safety**: Strong usage of Jakarta Validation.

## 3. Architectural Rules
### Layer Responsibilities
1.  **API Layer (`http`)**:
    -   Handles HTTP requests/responses.
    -   Accepts **Request DTOs**, returns **Response DTOs**.
    -   **NEVER** exposes Domain Entities (`service.entities`) or DAOs (`jpa.daos`) directly.
2.  **Service Layer (`service`)**:
    -   Contains all business logic.
    -   Operates on **Domain Entities** (Java Records).
    -   Transactional boundary (`@Transactional`).
    -   Orchestrates data flow between Repositories and external clients (`NotificationClient`).
3.  **Persistence Layer (`jpa`)**:
    -   Handles database interactions.
    -   Operates on **DAOs** (Mongo Documents annotated with `@Document`).
    -   Exposes Spring Data Repositories.

### Dependency Direction
-   `http` -> `service` -> `jpa`
-   **Strict Prohibition**: `jpa` MUST NOT depend on `http` or `service`. `service` MUST NOT depend on `http`.

## 4. Package & Module Conventions
-   **Root Package**: `com.kirjaswappi.backend`
-   **Module Structure**:
    -   `common`: Shared utilities, extensions, global exception handling.
    -   `http`:
        -   `controllers`: REST Controllers.
        -   `dtos`: Request/Response records/classes.
        -   `validations`: Custom validation annotations.
    -   `service`:
        -   `Root`: Service classes (e.g., `BookService`).
        -   `entities`: Domain models (Records).
        -   `exceptions`: Business exceptions.
        -   `enums`: Domain enums.
    -   `jpa`:
        -   `daos`: MongoDB Key-Value entities (`*Dao`).
        -   `repositories`: Spring Data Repositories.
    -   `mapper`: Static mapping classes.

## 5. Coding Style & Conventions
### Naming
-   **Classes**: PascalCase (e.g., `BookService`).
-   **Variables/Methods**: camelCase.
-   **Persistence Entities**: MUST end with `Dao` (e.g., `BookDao`).
-   **Domain Entities**: Simple names (e.g., `Book`).
-   **Tests**: Must end with `Test`.

### Immutability & Lombok
-   **Domain Entities**: MUST be Java `record` types.
-   **DAOs**: MUST use Lombok:
    -   `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
    -   `@Accessors(fluent = true)`
-   **Services**: MUST use `@RequiredArgsConstructor` for dependency injection (Constructor Injection).

### Java 25 Features
-   Use `var` for local variable inference.
-   Use `record` for data carriers.

## 6. Domain & Business Logic Rules
-   **Location**: Business logic MUST reside ONLY in the `service` package.
-   **Validation**:
    -   Parameter validation via Jakarta Constraints in DTOs.
    -   Business validation in Services (throwing `BusinessException` subclasses).
-   **State**: Domain objects (`record`) are immutable. Updates require creating new instances (using `@With` or `toBuilder` patterns if available, or manual reconstruction).

## 7. API & DTO Rules
-   **Controllers**:
    -   Annotated with `@RestController`, `@RequestMapping`.
    -   MUST return `ResponseEntity<DTO>`.
    -   MUST implement Swagger/OpenAPI annotations (`@Operation`, `@Tag`).
    -   Use `Constants` for path mappings (e.g., `API_BASE + BOOKS`).
-   **DTOs**:
    -   Located in `http.dtos`.
    -   Uses records or classes with Lombok `@Data`/`@Builder`.
-   **Versioning**: APIs are versioned (e.g., `/api/v1`).

## 8. Persistence & Data Access Rules
-   **Technology**: MongoDB.
-   **Entities**: MUST be in `jpa.daos` and annotated with `@Document`.
-   **Repositories**: MUST extend `MongoRepository`.
-   **Queries**: Use Spring Data derived query methods where possible.
-   **Transactions**: Services MUST be annotated with `@Transactional` (even for Mongo, if replica set enabled/supported).

## 9. Error Handling & Logging
-   **Global Handler**: `com.kirjaswappi.backend.common.http.GlobalExceptionHandler`.
-   **Checked Exceptions**: AVOID. Use runtime exceptions.
-   **Hierarchy**:
    -   `SystemException`: Internal errors (500).
    -   `BusinessException`: Logical errors (400/404/etc).
    -   Specific exceptions (e.g., `BookNotFoundException`) extend `BusinessException`.
-   **Logging**: Use `@Slf4j` (if available) or `LoggerFactory`. Log significant business events at `INFO`. Debug info at `DEBUG`.

## 10. Testing Rules
-   **Unit Tests (`service`)**:
    -   Target: Services.
    -   Tooling: JUnit 5, Mockito.
    -   Naming: `[ClassName]Test`.
    -   Pattern: Mock all dependencies (`Repositories`). Verify interactions.
-   **Slice Tests (`http`)**:
    -   Target: Controllers.
    -   Tooling: `@WebMvcTest`.
    -   Pattern: Mock Service layer using `@MockitoBean`. Use `MockMvc` for requests.
-   **Integration Tests**:
    -   Target: Full Context / Repositories.
    -   Tooling: `@SpringBootTest`, `Testcontainers` (Mongo).
    -   Pattern: Use `TestContainersConfig` to spin up real infrastructure.

## 11. Configuration & Environment Rules
-   **Config Files**: `application.yml` / `application.properties`.
-   **Profiles**: Use `@ActiveProfiles("test")` for tests.
-   **Secrets**: Do not hardcode secrets. Use environment variables.

## 12. What an Agent MUST Do
-   **MUST** strictly follow the `DTO -> Service -> DAO` mapping pattern.
-   **MUST** use Java Records for all new Domain entities.
-   **MUST** suffix all Mongo documents with `Dao` and place them in `jpa.daos`.
-   **MUST** write Unit Tests for any new Service logic.
-   **MUST** write `@WebMvcTest` for any new Controllers.
-   **MUST** use Constructor Injection (`@RequiredArgsConstructor`).
-   **MUST** use `Constants` for API paths.

## 13. What an Agent MUST NOT Do
-   **MUST NOT** expose `*Dao` objects in the API response.
-   **MUST NOT** use Field Injection (`@Autowired` on fields).
-   **MUST NOT** mix MapStruct with the existing manual `Mapper` classes (unless refactoring the entire mapper strategy—stick to the existing manual pattern for consistency).
-   **MUST NOT** use `System.out.println` for logging.
-   **MUST NOT** create mutable Domain entities (use `record`).
