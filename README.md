# Reactive URL Shortener

A high-performance, reactive URL shortening service built with **Spring Boot 4.0** and **Java 25**. This project demonstrates
modern backend patterns, focusing on scalability, type safety, and efficient data retrieval.

## 🚀 Tech Stack

* **Java 25** (utilizing Foojay Toolchain for automatic provisioning)
* **Spring Boot 4.0** (WebFlux, R2DBC)
* **PostgreSQL 18** (Reactive driver)
* **OpenAPI Generator** (API-first approach with reactive stubs)
* **Lombok** (FieldNameConstants for type-safe database queries)
* **Testcontainers** (Automated integration testing with real PostgreSQL instances)

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

1. **Spin up the database:**
   ```bash
   docker compose up -d
   ```
2. **Build and generate API stubs:**
   ```bash
   ./gradlew build
   ```
3. **Run the application:**
   ```bash
   ./gradlew :shortener:bootRun
   ```

## 📊 Monitoring & Health

The service includes **Spring Boot Actuator** and **Micrometer Prometheus** registry:

* **Health Check:** `GET /actuator/health`
* **Metrics:** `GET /actuator/prometheus`

## 🧪 Testing

The project uses **Testcontainers** to run tests against a real PostgreSQL instance in an isolated environment.

```bash
./gradlew test
