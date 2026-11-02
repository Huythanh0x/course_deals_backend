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
- **GitHub Project**: `"Course Deals"` (`PVT_kwHOBEROFc4BhjJ2`, Project #4)
- **Sprint Iteration Field**: `Sprint` (`PVTIF_lAHOBEROFc4BhjJ2zhn_l1Q`)

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

As a solo developer supercharged with daily Antigravity multi-agent orchestration (`team-flow`), boilerplate generation, cross-repo contracts, automated unit tests, and emulator verifications occur rapidly and in parallel.

- **Recommended Weekly Velocity**: **30 – 40 story points** total per sprint (~5–7 points per day).
- **Fibonacci Point Scale**:
  - `size: 1`: Quick fix, documentation, minor config adjustment (~1-2 hours).
  - `size: 2`: Standard isolated endpoint or single-screen UI fix (~half day).
  - `size: 3`: Multi-table migration, new service logic, or complex ViewModel UI screen (~1 day).
  - `size: 5`: Full-stack cross-repo feature (e.g. new auth provider, offline sync flow) (~2-3 days).
  - `size: 8`: Large architectural change (must be split into sub-issues before sprint commitment).

---

## 3. Active Sprint Discovery & Auto-Assignment

To find the current active sprint on Project 4:
```bash
gh api graphql -f query='
query {
  node(id: "PVTIF_lAHOBEROFc4BhjJ2zhn_l1Q") {
    ... on ProjectV2IterationField {
      configuration {
        iterations {
          id
          title
          startDate
          duration
        }
      }
    }
  }
}
'
```

To assign an issue/PR to an iteration:
```bash
gh api graphql -f query='
mutation($projectId: ID!, $itemId: ID!, $fieldId: ID!, $iterationId: String!) {
  updateProjectV2ItemFieldValue(input: {
    projectId: $projectId,
    itemId: $itemId,
    fieldId: $fieldId,
    value: { iterationId: $iterationId }
  }) {
    projectV2Item { id }
  }
}
' -f projectId="PVT_kwHOBEROFc4BhjJ2" -f itemId="<PROJECT_ITEM_ID>" -f fieldId="PVTIF_lAHOBEROFc4BhjJ2zhn_l1Q" -f iterationId="<ITERATION_ID>"
```

---

## 4. Command: `sprint-flow plan` (Sunday Planning)

When invoked:
1. **Query Active / Next Iteration**: Determine current date and locate the target Sprint (e.g. Sprint 2).
2. **Review Incomplete Tickets from Previous Sprint**:
   - Query open tickets with previous sprint tag.
   - User choices: carry forward into new sprint or move to `"Backlog"`.
3. **Pull Candidate Backlog Items**:
   - Query unassigned backlog items labeled `priority: P0` or `priority: P1`.
   - Calculate total points using `size: N` labels (Cap at **30–40 points** max).
4. **Update Project Board**:
   - Move approved issues to `"Ready"` / `"Todo"` status.
   - Stamp each approved issue with the target `iterationId`.

---

## 5. Command: `sprint-flow retro` (Sunday Retrospective)

When invoked:
1. **Calculate Completed Points**:
   - Query all issues closed in the sprint's date window.
   - Sum story points (`size: N`) completed.
2. **Review Carried-Over / Blocked Work**:
   - Identify open tickets and reasons (CI failure, scope creep, blocked on external API).
3. **Produce Sprint Summary Table**:
   ```markdown
   ### Sprint N Summary
   - **Target Points**: 18
   - **Completed Points**: 16
   - **Carried Over**: 1 issue (2 pts)
   - **Velocity Trend**: Stable
   ```

---

## 6. Command: `sprint-flow hygiene` (Audit "No Sprint" Items)

To prevent items from floating on the board without a sprint:
1. Scan all board items where `Sprint` iteration value is null.
2. For closed items: Assign to `Sprint 0 (Foundation)` (if closed before Oct 26) or appropriate sprint by closed date.
3. For open items: Prompt user or automatically attach to the active sprint if scheduled for work.

---

## 7. Definition of Done (DoD) for Sprint Items

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
3. **Documentation**:
   - Product flows and route specs in `docs/product/` updated in the same PR.
