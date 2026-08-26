#!/usr/bin/env sh
set -eu

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8081}"
TENANT_A_TOKEN="${TENANT_A_TOKEN:-}"
TENANT_B_TOKEN="${TENANT_B_TOKEN:-}"

if [ -z "$TENANT_A_TOKEN" ] || [ -z "$TENANT_B_TOKEN" ]; then
  echo "TENANT_A_TOKEN and TENANT_B_TOKEN are required for the authenticated E2E smoke." >&2
  exit 2
fi

request() {
  token="$1"
  path="$2"
  curl --fail-with-body --silent --show-error \
    -H "Authorization: Bearer $token" \
    -H "X-Correlation-Id: e2e-smoke-$(date +%s)" \
    "$GATEWAY_URL$path"
}

echo "Checking authenticated gateway access for tenant A..."
request "$TENANT_A_TOKEN" "/actuator/health" >/dev/null

echo "Checking authenticated gateway access for tenant B..."
request "$TENANT_B_TOKEN" "/actuator/health" >/dev/null

echo "Authenticated gateway smoke preflight passed."
