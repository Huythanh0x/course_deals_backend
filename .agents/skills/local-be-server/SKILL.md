---
name: local-be-server
description: >-
  Manage the dedicated Course Deals backend test server on port 8081 (mobile-local profile)
  with isolated MySQL schema (training_coupon_mobile_local), Redis DB 1, and background
  crawler service on port 8082. Ensures both backend API and crawler daemon are running and
  healthy before running Android emulator tests.
---

# Local Backend Test Server (`local-be-server`)

Manages the isolated stable backend on port `8081` (`coupon-api-service`) and crawler daemon on port `8082` (`coupon-crawler-service`) dedicated to mobile development & emulator QA.

- **API Port**: `8081` (`coupon-api-service`, avoids port 8080 used by active dev)
- **Crawler Port**: `8082` (`coupon-crawler-service`, runs periodic scraping & updates Redis `last_fetch_time`)
- **JobRunr Dashboard**: `8001` (shared JobRunr dashboard)
- **Database**: `training_coupon_mobile_local` (isolated from dev schema)
- **Redis**: Database Index `1` (isolated from dev index 0; key `last_fetch_time`)
- **Profile**: `--spring.profiles.active=mobile-local`

---

## 1. Ensure Running (`ensure-healthy`)

Before launching Android emulator E2E tests or checking live deal sync, verify backend API (port 8081) and background crawler daemon (port 8082):

```bash
# Check if backend API on 8081 is already UP
API_UP=false
if curl -s -f http://localhost:8081/actuator/health >/dev/null 2>&1; then
  API_UP=true
fi

# Check if crawler on 8082 is already UP
CRAWLER_UP=false
if lsof -ti :8082 >/dev/null 2>&1; then
  CRAWLER_UP=true
fi

if [ "$API_UP" = true ] && [ "$CRAWLER_UP" = true ]; then
  echo "Backend API (8081) and Crawler daemon (8082) are both UP and healthy."
  exit 0
fi

echo "Starting mobile-local stack (API 8081 + Crawler 8082)..."
bash /Users/thanh0x/StudioProjects/CourseDeals/course_deals_backend/scripts/bootRunMobileLocal.sh > /tmp/mobile_local_be.log 2>&1 &

# Poll health for up to 45 seconds
for i in {1..45}; do
  if curl -s -f http://localhost:8081/actuator/health >/dev/null 2>&1 && lsof -ti :8082 >/dev/null 2>&1; then
    echo "Backend mobile-local server and crawler are healthy!"
    exit 0
  fi
  sleep 1
done

echo "Failed to start mobile-local stack. Last logs:" >&2
tail -n 25 /tmp/mobile_local_be.log >&2
tail -n 25 /tmp/mobile_local_crawler.log >&2
exit 1
```

---

## 2. Check Status (`status`)

Inspect which processes are occupying the dev, test, and crawler ports:
```bash
lsof -i :8080 -i :8081 -i :8082 -i :8001
```

Check last fetch timestamp in Redis:
```bash
docker exec training-coupon-redis-local redis-cli -n 1 GET last_fetch_time
```

---

## 3. Refresh Server (`refresh`)

When new features or schema changes are merged to `main`, restart the mobile-local server and crawler daemon:
```bash
# Terminate existing mobile-local instances
kill -9 $(lsof -ti :8081) 2>/dev/null || true
kill -9 $(lsof -ti :8082) 2>/dev/null || true

# Relaunch both services via bootRunMobileLocal.sh
bash /Users/thanh0x/StudioProjects/CourseDeals/course_deals_backend/scripts/bootRunMobileLocal.sh > /tmp/mobile_local_be.log 2>&1 &
```
