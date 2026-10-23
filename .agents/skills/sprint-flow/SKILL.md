---
name: sprint-flow
description: >-
  Manage the 1-week solo developer sprint lifecycle (Mon-Sat dev/test, Sunday stop & plan)
  using GitHub Projects and Antigravity. Use when the user asks to plan a sprint, run a sprint
  retro, check sprint backlog/velocity, or manage GitHub project iterations.
---

# Sprint Flow (`sprint-flow`)

Automates sprint planning, retrospectives, and backlog hygiene for Course Deals on a **1-week sprint cadence** (Monday 00:00 to Saturday 23:59 build & test, Sunday stop & planning).

Target GitHub Repositories & Project:
- **Backend repo**: `/Users/thanh0x/StudioProjects/course_deals_backend` (`Huythanh0x/course_deals_backend`)
- **Android / KMP repo**: `/Users/thanh0x/StudioProjects/course_deals_ComposeMultiplatform` (`Huythanh0x/course_deals_ComposeMultiplatform`)
- **GitHub Project**: `"Course Deals"`

---

## 1. Weekly Sprint Rhythm

| Phase | Window | Objective | Tool / Agent |
|---|---|---|---|
| **Sprint Retro & Demo** | Sunday morning / afternoon | Review completed vs carried-over issues, velocity, and tech debt. | `sprint-flow retro` |
| **Sprint Planning** | Sunday evening (e.g. 20:00) | Groom backlog, point tickets (15-20 pts max), lock iteration scope. | `sprint-flow plan` |
| **Daily Execution** | Monday – Friday | Implement full-stack or single-repo tickets in parallel. | `team-flow` |
| **Release & Hardening** | Saturday | Local backend boots, mobile emulator E2E smoke tests, deploy/tag. | `qa-release-engineer` |

---

## 2. Sprint Sizing & Capacity Budget

As a solo developer paired with Antigravity multi-agent orchestration (`team-flow`), boilerplate, DTO contracts, unit tests, and emulator runs are handled autonomously.

- **Recommended Weekly Velocity**: **15 – 20 story points** total per sprint.
- **Fibonacci Point Scale**:
  - `size: 1`: Quick fix, documentation, minor config adjustment (~1-2 hours).
  - `size: 2`: Standard isolated endpoint or single-screen UI fix (~half day).
  - `size: 3`: Multi-table migration, new service logic, or complex ViewModel UI screen (~1 day).
  - `size: 5`: Full-stack cross-repo feature (e.g. new auth provider, offline sync flow) (~2-3 days).
  - `size: 8`: Large architectural change (must be split into sub-issues before sprint commitment).

---

## 3. Command: `sprint-flow plan` (Sunday Planning)

When invoked:
1. **Query Active / Next Iteration**:
   Run GitHub CLI to fetch the project board and active iteration:
   ```bash
   gh project item-list 1 --owner Huythanh0x --format json
   ```
2. **Review Incomplete Tickets from Previous Sprint**:
   - List any issues still in `"In Progress"` or `"Todo"`.
   - Ask the user whether to carry over or de-prioritize to `"Backlog"`.
3. **Pull Candidate Backlog Items**:
   - Query unassigned backlog items labeled `priority: P0` or `priority: P1`.
   - Sum story points to ensure total does not exceed **20 points**.
4. **Contract & Spec Pre-check**:
   - For full-stack features, verify if cross-repo sub-issues are generated (Backend sub-issue + Android sub-issue).
5. **Update GitHub Project**:
   - Move approved issues to `"Sprint Backlog"` / `"Ready"` status and set the current `Iteration` field.

---

## 4. Command: `sprint-flow retro` (Sunday Retrospective)

When invoked:
1. **Calculate Completed Points**:
   - Scan all issues closed or merged in the sprint's date window.
   - Sum completed story points (`size: N`).
2. **Review Carried-Over / Blocked Work**:
   - Highlight any ticket that failed verification or emulator QA.
3. **Identify Architectural / Security Debt**:
   - Check if any ADR or Intent security warnings were flagged by `security-arch-reviewer`.
4. **Produce Sprint Summary Table**:
   ```markdown
   ### Sprint N Summary
   - **Target Points**: 18
   - **Completed Points**: 16
   - **Carried Over**: 1 issue (2 pts)
   - **Velocity Trend**: Stable
   ```

---

## 5. Definition of Done (DoD) for Sprint Items

An issue is only considered **Done** in the sprint when:
1. **Backend**:
   - Flyway migration validated & cleanly applied.
   - `./gradlew ktlintCheck detekt test` passes with zero violations.
   - Docker container spins up and curl verification passes.
   - Squash-merged to `main`.
2. **Android / KMP**:
   - Clean Architecture layers respected (`data` -> `domain` -> `presentation`).
   - `:app:detekt` and unit tests pass.
   - Live emulator-5554 verification captured (UI screenshots uploaded to Cloudflare R2).
   - Squash-merged to `main`.
