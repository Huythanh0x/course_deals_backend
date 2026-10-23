# Course Deals - 1-Week Sprint Lifecycle Guide (Backend)

This guide documents the weekly engineering sprint cadence for Course Deals Backend development, optimized for a solo full-stack developer paired with Antigravity multi-agent orchestration.

---

## 1. Cadence & Rhythm

| Window | Focus | Key Actions |
|---|---|---|
| **Sunday (Stop & Plan)** | **Retro & Planning** | • Run `sprint-flow retro` to tally completed story points & debt.<br>• Run `sprint-flow plan` to select backlog items and set Iteration scope on GitHub Project.<br>• Review cross-repo REST / DTO contracts. |
| **Mon - Fri (Build & Test)** | **Execution (`team-flow`)** | • Pick highest-priority issue from GitHub Sprint iteration.<br>• Run `team-flow` to implement Spring Boot, Flyway, and JPA changes.<br>• Continuous CI validation and PR reviews. |
| **Saturday (Release & Hardening)** | **Verification & Staging** | • Full Dockerized backend smoke test (`docker compose up -d`).<br>• Verify active Redis sessions, Flyway schema consistency, and Swagger docs.<br>• Tag release or prepare staging deployment. |

---

## 2. GitHub Projects Integration

- **Board Name**: `"Course Deals"`
- **Iteration Field**: 1-week cycles (Monday 00:00 to Sunday 23:59).
- **Weekly Capacity**: **15 – 20 Story Points** (solo dev + multi-agent parallel execution).
- **Labeling Conventions**:
  - `type: feat` | `type: fix` | `type: chore` | `type: docs`
  - `priority: P0` (Blocker) | `priority: P1` (This Sprint) | `priority: P2` (Next Sprint) | `priority: P3` (Backlog)
  - `area: backend`
  - `size: 1` (~1-2h), `size: 2` (~half day), `size: 3` (~1 day), `size: 5` (~2-3 days), `size: 8` (split required).

---

## 3. Backend Definition of Done (DoD)

A backend ticket is considered **Done** when:
1. **Contract Locked**: Request/response DTO schemas match the cross-repo contract established in Checkpoint 1.
2. **Database & Migrations**: Any schema changes include a deterministic Flyway migration (`V<timestamp>__*.sql`) that applies cleanly without manual fixups.
3. **Automated Quality Checks**:
   ```bash
   ./gradlew ktlintCheck detekt test
   ```
   All tests pass with zero lint or static analysis violations.
4. **Local Verification**:
   - Application boots via `./gradlew bootRun` or Docker container.
   - Core endpoint verified via `curl -i` and HTTP status confirmed.
5. **Release PR**:
   - Pull Request opened with labeled tags (`area: backend`, linked issue `#N`).
   - CI build green.
   - Squash-merged to `main`.
