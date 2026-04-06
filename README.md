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

## CorrelationId e Logging para Kibana/Elasticsearch

- O gateway adiciona `X-Correlation-Id` automaticamente quando a requisição não traz o header.
- O mesmo `correlationId` é propagado para MDC e para logs de controller/service/client.
- Foi adicionada publicação de logs para Elasticsearch (compatível com visualização no Kibana).

### Variáveis de ambiente de logging

```bash
LOG_ELASTICSEARCH_ENABLED=true
LOG_ELASTICSEARCH_ENDPOINT=https://my-elasticsearch-project-f23ad4.es.us-central1.gcp.elastic.cloud:443
LOG_ELASTICSEARCH_INDEX=sboot-security-base-api-gateway-logs
LOG_ELASTICSEARCH_API_KEY=<api_key_elastic>
```

> Observação: Em Elastic Cloud, normalmente é necessário `LOG_ELASTICSEARCH_API_KEY` válido para indexação.

## Ambiente E2E com todos os componentes

Foi adicionado o arquivo `docker-compose.e2e.yml` com os componentes solicitados e seus encadeamentos principais via HTTP + RabbitMQ.

### Subir ambiente

```bash
docker compose -f docker-compose.e2e.yml up -d --build
```

### Validar fluxo básico E2E (exemplo)

```bash
curl -i 'http://localhost:8081/api/v1/payroll?year=2026&month=4' \
  -H "Authorization: Bearer <jwt_valido>" \
  -H "X-Correlation-Id: e2e-2026-04-06-001"
```

- Verifique no retorno o header `X-Correlation-Id`.
- Consulte no Kibana pelo campo `correlationId:"e2e-2026-04-06-001"` para acompanhar o fluxo ponta a ponta.

## Security

- JWT tokens are validated locally with HMAC `HS512`.
- The gateway extracts `companyId` and `employeeId` claims to authorize access to payroll data.
- All endpoints under `/api/v1` require authentication.

Set the HMAC secret via env var:

```bash
JWT_HS512_SECRET=YmFzaWNfY3JlZGVudGlhbC5zZWNyZXQta2V5LWZvci1zYWFzLWhvbGVyaXRlLWJmZi1nYXRld2F5
```

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
  -e LOG_ELASTICSEARCH_ENABLED=true \
  -e LOG_ELASTICSEARCH_ENDPOINT=https://my-elasticsearch-project-f23ad4.es.us-central1.gcp.elastic.cloud:443 \
  -e LOG_ELASTICSEARCH_API_KEY=... \
  sboot-security-base-api-gateway:latest
```
