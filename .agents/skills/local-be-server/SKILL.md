---
name: local-be-server
description: >-
  Manage the dedicated Course Deals backend test server on port 8081 (mobile-local profile)
  with isolated MySQL schema (training_coupon_mobile_local) and Redis DB 1.
  Ensures the backend is running and healthy before running Android emulator tests.
---

# Local Backend Test Server (`local-be-server`)

Manages the isolated stable backend on port `8081` dedicated to mobile development & emulator QA.

- **Port**: `8081` (avoids port 8080 used by active dev)
- **Database**: `training_coupon_mobile_local` (isolated from dev schema)
- **Redis**: Database Index `1` (isolated from dev index 0)
- **Profile**: `--spring.profiles.active=mobile-local`

---

## 1. Ensure Running (`ensure-healthy`)

Before launching Android emulator E2E tests, verify port 8081 health:

```bash
# Check if backend on 8081 is already UP
if curl -s -f http://localhost:8081/actuator/health >/dev/null 2>&1; then
  echo "Backend mobile-local on port 8081 is already UP and healthy."
  exit 0
fi

echo "Backend on 8081 is down. Starting mobile-local server..."
bash /Users/thanh0x/StudioProjects/course_deals_backend/scripts/bootRunMobileLocal.sh > /tmp/mobile_local_be.log 2>&1 &

# Poll health for up to 45 seconds
for i in {1..45}; do
  if curl -s -f http://localhost:8081/actuator/health >/dev/null 2>&1; then
    echo "Backend mobile-local server is healthy on port 8081!"
    exit 0
  fi
  sleep 1
done

echo "Failed to start backend on port 8081. Last logs:" >&2
tail -n 25 /tmp/mobile_local_be.log >&2
exit 1
```

---

## 2. Check Status (`status`)

Inspect which processes are occupying the dev and test ports:
```bash
lsof -i :8080 -i :8081
```

---

## 3. Refresh Server (`refresh`)

When new features are merged to `main`, restart the mobile-local server to apply new Flyway migrations:
```bash
kill -9 $(lsof -ti :8081) 2>/dev/null || true
bash /Users/thanh0x/StudioProjects/course_deals_backend/scripts/bootRunMobileLocal.sh > /tmp/mobile_local_be.log 2>&1 &
```
