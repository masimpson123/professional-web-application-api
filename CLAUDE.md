# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application on port 8081 (or use the Run button on Service2025Application in IntelliJ)
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class or method
./mvnw test -Dtest=Service2025ApplicationTests
./mvnw test -Dtest=Service2025ApplicationTests#contextLoads

# Build
./mvnw clean package

# Docker build and run (the container listens on 8080)
docker build --platform linux/amd64 -t service2025 .
docker run -p 8080:8080 service2025
```

## Architecture

Spring Boot application, package `com.service.service_2025`. `Service2025Application.java` combines the main entry point, most REST endpoints, and the WebSocket broker configuration; Zoom token signing lives in its own controller.

### Key Files
- `Service2025Application.java` — Main app, REST endpoints except `/zoom-token`, WebSocket config
- `ZoomController.java` — `GET /zoom-token`, signs Zoom Video SDK session tokens
- `Data.java` — In-memory data for `/search` (places, items, books lists)
- `AIQuery.java` — Request DTO for the `/ai` endpoint

### REST Endpoints

All REST endpoints allow any origin (`@CrossOrigin`).

| Endpoint | Description |
|---|---|
| `GET /` | HTML welcome page; doubles as a health check |
| `GET /weather` | Requires a Firebase ID token |
| `GET /weather-advanced` | Requires a Firebase ID token; the message depends on the `advanced-usage` custom claim |
| `GET /request-advanced-usage-claim` | Sets the `advanced-usage` claim on the authenticated user |
| `POST /ai` | Forwards the request body's `query` to Gemini (`gemini-3.6-flash`) and returns `{"response": ...}` |
| `GET /search/{searchTerm}` | Concurrent search across places, items, and books, streamed as each finishes |
| `GET /zoom-token` | Signs a Zoom Video SDK JWT for the shared `msio-video` session |

The Firebase endpoints return `{"response": ...}` on success and `{"error": ...}` on failure, both with HTTP 200.

### Configuration

`application.properties` imports `localhost.properties` from the project root with `optional:file:`, and values there override it. The file is gitignored and never copied into the Docker image, and it's found as long as the app runs from the project root (the IntelliJ default). Never commit secrets: this repo is public.

| Property | Production source | Local source |
|---|---|---|
| `gemini.api.key` | `GEMINI_KEY` env var | `localhost.properties` |
| `zoom.sdk.key`, `zoom.sdk.secret` | `ZOOM_SDK_KEY`, `ZOOM_SDK_SECRET` env vars | `localhost.properties` |
| `websocket.allowed-origins` | `application.properties` (production site only) | `localhost.properties` (adds `http://localhost:4200`) |
| `server.port` | `PORT` env var, set by Cloud Run and the Dockerfile (8080) | Defaults to 8081, so the Express server in `../client-2026` can use 8080 |

Missing secrets default to empty, so the app still starts; `/zoom-token` returns 503 until the Zoom values are set.

### WebSocket
- STOMP endpoint at `/websocket-broker`, accepting browser origins from `websocket.allowed-origins`
- Clients publish to `/pub/{roomId}` and subscribe to `/sub/{roomId}`
- Each message is broadcast to the room as `{roomId} | HH:mm:ss | {message}`

### Auth
Firebase Admin SDK (`firebase-admin 9.4.2`) verifies ID tokens sent as `Authorization: Bearer <token>`. It authenticates with Application Default Credentials for the `endpoint-one` project, so locally run `gcloud auth application-default login` before calling the Firebase endpoints.

### Concurrent Search
`/search` launches three parallel `CompletableFuture` tasks (places, items, books). Each waits a random 0–5 seconds, then writes its matches to the response stream as a JSON object, so results arrive in completion order.

## Deployment

Hosted on GCP Cloud Run as the `endpoint-one-2` service. Images are pushed to:
```
us-central1-docker.pkg.dev/endpoint-one/endpoint-one/service2025
```

Run `/deploy-to-gcp` to deploy the local working tree without committing; it checks sign-in, the Cloud Run secrets, and the tests first. The README lists the manual steps. The Dockerfile runs `./mvnw test` then `spring-boot:run` at container startup (not a pre-built JAR).
