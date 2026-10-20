---
name: capture-evidence
description: Capture e2e/manual verification evidence, API responses, or service logs for a ticket, honestly reflecting what automated/manual verification was actually possible. Use after run-tests, standalone or as part of ticket-flow.
---

# Capture evidence

1. Determine verification type needed for the ticket:
   - **REST API endpoint**: Run the service locally (against `docker compose -f docker-compose.local.yml up -d`) and execute `curl -i` requests.
   - **Crawler / Background worker**: Trigger a test run or crawler cycle and capture service logs or database query verification.
   - **Database schema change**: Verify Flyway migration logs on application startup.
   - **Web / Swagger UI documentation**: Capture screenshot via browser or local tool if relevant.
2. Record verbatim outputs (`curl` status/response JSON, logs, DB query results) in the
   workspace file at `.agents/tickets/<issue-number>-<slug>/NOTES.md` under
   `## Test evidence` → `### E2E / manual verification`.
3. If screenshots are relevant (e.g. Swagger UI or HTML output), upload them to hosted
   storage using the Cloudflare R2 Worker if configured:
   ```bash
   worker_base="https://r2-image-worker.huythanh0x.workers.dev"
   response="$(curl -sS -X PUT "${worker_base}/upload" \
     -u "$R2_IMAGE_UPLOAD_USER:$R2_IMAGE_UPLOAD_TOKEN" \
     -F "image=@.agents/tickets/<issue-number>-<slug>/evidence/<name>.png")"
   ```
   Add resulting URLs under `### Screenshots`.
4. If local service verification is not feasible in this environment (e.g. missing Docker
   or local MySQL/Redis credentials), state so explicitly and ask the user to verify.
5. Append one line to Progress log: `<date> captured evidence — <what, or "asked user">`.

## Output

Report what evidence exists (API curl outputs, logs, or screenshot URLs) — `ship-pr` uses
this to fill in the PR's "How this was tested" section.
