---
name: capture-evidence
description: Capture multi-state UI screenshots, live emulator verification, and deterministic evidence mapped 1:1 against the ticket's concrete Definition of Done (DoD).
---

# Capture Evidence & Deterministic Verification

Verification must be multi-state, deterministic, and mapped directly against the ticket's Definition of Done (DoD) matrix.

---

## 1. Multi-State UI Verification (Avoid Blind Screenshots)

A single arbitrary screenshot is insufficient. Verification requires capturing evidence across multiple discrete states defined in the DoD:

1. **State 1: Initial / Empty State** (e.g. no items found, first-time user prompt).
2. **State 2: Populated / Success State** (with live data populated from the running local backend or mock server).
3. **State 3: Error / Offline / Boundary State** (e.g. network failure retry banner, validation error state).

---

## 2. Mandatory Visual Pixel Inspection (`read_file`) & Integrity Policy

> [!IMPORTANT]
> **Strict Zero-Assumption & Mandatory Pixel Inspection Policy**:
> 1. **Visual Pixel Inspection via `read_file`**: After capturing any screenshot via `screencap`, agents **MUST invoke `read_file`** on the `.png` image path to view and inspect actual pixels. Never claim a UI state is verified, rendered, or error-free without inspecting the image first.
> 2. **Catch Rendering / Connection Errors**: Verify that the screenshot actually displays the expected UI state (e.g. proper controls, populated list, or expected error dialog) rather than an unhandled connection crash, raw stacktrace, or missing view.
> 3. **Document Technical Constraints as Separate PR Comments**: If a specific UI state or activity cannot be driven or captured on device due to an operating system or platform technical constraint (e.g. non-exported activity without deeplink, biometric hardware limitation), **do NOT fabricate or claim visual verification**. Instead, document the exact technical constraint, steps attempted, and root cause in a separate, dedicated comment on the PR.

---

## 3. Emulator Capture & Cloudflare R2 Upload

1. Ensure `emulator-5554` is booted and application is installed via `./gradlew installLocalDebug`.
2. Navigate to target screens using ADB or interactive runner:
   ```bash
   adb exec-out screencap -p > .agents/tickets/<issue>-<slug>/evidence/<state-name>.png
   ```
3. **Inspect the captured screenshot pixels**:
   Invoke `read_file` on `.agents/tickets/<issue>-<slug>/evidence/<state-name>.png` and visually verify the rendered contents before uploading.
4. Upload each captured state to Cloudflare R2:
   ```bash
   worker_base="https://r2-image-worker.huythanh0x.workers.dev"
   response="$(curl -sS -X PUT "${worker_base}/upload" \
     -u "$R2_IMAGE_UPLOAD_USER:$R2_IMAGE_UPLOAD_TOKEN" \
     -F "image=@.agents/tickets/<issue>-<slug>/evidence/<state-name>.png")"
   ```
5. Extract the direct image URL from the JSON response (`${worker_base}/<filename>`) and verify it returns HTTP 200 (`curl -sS -o /dev/null -w '%{http_code}' "$url"`). Never use `pub-*.r2.dev` bucket URLs.

---

## 4. DoD Mapping Table in PR

Map every captured screenshot and test output directly to the ticket's DoD matrix:

```markdown
### Verification Evidence vs. Definition of Done
| DoD Criterion | Status | Evidence / Artifact |
|---|---|---|
| Empty State Rendered | PASSED | ![Empty State](https://r2-image-worker.huythanh0x.workers.dev/<hash>.png) |
| Populated Success State | PASSED | ![Success State](https://r2-image-worker.huythanh0x.workers.dev/<hash>.png) |
| Network Error / Retry State | PASSED | ![Error State](https://r2-image-worker.huythanh0x.workers.dev/<hash>.png) |
| Ktlint & Detekt Check | PASSED | `./gradlew ktlintCheck detekt` (0 issues) |
| Unit & Integration Tests | PASSED | `./gradlew test` (all passed) |
```
