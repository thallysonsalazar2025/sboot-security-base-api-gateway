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

if [ "$TENANT_A_TOKEN" = "$TENANT_B_TOKEN" ]; then
  echo "TENANT_A_TOKEN and TENANT_B_TOKEN must represent distinct tenant identities." >&2
  exit 2
fi

request() {
  token="$1"
  path="$2"
  correlation_id="$3"
  headers_file="$(mktemp)"

  status="$(curl --silent --show-error --output /dev/null --dump-header "$headers_file" --write-out '%{http_code}' \
    -H "Authorization: Bearer $token" \
    -H "X-Correlation-Id: $correlation_id" \
    "$GATEWAY_URL$path")"

  response_correlation_id="$(awk 'tolower($1) == "x-correlation-id:" {gsub("\r", "", $2); print $2; exit}' "$headers_file")"
  rm -f "$headers_file"

  printf '%s|%s\n' "$status" "$response_correlation_id"
}

assert_authenticated() {
  tenant="$1"
  token="$2"
  correlation_id="e2e-smoke-${tenant}-$(date +%s)-$$"
  response="$(request "$token" "/api/v1/payroll?year=$SMOKE_YEAR&month=$SMOKE_MONTH" "$correlation_id")"
  status="${response%%|*}"
  response_correlation_id="${response#*|}"

  if [ "$response_correlation_id" != "$correlation_id" ]; then
    echo "Authenticated gateway smoke failed for tenant $tenant: correlation id was not preserved (sent=$correlation_id, received=${response_correlation_id:-missing})." >&2
    exit 1
  fi

  case "$status" in
    2??|3??|404)
      echo "Tenant $tenant token was accepted by the protected payroll endpoint (HTTP $status) and correlation id was preserved."
      ;;
    401|403)
      echo "Authenticated gateway smoke failed for tenant $tenant: protected endpoint returned HTTP $status." >&2
      exit 1
      ;;
    000)
      echo "Authenticated gateway smoke failed for tenant $tenant: gateway was unreachable." >&2
      exit 1
      ;;
    4??)
      echo "Authenticated gateway smoke failed for tenant $tenant: unexpected client error HTTP $status." >&2
      exit 1
      ;;
    5??)
      echo "Authenticated gateway smoke failed for tenant $tenant: downstream/server error HTTP $status." >&2
      exit 1
      ;;
    *)
      echo "Authenticated gateway smoke failed for tenant $tenant: unexpected HTTP status $status." >&2
      exit 1
      ;;
  esac
}

echo "Checking protected gateway access for tenant A..."
assert_authenticated "A" "$TENANT_A_TOKEN"

echo "Checking protected gateway access for tenant B..."
assert_authenticated "B" "$TENANT_B_TOKEN"

echo "Authenticated gateway smoke preflight passed."
