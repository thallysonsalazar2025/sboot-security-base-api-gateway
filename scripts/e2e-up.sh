#!/usr/bin/env sh
set -eu

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.e2e.yml}"

if [ -z "${JWT_HS512_SECRET:-}" ]; then
  echo "JWT_HS512_SECRET is required. Copy .env.example to .env or export a local E2E secret before starting." >&2
  exit 2
fi

export JWT_HS512_SECRET

cleanup_on_failure() {
  echo "E2E environment failed to become healthy. Recent service status:" >&2
  docker compose -f "$COMPOSE_FILE" ps >&2 || true
}
trap cleanup_on_failure INT TERM HUP

echo "Starting SaaS Holerite E2E environment..."
docker compose -f "$COMPOSE_FILE" up -d --build --wait --wait-timeout "${E2E_WAIT_TIMEOUT:-300}" || {
  cleanup_on_failure
  exit 1
}

echo "E2E environment is healthy."
echo "Gateway: http://localhost:8081"
echo "RabbitMQ management: http://localhost:15672"
