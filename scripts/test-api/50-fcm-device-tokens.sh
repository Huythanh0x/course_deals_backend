#!/usr/bin/env bash
set -euo pipefail

PORT="${PORT:-8081}"
BASE_URL="http://localhost:${PORT}"

echo "============================================================"
echo "E2E API Test: FCM Device Tokens & Topics (Ticket #50)"
echo "Target Base URL: ${BASE_URL}"
echo "============================================================"

# Test Token
TEST_FCM_TOKEN="fcm_test_token_qa_$(date +%s)"
PLATFORM="ANDROID"

# 1. Happy Path: Register Anonymous / Guest Device Token
echo ""
echo "--- [1/4] POST /api/v1/users/device-token (Guest Registration) ---"
REGISTER_RESP=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "${BASE_URL}/api/v1/users/device-token" \
  -H "Content-Type: application/json" \
  -d "{\"fcmToken\": \"${TEST_FCM_TOKEN}\", \"platform\": \"${PLATFORM}\"}")

STATUS=$(echo "$REGISTER_RESP" | grep "HTTP_STATUS:" | cut -d':' -f2)
BODY=$(echo "$REGISTER_RESP" | grep -v "HTTP_STATUS:")

echo "HTTP Status: $STATUS"
echo "Response Body: $BODY"

if [ "$STATUS" -ne 200 ]; then
  echo "❌ Expected HTTP 200 but got $STATUS"
  exit 1
fi

REGISTERED=$(echo "$BODY" | jq -r '.registered')
if [ "$REGISTERED" != "true" ]; then
  echo "❌ Expected registered=true but got $REGISTERED"
  exit 1
fi
echo "✅ Guest device token successfully registered."

# 2. Failed Path: Invalid Registration Body (Blank token / Invalid platform)
echo ""
echo "--- [2/4] POST /api/v1/users/device-token (Invalid Request Body) ---"
INVALID_RESP=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "${BASE_URL}/api/v1/users/device-token" \
  -H "Content-Type: application/json" \
  -d '{"fcmToken": "", "platform": "INVALID_PLATFORM"}')

STATUS_INVALID=$(echo "$INVALID_RESP" | grep "HTTP_STATUS:" | cut -d':' -f2)
BODY_INVALID=$(echo "$INVALID_RESP" | grep -v "HTTP_STATUS:")

echo "HTTP Status: $STATUS_INVALID"
echo "Response Body: $BODY_INVALID"

if [ "$STATUS_INVALID" -ne 400 ]; then
  echo "❌ Expected HTTP 400 but got $STATUS_INVALID"
  exit 1
fi
echo "✅ Invalid registration rejected with 400 Bad Request."

# 3. Happy Path: Delete Device Token
echo ""
echo "--- [3/4] DELETE /api/v1/users/device-token?fcmToken=... ---"
DELETE_RESP=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X DELETE "${BASE_URL}/api/v1/users/device-token?fcmToken=${TEST_FCM_TOKEN}")

STATUS_DELETE=$(echo "$DELETE_RESP" | grep "HTTP_STATUS:" | cut -d':' -f2)

echo "HTTP Status: $STATUS_DELETE"

if [ "$STATUS_DELETE" -ne 204 ]; then
  echo "❌ Expected HTTP 204 but got $STATUS_DELETE"
  exit 1
fi
echo "✅ Device token deleted with 204 No Content."

# 4. Failed Path: Delete Missing FCM Token Query Parameter
echo ""
echo "--- [4/4] DELETE /api/v1/users/device-token (Missing Parameter) ---"
DELETE_MISSING_RESP=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X DELETE "${BASE_URL}/api/v1/users/device-token")

STATUS_MISSING=$(echo "$DELETE_MISSING_RESP" | grep "HTTP_STATUS:" | cut -d':' -f2)
BODY_MISSING=$(echo "$DELETE_MISSING_RESP" | grep -v "HTTP_STATUS:")

echo "HTTP Status: $STATUS_MISSING"
echo "Response Body: $BODY_MISSING"

if [ "$STATUS_MISSING" -ne 400 ]; then
  echo "❌ Expected HTTP 400 but got $STATUS_MISSING"
  exit 1
fi
echo "✅ Missing parameter rejected with 400 Bad Request."

echo ""
echo "============================================================"
echo "🎉 ALL E2E API TESTS PASSED SUCCESSFULLY!"
echo "============================================================"
