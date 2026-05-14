# Reactive URL Shortener

A high-performance, reactive URL shortening service built with **Spring Boot 4.0** and **Java 25**. This project demonstrates
modern backend patterns, focusing on scalability, type safety, and efficient data retrieval.

## 🚀 Tech Stack

* **Java 25** (utilizing Foojay Toolchain for automatic provisioning)
* **Spring Boot 4.0** (WebFlux, R2DBC, Security, Actuator)
* **PostgreSQL 18** (Reactive driver via R2DBC)
* **Redis 8** (Reactive cache via `spring-boot-starter-data-redis-reactive`)
* **Keycloak 26** (OAuth2 / OIDC authentication and authorization)
* **Flyway** (Database migrations in Docker)
* **OpenAPI Generator** (API-first approach with reactive stubs)
* **SpringDoc** (OpenAPI UI for manual testing)
* **Lombok** (FieldNameConstants for type-safe database queries)
* **Testcontainers** (Automated integration testing with real PostgreSQL instances)
* **JaCoCo** (Code coverage enforcement — 80% minimum)
* **Pitest** (Mutation testing — 85% mutation coverage, 90% line coverage)
* **Gatling** (Load and performance testing)
* **ArchUnit** (Architectural tests)
* **AspectJ** (AOP for cross-cutting concerns)

## 🛠 Key Architectural Features

### 1. Advanced Keyset Pagination

Unlike traditional `OFFSET`-based pagination, this service implements **Keyset (Cursor) Pagination**:

* **Performance:** Constant-time O(1) lookups regardless of page depth.
* **UUID v7 Integration:** Identifiers are time-ordered (time-based epoch), ensuring natural chronological sorting in the
  database.
* **Base62 Encoding:** 128-bit UUIDs are compressed into 22-character strings (GMP alphabet) for short, user-friendly URLs while
  maintaining lexicographical order for cursor stability.
* **Padding:** Fixed-length codes (left-padded with '0') to ensure consistent string comparison in SQL queries.

### 2. Multi-Module Project Structure

* `:database` – Contains schema definitions and core R2DBC entity mappings.
* `:shortener` – Houses the reactive business logic, service layer, and OpenAPI-generated controllers.
* `:shortener.gatling` – Gatling load test scenarios.

### 3. API-First Development

The API is defined using **OpenAPI 3.0** (`api.yaml`). The build process automatically generates:

* Reactive controller interfaces.
* Immutable DTO models with Java 8 date/time support.

## 🏁 Getting Started

### Prerequisites

* **Docker** – Required for the database and integration tests.
* The project uses **Gradle Foojay Resolver**, so it will automatically download the required **JDK 25** if it is not present on
  your system.

### Run Development Environment

1. **Build the application** (includes compilation, tests, and JaCoCo coverage check):
   ```bash
   ./gradlew build
   ```
2. **Start only infrastructure** (PostgreSQL, Redis, Keycloak, Flyway):
   ```bash
   docker compose -f ./deployment/docker-compose.yml up -d
   ```
3. **Run the full stack** (builds Docker image + starts application):
   ```bash
   docker compose -f ./deployment/docker-compose.yml --profile full up -d
   ```

### Keycloak Setup

The Keycloak instance auto-imports a realm configuration from `deployment/keycloak/realm-export.json` on first start.
The admin console is available at `http://localhost:8180` (admin / admin).

## 📊 Monitoring & Health

The service includes **Spring Boot Actuator** and **Micrometer Prometheus** registry on a dedicated management port:

* **Health Check:** `GET http://localhost:8081/actuator/health`
* **Metrics:** `GET http://localhost:8081/actuator/prometheus`

## 🧪 Testing

The project uses **Testcontainers** to run tests against a real PostgreSQL instance in an isolated environment.

Tests and JaCoCo coverage verification run automatically during `./gradlew build`.

### Run Gatling load tests

```bash
./gradlew gatlingRun
```

### Run Pitest mutation tests

```bash
./gradlew pitest
```
