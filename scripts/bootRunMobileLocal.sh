#!/usr/bin/env bash
set -euo pipefail

# Runs coupon-api-service locally on port 8081 with dedicated isolated database & Redis DB 1
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "=== Ensuring Local Infrastructure is Running ==="
if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
  docker compose -f docker-compose.local.yml up -d mysql redis
fi

echo "=== Starting Course Deals Backend (mobile-local on port 8081) ==="
exec ./gradlew :modules:coupon-api-service:bootRun --args='--spring.profiles.active=mobile-local'
