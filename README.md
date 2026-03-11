# sboot-security-base-api-gateway

Production-ready Spring Boot API Gateway acting as **Gateway + BFF + OAuth2 Resource Server** for an Angular frontend.

## Architecture

```text
Angular Client
   |
   v
Identity Service (authenticates users, issues JWT)
   |
   v
sboot-security-base-api-gateway (this project)
   |
   v
holerite-service
   |
   v
RabbitMQ workers
```

## Responsibilities

This gateway is responsible for:

1. Receiving requests from Angular clients.
2. Validating JWT tokens issued by `identity-service` using JWKS.
3. Applying cross-cutting concerns globally:
   - Correlation ID
   - Request logging
   - Rate limiting
   - Request timing
4. Routing requests to backend microservices.
5. Serving BFF endpoints for frontend-friendly APIs.

## Tech Stack

- Java 21
- Spring Boot 3.3.x
- Spring Cloud Gateway
- Spring Security (WebFlux)
- OAuth2 Resource Server
- JWT / JWKS validation
- Maven

## Project Structure

```text
.
├── Dockerfile
├── pom.xml
├── README.md
└── src
    ├── main
    │   ├── java/com/example/gateway
    │   │   ├── SbootSecurityBaseApiGatewayApplication.java
    │   │   ├── config
    │   │   │   ├── GatewayRouteConfig.java
    │   │   │   └── SecurityConfig.java
    │   │   ├── filter
    │   │   │   ├── CorrelationIdFilter.java
    │   │   │   ├── RateLimitingFilter.java
    │   │   │   ├── RequestLoggingFilter.java
    │   │   │   └── RequestTimingFilter.java
    │   │   └── web
    │   │       └── HealthController.java
    │   └── resources
    │       └── application.yml
    └── test
        └── java/com/example/gateway
```

## Security

- Configured as an OAuth2 Resource Server (`spring-boot-starter-oauth2-resource-server`).
- JWT tokens are validated against identity-service JWKS URI.
- All endpoints require authentication except:
  - `GET /actuator/health`

Set JWKS URI via env var:

```bash
IDENTITY_SERVICE_JWKS_URI=http://identity-service:8081/oauth2/jwks
```

## Gateway Routes

Routes configured in `GatewayRouteConfig`:

1. `POST /holerites/generate`
   - forwards to `http://holerite-service:8080/holerites/generate`
2. `GET /holerites/{id}`
   - forwards to `http://holerite-service:8080/holerites/{id}`

## Filters

### 1) Correlation ID Filter
- Header: `X-Correlation-Id`
- Reuses existing correlation ID or generates a new UUID.

### 2) Request Logging Filter
- Logs incoming requests (method/path/query/remote address).
- Logs response status when completed.

### 3) Rate Limiting Filter
- In-memory per-client IP window limiter.
- Configurable with:
  - `gateway.rate-limit.max-requests-per-window`
  - `gateway.rate-limit.window-seconds`
- Returns HTTP `429 Too Many Requests` with `Retry-After` header.

### 4) Request Timing Filter
- Measures end-to-end processing time.
- Adds `X-Response-Time-Ms` response header.

## Health Endpoints

- Public operational health check:
  - `GET /actuator/health`
- Example BFF endpoint:
  - `GET /api/v1/health` (requires authentication)

## Running Locally

```bash
mvn clean spring-boot:run
```

## Build

```bash
mvn clean package
```

## Docker

Build image:

```bash
docker build -t sboot-security-base-api-gateway:latest .
```

Run container:

```bash
docker run --rm -p 8080:8080 \
  -e IDENTITY_SERVICE_JWKS_URI=http://identity-service:8081/oauth2/jwks \
  sboot-security-base-api-gateway:latest
```

## Production Notes

- For distributed rate limiting, replace in-memory limiter with Redis-based limiter.
- Ensure tokens include required audience/issuer claims and validate them in policy.
- Use centralized log aggregation and propagate correlation IDs to downstream services.
- Prefer mTLS and private networking between gateway and downstream services.
