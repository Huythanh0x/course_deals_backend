#!/usr/bin/env bash
set -euo pipefail

PORT="${PORT:-8081}"
BASE_URL="${BASE_URL:-http://localhost:${PORT}}"

echo "============================================================"
echo "E2E API Test: Remote App Configuration & Feature Flags (Ticket #156 / BE #52)"
echo "Target Base URL: ${BASE_URL}"
echo "============================================================"

# 1. Quadrant 1 (Happy Path): GET /api/v1/app/config unauthenticated
echo ""
echo "--- [1/4] GET /api/v1/app/config (Unauthenticated Public Endpoint) ---"
CONFIG_RESP=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "${BASE_URL}/api/v1/app/config" \
  -H "Accept: application/json")

STATUS=$(echo "$CONFIG_RESP" | grep "HTTP_STATUS:" | cut -d':' -f2)
BODY=$(echo "$CONFIG_RESP" | grep -v "HTTP_STATUS:")

echo "HTTP Status: $STATUS"
echo "Response Body: $BODY"

if [ "$STATUS" -ne 200 ]; then
  echo "❌ Expected HTTP 200 OK for GET /api/v1/app/config but got $STATUS"
  exit 1
fi

MIN_VERSION=$(echo "$BODY" | jq -r '.minSupportedVersionCode')
LATEST_VERSION=$(echo "$BODY" | jq -r '.latestVersionCode')
FORCE_UPDATE_URL=$(echo "$BODY" | jq -r '.forceUpdateUrl')

if [ "$MIN_VERSION" != "105" ]; then
  echo "❌ Expected minSupportedVersionCode=105 but got $MIN_VERSION"
  exit 1
fi

if [ "$LATEST_VERSION" != "106" ]; then
  echo "❌ Expected latestVersionCode=106 but got $LATEST_VERSION"
  exit 1
fi

if [ "$FORCE_UPDATE_URL" != "https://play.google.com/store/apps/details?id=com.thanh0x.coursedeals" ]; then
  echo "❌ Unexpected forceUpdateUrl: $FORCE_UPDATE_URL"
  exit 1
fi
echo "✅ Quadrant 1: Public GET /api/v1/app/config returned 200 OK with expected version gate metadata."

# 2. Quadrant 2 (Feature Flags Schema & Value Verification)
echo ""
echo "--- [2/4] Feature Flags Schema & Value Assertions ---"
FF_CLIENT_VERIF=$(echo "$BODY" | jq -r '.featureFlags.clientVerificationEnabled')
FF_COMMUNITY=$(echo "$BODY" | jq -r '.featureFlags.communityReportsEnabled')
FF_EXTERNAL_CHECKOUT=$(echo "$BODY" | jq -r '.featureFlags.preferExternalBrowserCheckout')
FF_THRESHOLD=$(echo "$BODY" | jq -r '.featureFlags.enrollFlowReportThreshold')

echo "featureFlags.clientVerificationEnabled = ${FF_CLIENT_VERIF}"
echo "featureFlags.communityReportsEnabled = ${FF_COMMUNITY}"
echo "featureFlags.preferExternalBrowserCheckout = ${FF_EXTERNAL_CHECKOUT}"
echo "featureFlags.enrollFlowReportThreshold = ${FF_THRESHOLD}"

if [ "$FF_CLIENT_VERIF" != "false" ] || [ "$FF_COMMUNITY" != "true" ] || [ "$FF_EXTERNAL_CHECKOUT" != "false" ] || [ "$FF_THRESHOLD" != "0.3" ]; then
  echo "❌ Feature flags payload did not match expected default contract!"
  exit 1
fi
echo "✅ Quadrant 2: All 4 remote feature flags match contract types and values."

# 3. Quadrant 3 (Error / Unsupported Method Path): POST /api/v1/app/config must not be allowed
echo ""
echo "--- [3/4] POST /api/v1/app/config (Read-Only Endpoint Enforcement) ---"
POST_RESP=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "${BASE_URL}/api/v1/app/config" \
  -H "Content-Type: application/json" \
  -d '{"minSupportedVersionCode": 1}')

POST_STATUS=$(echo "$POST_RESP" | grep "HTTP_STATUS:" | cut -d':' -f2)
POST_BODY=$(echo "$POST_RESP" | grep -v "HTTP_STATUS:")

echo "HTTP Status: $POST_STATUS"
echo "Response Body: $POST_BODY"

if [ "$POST_STATUS" -eq 200 ]; then
  echo "❌ Expected non-200 status for POST /api/v1/app/config, got 200"
  exit 1
fi
echo "✅ Quadrant 3: Unsupported POST method rejected with HTTP $POST_STATUS."

# 4. Quadrant 4 (Related Security Config Regression Check): Protected endpoint still requires auth
echo ""
echo "--- [4/4] GET /api/v1/users/me (Protected Endpoint Regression Check) ---"
PROTECTED_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "${BASE_URL}/api/v1/users/me")
echo "HTTP Status: $PROTECTED_STATUS"

if [ "$PROTECTED_STATUS" -ne 401 ] && [ "$PROTECTED_STATUS" -ne 403 ]; then
  echo "❌ Expected HTTP 401/403 for unauthenticated GET /api/v1/users/me, got $PROTECTED_STATUS"
  exit 1
fi
echo "✅ Quadrant 4: Protected endpoint remains secured (HTTP $PROTECTED_STATUS)."

echo ""
echo "============================================================"
echo "🎉 All 4 Quadrants PASSED for Ticket #156 (BE #52)!"
echo "============================================================"
