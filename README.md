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
- A publicação remota para Elasticsearch é opt-in no ambiente E2E para evitar envio acidental de logs de testes.

### Variáveis de ambiente de logging

```bash
LOG_ELASTICSEARCH_ENABLED=true
LOG_ELASTICSEARCH_ENDPOINT=https://<seu-cluster-elastic>:443
LOG_ELASTICSEARCH_INDEX=sboot-security-base-api-gateway-logs
LOG_ELASTICSEARCH_API_KEY=<api_key_elastic>
```

## Ambiente E2E com todos os componentes

`docker-compose.e2e.yml` contém os componentes do caminho principal e health gates para RabbitMQ e serviços Spring.

### Subir ambiente em um comando

```bash
sh scripts/e2e-up.sh
```

O script usa um segredo HS512 exclusivamente local quando `JWT_HS512_SECRET` não é informado, executa `docker compose up --build --wait` e só retorna sucesso quando todos os healthchecks estiverem verdes. Para fornecer seu próprio segredo de teste:

```bash
JWT_HS512_SECRET='<segredo-hs512-de-teste-com-entropia-suficiente>' sh scripts/e2e-up.sh
```

O segredo local do script não deve ser usado fora do ambiente E2E.

### Validar fluxo básico E2E (exemplo)

```bash
curl -i 'http://localhost:8081/api/v1/payroll?year=2026&month=4' \
  -H "Authorization: Bearer <jwt_valido>" \
  -H "X-Correlation-Id: e2e-2026-04-06-001"
```

- Verifique no retorno o header `X-Correlation-Id`.
- Se o logging remoto tiver sido habilitado explicitamente, consulte o observability backend pelo `correlationId` para acompanhar o fluxo ponta a ponta.

## Security

- JWT tokens are validated locally with HMAC `HS512`.
- The gateway extracts `companyId` and `employeeId` claims to authorize access to payroll data.
- All endpoints under `/api/v1` require authentication.
- Não existe segredo JWT padrão na configuração da aplicação ou no compose E2E; ambientes compartilhados devem fornecer `JWT_HS512_SECRET` externamente.

Set the HMAC secret via env var:

```bash
JWT_HS512_SECRET=<segredo-hs512-gerenciado-fora-do-repositorio>
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
  -e LOG_ELASTICSEARCH_ENDPOINT=https://<seu-cluster-elastic>:443 \
  -e LOG_ELASTICSEARCH_API_KEY=... \
  sboot-security-base-api-gateway:latest
```
