---
name: capture-evidence
description: Capture committed API verification test scripts, service logs, and deterministic evidence matching the ticket's concrete Definition of Done (DoD).
---

# Capture Evidence & Deterministic Verification

Verification must be reproducible, committed to version control, and mapped directly against the ticket's Definition of Done (DoD) matrix.

---

## 1. Backend Verification: Committed Executable Test Harness

For all REST API or backend changes, do **NOT** rely solely on ephemeral terminal copy-pasting.

1. Create a reproducible bash test script at `scripts/test-api/<issue-number>-<slug>.sh` (executable via `chmod +x`).
2. The script must:
   - Spin up or check local dependencies (`curl -s http://localhost:8080/actuator/health`).
   - Execute tests for each item in the ticket's DoD matrix (happy path, boundary conditions, invalid inputs, auth errors).
   - Assert HTTP status codes explicitly (e.g. `[[ "$status" == "200" ]] || exit 1`).
   - Validate response fields using `jq`.
   - Print clear PASS / FAIL output and exit with code 0 on complete success.
3. Commit this script into version control as part of the PR branch under `scripts/test-api/`.
4. Run the script and record its verbatim standard output in `.agents/tickets/<issue-number>-<slug>/NOTES.md` under `## Test evidence`.

---

## 2. Background Crawlers & JobRunr Verification

For crawler or background job tickets:
1. Trigger the background job via test controller or CLI.
2. Query and assert database state in MySQL (e.g. `SELECT id, status, external_url FROM coupon_deals WHERE ...`).
3. Verify Redis keys and JobRunr dashboard / database records.
4. Record exact SQL outputs and application logs under `## Test evidence`.

---

## 3. DoD Mapping Table in PR

Map every captured evidence item directly to the ticket's DoD matrix:

```markdown
### Verification Evidence vs. Definition of Done
| DoD Criterion | Status | Evidence / Committed Test |
|---|---|---|
| Happy Path 200 | PASSED | `scripts/test-api/<issue>.sh` test case 1 |
| Validation 400 Bad Request | PASSED | `scripts/test-api/<issue>.sh` test case 2 |
| Flyway Migration Clean | PASSED | `./gradlew test` (SchemaValidationTest) |
| Detekt / Ktlint Zero Violations | PASSED | `./gradlew ktlintCheck detekt` (0 issues) |
```

If screenshots of Swagger UI or administrative dashboards are relevant, upload them to Cloudflare R2 worker and embed the direct markdown links.
