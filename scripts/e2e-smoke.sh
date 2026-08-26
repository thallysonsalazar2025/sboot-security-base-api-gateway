#!/usr/bin/env sh
set -eu

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8081}"
TENANT_A_TOKEN="${TENANT_A_TOKEN:-}"
TENANT_B_TOKEN="${TENANT_B_TOKEN:-}"
SMOKE_YEAR="${SMOKE_YEAR:-2026}"
SMOKE_MONTH="${SMOKE_MONTH:-1}"

if [ -z "$TENANT_A_TOKEN" ] || [ -z "$TENANT_B_TOKEN" ]; then
  echo "TENANT_A_TOKEN and TENANT_B_TOKEN are required for the authenticated E2E smoke." >&2
  exit 2
fi

request() {
  token="$1"
  path="$2"
  curl --silent --show-error --output /dev/null --write-out '%{http_code}' \
    -H "Authorization: Bearer $token" \
    -H "X-Correlation-Id: e2e-smoke-$(date +%s)" \
    "$GATEWAY_URL$path"
}

assert_authenticated() {
  tenant="$1"
  token="$2"
  status="$(request "$token" "/api/v1/payroll?year=$SMOKE_YEAR&month=$SMOKE_MONTH")"

  case "$status" in
    401|403)
      echo "Authenticated gateway smoke failed for tenant $tenant: protected endpoint returned HTTP $status." >&2
      exit 1
      ;;
    000)
      echo "Authenticated gateway smoke failed for tenant $tenant: gateway was unreachable." >&2
      exit 1
      ;;
    *)
      echo "Tenant $tenant token was accepted by the protected payroll endpoint (HTTP $status)."
      ;;
  esac
}

echo "Checking protected gateway access for tenant A..."
assert_authenticated "A" "$TENANT_A_TOKEN"

echo "Checking protected gateway access for tenant B..."
assert_authenticated "B" "$TENANT_B_TOKEN"

echo "Authenticated gateway smoke preflight passed."
