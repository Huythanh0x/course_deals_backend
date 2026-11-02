---
name: release-resource
description: >-
  Reclaim host RAM, Swap, and CPU after ticket implementation, E2E QA verification, or PR shipping.
  Captures before/after system stats, stops Gradle and Kotlin compile daemons across backend and
  KMP repositories, cleans up lingering test/bootRun/Desktop JVM processes and orphaned wrappers,
  and outputs a resource reclamation summary table.
---

# Post-Ticket Resource Reclamation (`release-resource`)

Use this skill automatically at the end of ticket implementation / E2E QA (`team-flow`, `ticket-flow`, `swarm-flow`), or whenever the user invokes `/release-resource` to free up RAM, Swap, and CPU on macOS.

> [!IMPORTANT]
> **Why This Gate Exists**:
> Multi-agent builds and cross-platform E2E tests across `./course_deals_backend` and `./course_deals_ComposeMultiplatform` spawn up to **8+ `GradleDaemon`**, **3+ `KotlinCompileDaemon`**, duplicate Spring Boot (`8080`/`8081`/`8082`) and `:desktopApp` JVMs, and orphaned `gradle-wrapper.jar` processes — consuming **7–10+ GB of physical RAM** and **3–4+ GB of Swap**. Stopping these daemons and ephemeral test processes immediately restores system memory headroom without touching persistent infrastructure (Colima/Docker databases, booted Android Emulator, or booted iOS Simulator unless `--deep` is requested).

---

## Execution Protocol

### Step 1: Capture "Before" System Stats Snapshot

Record baseline CPU, Load Average, Physical RAM, Swap, Memory Free %, and active JVM processes:

```bash
echo "=== BEFORE RELEASE-RESOURCE ==="
top -l 1 -n 0 | grep -E "^CPU|^Load|^PhysMem"
sysctl vm.swapusage
memory_pressure | grep "System-wide memory free percentage:" || true
echo "--- Active Java / Gradle / Kotlin Processes ---"
jps -l 2>/dev/null || true
```

---

### Step 2: Stop All Gradle & Kotlin Compile Daemons (`./gradlew --stop`)

Stop all Gradle daemons across both repositories (using portable workspace-relative or repo-relative paths):

```bash
# Resolve workspace root whether invoked from parent workspace or inside a sub-repo
WORKSPACE_ROOT="."
if [ ! -d "./course_deals_backend" ] && [ -d "../course_deals_backend" ]; then
  WORKSPACE_ROOT=".."
fi

if [ -d "$WORKSPACE_ROOT/course_deals_backend" ]; then
  echo "=== Stopping Backend Gradle Daemons ==="
  (cd "$WORKSPACE_ROOT/course_deals_backend" && ./gradlew --stop) || true
fi

if [ -d "$WORKSPACE_ROOT/course_deals_ComposeMultiplatform" ]; then
  echo "=== Stopping KMP Gradle Daemons ==="
  (cd "$WORKSPACE_ROOT/course_deals_ComposeMultiplatform" && ./gradlew --stop) || true
fi
```

---

### Step 3: Terminate Orphaned JVM Wrappers, Ephemeral Test Servers & Desktop Runners

Clean up any remaining Kotlin compile daemons, orphaned `gradle-wrapper.jar` processes, lingering `:desktopApp` (`com.thanh0x.coursedeals.desktop.MainKt`) instances, ephemeral backend dev/test processes (`8080`, `8081`, `8082`), and temporary E2E screenshot staging files:

```bash
# 1. Kill any remaining GradleDaemon (across different JDKs like JBR-21 vs OpenJDK-17), KotlinCompileDaemon, Desktop MainKt, or orphaned gradle-wrapper processes
pkill -f "org.gradle.launcher.daemon.bootstrap.GradleDaemon" 2>/dev/null || true
pkill -f "org.jetbrains.kotlin.daemon.KotlinCompileDaemon" 2>/dev/null || true
pkill -f "com.thanh0x.coursedeals.desktop.MainKt" 2>/dev/null || true
pkill -f "gradle-wrapper.jar" 2>/dev/null || true

# 2. Stop ephemeral Spring Boot dev/test instances on 8080, 8081, 8082
for port in 8080 8081 8082; do
  pids=$(lsof -ti :$port 2>/dev/null || true)
  if [ -n "$pids" ]; then
    echo "Stopping process on port $port (PID: $pids)"
    kill -15 $pids 2>/dev/null || true
  fi
done

# 3. Clean temporary E2E screenshot directories
rm -rf /tmp/desktop-e2e 2>/dev/null || true
```

> [!NOTE]
> **What is Preserved by Default**:
> - **Booted Android Emulator (`emulator-5554`)** and **iOS Simulator (`SpringBoard`)** remain booted for fast subsequent runs unless the user explicitly asks for a deep shutdown (`adb -s emulator-5554 emu kill` / `xcrun simctl shutdown all`).
> - **Colima / Docker containers** (`training-coupon-mysql-local`, `training-coupon-redis-local`, `github-mcp-server`) remain running. However, if non-CourseDeals containers inside Colima are consuming excessive CPU (>50%), highlight them to the user via `docker stats --no-stream`.

---

### Step 4: Capture "After" System Stats Snapshot & Verify

Wait 2 seconds for JVM shutdown hooks to complete, then capture the post-cleanup snapshot:

```bash
sleep 2
echo "=== AFTER RELEASE-RESOURCE ==="
top -l 1 -n 0 | grep -E "^CPU|^Load|^PhysMem"
sysctl vm.swapusage
memory_pressure | grep "System-wide memory free percentage:" || true
echo "--- Remaining Java Processes ---"
jps -l 2>/dev/null || true
```

---

### Step 5: Report Resource Reclamation Table

Present a concise **Before vs. After Resource Reclamation Summary** in chat (and record in `NOTES.md` / `STATE.json` if part of a ticket flow):

```markdown
### 🧹 Resource Reclamation Summary (`release-resource`)

| Metric | Before Cleanup | After Cleanup | Reclaimed / Delta |
| :--- | :--- | :--- | :--- |
| **Physical RAM Used** | `<before_used> GB` (`<before_free> MB` free) | **`<after_used> GB` (`<after_free> MB` free)** | **`<delta_ram>` RAM freed** |
| **Memory Compressor** | `<before_comp>` | **`<after_comp>`** | **`<delta_comp>`** |
| **Swap Used** | `<before_swap> MB` | **`<after_swap> MB`** | **`<delta_swap>` swap freed** |
| **System Memory Free %** | `<before_pct>%` | **`<after_pct>%`** | **`+<delta_pct>%` headroom** |
| **CPU Usage** | `<before_busy>% busy` (`<before_idle>% idle`) | **`<after_busy>% busy` (`<after_idle>% idle`)** | **`<delta_cpu>%` CPU load** |
| **JVM / Gradle Processes** | `<before_jps_count>` processes | **`<after_jps_count>` processes** | **`<stopped_count>` JVMs stopped** |
```
