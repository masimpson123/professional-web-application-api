# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class or method
./mvnw test -Dtest=Service2025ApplicationTests
./mvnw test -Dtest=Service2025ApplicationTests#contextLoads

# Build
./mvnw clean package

# Docker build and run
docker build --platform linux/amd64 -t service2025 .
docker run -p 8080:8080 service2025
```

## Architecture

Single-class Spring Boot application (`Service2025Application.java`) that combines the main entry point, REST controller, and WebSocket broker configuration in one class.

**Package:** `com.service.service_2025`

### Key Files
- `Service2025Application.java` — Main app, all REST endpoints, WebSocket config
- `Data.java` — Static in-memory data (places, items, books lists)
- `AIQuery.java` — Request DTO for the `/ai` endpoint

### REST Endpoints
| Endpoint | Description |
|---|---|
| `GET /` | Health check |
| `GET /weather` | Requires Firebase auth token in `Authorization` header |
| `GET /weather-advanced` | Requires Firebase auth + `advanced-usage` custom claim |
| `GET /request-advanced-usage-claim` | Sets `advanced-usage` claim on authenticated user |
| `POST /ai` | Proxies a query to Gemini 2.0 Flash |
| `GET /search/{searchTerm}` | Concurrent search across places, items, and books using `CompletableFuture` |

### WebSocket
- STOMP broker at `/websocket-broker`, allowed origin `http://localhost:4200`
- Input destination: `/websocket-input` → output: `/websocket-output`
- Current behavior: reverses the incoming string

### Auth
Firebase Admin SDK (`firebase-admin 9.4.2`) verifies ID tokens passed as `Authorization: <token>` headers. The Firebase project is `endpoint-one`.

### Concurrent Search
The `/search` endpoint launches three parallel `CompletableFuture` tasks (places, items, books), each with a random 0–5 second delay, and streams JSON results.

## Deployment

Hosted on GCP Cloud Run. Images pushed to:
```
us-central1-docker.pkg.dev/endpoint-one/endpoint-one/service2025
```

The Dockerfile runs `./mvnw test` then `spring-boot:run` at container startup (not a pre-built JAR).
