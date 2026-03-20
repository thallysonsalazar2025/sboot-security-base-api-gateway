# sboot-security-base-api-gateway

Production-ready Spring Boot API Gateway acting as **Gateway + BFF** for the Payroll system.

## Architecture

```text
API Gateway
   ↓
Publisher (request)
   ↓
RabbitMQ
   ↓
Orchestrator
   ↓
 ┌───────────────┬──────────────────┐
 ↓               ↓                  ↓
Employee       Company         Calculation
Service        Service         Service
 └───────────────┴──────────────────┘
           ↓
   Generation Processor
           ↓
   Document Storage Service
           ↓
   Status Service
           ↓
   Notification Service (opcional)

```

## Responsibilities

This gateway acts as the entry point for the Payroll system, handling:

1. **Authentication**: Validates JWT tokens using shared-secret HMAC (`HS512`).
2. **Context Extraction**: Extracts user context (`companyId`, `employeeId`) from the validated token.
3. **Request Adaptation**: Transforms frontend requests (`GET /payroll?year=YYYY&month=MM`) into backend service calls (`POST /payroll` with `PayrollRequest`).
4. **Proxying**: Forwards the request to `sboot-payroll-query-service` and returns the response transparently.
5. **Cross-Cutting Concerns**: Handles correlation IDs, logging, and security globally.

## Tech Stack

- Java 21
- Spring Boot 3.3.x
- Spring WebFlux (Reactive Stack)
- Spring Security (OAuth2 Resource Server + JWT)
- Spring WebClient
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
    │   │   ├── client
    │   │   │   ├── PayrollQueryClient.java
    │   │   │   └── WebClientPayrollQueryClient.java
    │   │   ├── config
    │   │   │   ├── CorsGlobalConfig.java
    │   │   │   ├── HmacJwtDecoderConfig.java
    │   │   │   ├── OutboundAuthPropagationConfig.java
    │   │   │   └── SecurityConfig.java
    │   │   ├── controller
    │   │   │   └── PayrollController.java
    │   │   ├── dto
    │   │   │   └── PayrollRequest.java
    │   │   ├── filter
    │   │   │   ├── CorrelationIdFilter.java
    │   │   │   ├── RateLimitingFilter.java
    │   │   │   ├── RequestLoggingFilter.java
    │   │   │   └── RequestTimingFilter.java
    │   │   ├── security
    │   │   │   └── JwtAuthenticationFilter.java
    │   │   └── service
    │   │       ├── PayrollService.java
    │   │       └── PayrollServiceImpl.java
    │   └── resources
    │       └── application.yml
    └── test
        └── java/com/example/gateway
```

## Security

- JWT tokens are validated locally with HMAC `HS512`.
- The gateway extracts `companyId` and `employeeId` claims to authorize access to payroll data.
- All endpoints under `/api/v1` require authentication.

Set the HMAC secret via env var:

```bash
JWT_HS512_SECRET=YmFzaWNfY3JlZGVudGlhbC5zZWNyZXQta2V5LWZvci1zYWFzLWhvbGVyaXRlLWJmZi1nYXRld2F5
```

## API Endpoints

### Payroll Query

- **Endpoint**: `GET /api/v1/payroll`
- **Parameters**:
  - `year` (int): Year of the payroll period (YYYY)
  - `month` (int): Month of the payroll period (MM)
- **Headers**:
  - `Authorization`: `Bearer <valid_jwt_token>`
- **Description**: Retrieves payroll information for the authenticated user based on the provided period. The gateway enriches the request with `companyId` and `employeeId` from the token before forwarding it to the backend service.

## Configuration

Set the backend service URL via env var:

```bash
PAYROLL_QUERY_SERVICE_URL=http://localhost:8082
```

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
docker run --rm -p 8081:8081 \
  -e JWT_HS512_SECRET=... \
  -e PAYROLL_QUERY_SERVICE_URL=http://host.docker.internal:8082 \
  sboot-security-base-api-gateway:latest
```

## Production Notes

- Ensure `PAYROLL_QUERY_SERVICE_URL` points to the correct service discovery name or load balancer URL.
- Validate that JWT tokens contain the required `companyId` and `employeeId` claims.
- Monitor `X-Response-Time-Ms` headers for performance tracking.
