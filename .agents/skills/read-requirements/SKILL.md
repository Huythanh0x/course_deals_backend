---
name: read-requirements
description: Read a GitHub issue's requirements, differentiate Bug reproduction vs Feature RFC research, establish a concrete testable DoD matrix, and record into the ticket workspace.
---

# Read Requirements & Intake Analysis

1. Locate `.agents/tickets/<issue-number>-<slug>/NOTES.md`. If it doesn't exist yet,
   create it from the template in `ticket-flow` / `team-flow`.
2. `gh issue view <N> -R Huythanh0x/course_deals_backend` — copy the
   **verbatim** body and labels into the workspace's Requirements section.
3. Note labels explicitly (type/priority/area/size) — `size: 13` requires splitting first.

---

## Differentiated Intake Pipeline: Bug vs. Feature

Inspect the issue labels (`type: bug` vs `type: feat` / `type: chore`):

### A. If Bug (`type: bug`) — SWE-Bench Reproduction Protocol
1. **Mandatory Reproduction First**:
   - Before modifying any source code, construct an executable reproduction test or script (`scripts/test-api/reproduce-<issue>.sh` or a JUnit test).
   - Execute it on the unmodified codebase and verify that it **fails** (`FAIL_TO_PASS` pattern).
   - If the bug cannot be replicated, STOP. Report the environment discrepancy to the user rather than guessing a fix.
2. **Root Cause Analysis (RCA)**:
   - Identify the exact line, query, concurrency race, or boundary failure causing the defect.
   - Post an RCA comment on the GitHub issue detailing:
     - Root cause explanation.
     - Reproduction script / test command and failure traceback.
     - Proposed fix approach.
     - Triage verdict: Code PR required, seed/migration fix required, or invalid/won't fix.

### B. If Feature (`type: feat` / `type: chore`) — Spec-Driven Development (SDD)
1. **Common Sense & Boundary Framing**:
   - Identify edge cases, invalid inputs, authorization constraints, rate limits, and network timeouts.
2. **External / Industry Research**:
   - Perform web search for standard implementations, reference API designs, RFC specs, or how peer platforms handle this flow.
3. **Architecture Spike & Tradeoff RFC**:
   - Document technical tradeoffs (e.g., storage schema, caching strategy, sync vs async JobRunr processing).
   - Lock the cross-repo REST / DTO JSON schema (HTTP verb, path, query, headers, status codes, request/response models).

---

## Establish Concrete Definition of Done (DoD) Matrix

Every issue must establish a clear, verifiable DoD matrix before implementation begins:

| DoD Criterion | Verification Method | Committed Artifact / Evidence |
|---|---|---|
| Happy path response 200 + valid schema | Executable bash script | `scripts/test-api/<issue>-<slug>.sh` exit code 0 |
| Invalid payload returns 400 + RFC7807 error | Executable bash script | `scripts/test-api/<issue>-<slug>.sh` assert 400 |
| Unauthorized / forbidden checks (401/403) | Executable bash script | `scripts/test-api/<issue>-<slug>.sh` assert 401 |
| DB persistence & Flyway migration integrity | Automated migration test | `./gradlew test` passes Flyway schema validation |
| Static analysis & code style | Gradle detekt & ktlint | `./gradlew ktlintCheck detekt` 0 violations |

Record this matrix directly in `.agents/tickets/<issue-number>-<slug>/NOTES.md`.
