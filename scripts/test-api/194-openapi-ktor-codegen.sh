#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
API_DOCS_URL="${BASE_URL}/v3/api-docs"
TMP_JSON="$(mktemp /tmp/openapi-spec-XXXXXX.json)"
trap 'rm -f "$TMP_JSON"' EXIT

echo "=== [1/4] Fetching OpenAPI spec from ${API_DOCS_URL} ==="
HTTP_STATUS="$(curl -sS -o "$TMP_JSON" -w '%{http_code}' "$API_DOCS_URL")"
if [ "$HTTP_STATUS" != "200" ]; then
  echo "FAIL: Expected HTTP 200 from ${API_DOCS_URL}, got ${HTTP_STATUS}" >&2
  exit 1
fi
echo "PASS: GET /v3/api-docs returned HTTP ${HTTP_STATUS} ($(wc -c < "$TMP_JSON" | tr -d ' ') bytes)"

echo "=== [2/4] Verifying canonical operationIds in /v3/api-docs ==="
REQUIRED_OPERATION_IDS=(
  "listCoupons"
  "createCoupon"
  "getCouponDetail"
  "getCourseDetails"
  "reportCourse"
  "login"
  "register"
  "sendOtp"
  "verifyOtp"
  "refresh"
  "socialLogin"
  "getPreferences"
  "updatePreferences"
  "getProfile"
  "updateProfile"
  "acceptTerms"
  "deleteMyAccount"
)

ACTUAL_IDS="$(jq -r '[.paths[][] | select(type == "object") | .operationId // empty] | sort | unique | .[]' "$TMP_JSON")"

for op_id in "${REQUIRED_OPERATION_IDS[@]}"; do
  if ! grep -qx "$op_id" <<<"$ACTUAL_IDS"; then
    echo "FAIL: Missing canonical operationId '${op_id}' in /v3/api-docs" >&2
    exit 1
  fi
  echo "  - operationId '${op_id}': FOUND"
done
echo "PASS: All ${#REQUIRED_OPERATION_IDS[@]} required canonical operationIds verified."

echo "=== [3/4] Verifying zero internal entity leakage (UserEntity) ==="
USER_ENTITY_SCHEMA="$(jq -r '.components.schemas.UserEntity // "null"' "$TMP_JSON")"
if [ "$USER_ENTITY_SCHEMA" != "null" ]; then
  echo "FAIL: Internal entity UserEntity leaked into .components.schemas!" >&2
  exit 1
fi
echo "PASS: .components.schemas.UserEntity is null (zero internal entity leakage)."

echo "=== [4/4] Verifying CouponDetailDTO.isNew and CouponSummaryDTO.isNew properties ==="
DETAIL_IS_NEW_TYPE="$(jq -r '.components.schemas.CouponDetailDTO.properties.isNew.type // "null"' "$TMP_JSON")"
SUMMARY_IS_NEW_TYPE="$(jq -r '.components.schemas.CouponSummaryDTO.properties.isNew.type // "null"' "$TMP_JSON")"

if [ "$DETAIL_IS_NEW_TYPE" != "boolean" ]; then
  echo "FAIL: Expected CouponDetailDTO.properties.isNew.type == 'boolean', got '${DETAIL_IS_NEW_TYPE}'" >&2
  exit 1
fi
if [ "$SUMMARY_IS_NEW_TYPE" != "boolean" ]; then
  echo "FAIL: Expected CouponSummaryDTO.properties.isNew.type == 'boolean', got '${SUMMARY_IS_NEW_TYPE}'" >&2
  exit 1
fi
echo "PASS: CouponDetailDTO.properties.isNew (${DETAIL_IS_NEW_TYPE}) and CouponSummaryDTO.properties.isNew (${SUMMARY_IS_NEW_TYPE}) exist."

echo "=== ALL OPENAPI CONTRACT CHECKS PASSED ==="
