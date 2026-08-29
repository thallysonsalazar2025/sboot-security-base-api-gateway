#!/usr/bin/env bash
set -euo pipefail

OWNER="${GITHUB_OWNER:-thallysonsalazar2025}"
WORKDIR="${PAYROLL_E2E_WORKDIR:-.payroll-e2e}"
TAG="${IMAGE_TAG:-latest}"

services=(
  sboot-security-base-auth-service
  sboot-payroll-query-service
  boot-payroll-orchestrator-service
  payroll-generation-request-publisher
  sboot-payroll-generation-processor
  sboot-payroll-calculation-service
  sboot-data-employe-serice
  sboot-data-company-serice
  sboot-payroll-events-service
  sboot-payroll-validation-service
  sboot-time-tracking-integration-service
)

mkdir -p "$WORKDIR"

for service in "${services[@]}"; do
  repo_dir="$WORKDIR/$service"
  repo_url="https://github.com/$OWNER/$service.git"

  if [[ -d "$repo_dir/.git" ]]; then
    git -C "$repo_dir" fetch --depth=1 origin
    default_branch="$(git -C "$repo_dir" remote show origin | sed -n '/HEAD branch/s/.*: //p')"
    git -C "$repo_dir" reset --hard "origin/$default_branch"
  else
    git clone --depth=1 "$repo_url" "$repo_dir"
  fi

  if [[ ! -f "$repo_dir/Dockerfile" ]]; then
    echo "ERROR: $service does not contain a Dockerfile" >&2
    exit 1
  fi

  docker build --pull -t "$service:$TAG" "$repo_dir"
done

echo "Prepared ${#services[@]} payroll service images with tag $TAG"
