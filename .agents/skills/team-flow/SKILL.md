---
name: team-flow
description: >-
  Orchestrate a 5-agent specialized team (PM/Architect, Backend Engineer, Android/KMP Engineer,
  Security & Architecture Reviewer, QA & Release Engineer) across course_deals_backend and
  course_deals_ComposeMultiplatform with only 2 human-in-the-loop checkpoints. Use when the user
  describes a feature, bug, or ticket to build with the agent team, or asks to run team-flow.
---

# Course Deals Multi-Agent Team (`team-flow`)

Orchestrates a specialized 5-role engineering team across the two Course Deals repositories:
- **Backend repo**: `/Users/thanh0x/StudioProjects/course_deals_backend` (`Huythanh0x/course_deals_backend`)
- **Android / KMP repo**: `/Users/thanh0x/StudioProjects/course_deals_ComposeMultiplatform` (`Huythanh0x/course_deals_ComposeMultiplatform`)

## ⚡ The Golden Invariant: Clarify ➔ Ticket ➔ Implement

When the user gives a general or raw requirement (e.g. "Add Course Reports", "Offline Bookmarks", "Push Notifications"):
1. **NEVER start writing code or launching engineer subagents directly.**
2. **Clarify First (Pre-Planning via `spec-research`)**:
   - Run ambiguity triage across UX, Data/State, Security, and Edge Cases.
   - Use `ask_question` to resolve architectural forks and product ambiguities with the user.
   - Run competitive/best-practice research (`search_web`) if needed.
3. **Publish to GitHub Issue(s) & Initialize Workspace (`NOTES.md`)**:
   - Create parent issue and sub-issues on GitHub containing the locked API contract, architecture, and Definition of Done matrix.
   - Checkout branches `<type>/<issue-number>-<slug>`.
4. **Implement in Parallel**:
   - Only AFTER the issue is live and approved, launch `backend-engineer` and `android-engineer` subagents in parallel.

---

## The 5 Specialized Agent Roles

| Role | Agent Type / Name | Scope & Responsibility | Reference Prompt |
|---|---|---|---|
| **1. PM & Solutions Architect** | Main Agent + `research` subagents | Scans code in parallel, defines cross-repo REST/DTO contract, resolves edge cases, creates GitHub parent/sub-issues & branches, initializes `NOTES.md`. | Built into this `SKILL.md` |
| **2. Backend Engineer** | `backend-engineer` | Implements Spring Boot / Kotlin / Flyway / JobRunr changes & unit tests in `course_deals_backend`, runs `./gradlew ktlintCheck detekt test`, commits locally. | [references/backend-engineer.md](references/backend-engineer.md) |
| **3. Android / KMP Engineer** | `android-engineer` | Implements Clean Architecture (Retrofit, Room/DataStore, Hilt, ViewModel `StateFlow`, XML/Compose UI) & unit tests in `course_deals_ComposeMultiplatform`, runs `:app:detekt`, lint, and unit tests, commits locally. | [references/android-engineer.md](references/android-engineer.md) |
| **4. Security & Arch Reviewer** | `security-arch-reviewer` | Read-only auditor (runs in parallel with QA): verifies Backend↔Android DTO JSON contract parity, Android Intent security (`intent-security-review`), KMP readiness (`kmp-readiness`), and ADR criteria (`adr`). | [references/security-arch-reviewer.md](references/security-arch-reviewer.md) |
| **5. QA & Release Engineer** | `qa-release-engineer` | Runs live E2E verification: Backend `curl -i` check locally -> Android `emulator-5554` verification against running local backend + R2 screenshot upload -> opens labeled PRs (`ship-pr`). | [references/qa-release-engineer.md](references/qa-release-engineer.md) |

---

## Execution Workflow Overview

```
Phase 1: PM & Architect Discovery (Main Agent + parallel research subagents)
                   │
         [Checkpoint 1: Spec Approval]
                   │
Phase 2: Ticket & Workspace Setup (Branches & NOTES.md)
                   │
Phase 3: Parallel Implementation (backend-engineer & android-engineer run simultaneously)
                   ├───┐
                   │   │
Phase 4: Parallel Review & Backend QA
  • security-arch-reviewer: Audits DTO contract, intent security, KMP readiness
  • qa-release-engineer (Backend): docker compose up -> bootRun -> curl -i -> ship backend PR
                   │   │
                   └───┼───> Phase 5: Gated Mobile Live E2E & Release
                             (Triggered once BE is verified locally & Android is ready)
                             emulator-5554 -> installLocalDebug -> R2 upload -> ship Android PR
                                       │
                             [Checkpoint 2: Merge Gate]
```

---

## Phase 1: PM & Architect Discovery (Main Agent)

1. **Determine Repository Scope & Task Type**:
   - Scope: Backend-only, Android/KMP-only, or Full-Stack (Cross-Repo).
   - Task Type: **Bug** vs. **Feature / Chore**.

2. **Differentiated Discovery Protocol**:
   - **For Bugs (`type: bug`) — SWE-Bench Reproduction Protocol**:
     - Construct a reproduction script or failing test (`FAIL_TO_PASS` pattern).
     - Run on unmodified codebase to confirm reproduction.
     - Document Root Cause Analysis (RCA) comment on the GitHub issue before writing fix code.
   - **For Features (`type: feat` / `type: chore`) — Spec-Driven Development (SDD)**:
     - Clarify common-sense boundaries (empty states, errors, timeouts).
     - Conduct industry web research for reference patterns.
     - Formulate Architecture & Tradeoff RFC spike.

3. **Draft the Spec, Cross-Repo API Contract & Concrete DoD Matrix**:
   - **Cross-Repo API Contract** (REST path, HTTP verbs, request/response JSON schema, nullability, HTTP status codes).
   - **Concrete DoD Matrix**: Explicit verifiable criteria for happy path, error paths, migrations, and detekt checks.
   - **Issue Plan**: Propose issue titles, labels, and parent/sub-issue structure.

---

## Checkpoint 1: Spec & Plan Gate (Stop #1)

Present a concise **Team Execution Brief** to the user:
- **Scope**: Backend / Android / Full-Stack
- **Issue Plan**: Proposed issue title(s) and labels (or existing issue `#N`)
- **API Contract** (if applicable): Endpoint(s) and JSON DTO schema
- **Target Files**: Bullet list per repo from the research scan
- **Edge-Case Decisions**: Defaults chosen + any open questions

Then call `ask_question` with:
- **(Recommended) Approve plan — create issue(s) and launch Agent Team**
- **Adjust requirements or API contract** (user provides feedback via write-in)

Do **not** proceed to Phase 2 until the user approves.

---

## Phase 2: Ticket & Workspace Setup (Main Agent)

> [!CRITICAL]
> **HARD PREREQUISITE GATE FOR PHASE 3**:
> Under NO circumstances should `backend-engineer` or `android-engineer` subagents be defined or invoked before this phase is 100% complete!
> Every task MUST have an active GitHub Issue published with the full spec/API contract and confirmed issue URL, plus local `NOTES.md` initialized.

Once approved at Checkpoint 1:

1. **Create GitHub Issue(s) & Branches** (skip issue creation ONLY if the user provided existing issue number(s)):
   - Follow `new-ticket` and `create-sub-issues` in the target repo(s):
     - Top-level issue: `gh issue create -R Huythanh0x/<repo> --title "..." --label "type: ..." --label "priority: ..." --label "area: ..." --label "size: ..." --project "Course Deals" --body "..."`
     - Sub-issues (for full-stack parent + child issues): create parent (`--project "Course Deals"`) and both BE (`area: backend`) & App (`area: android`) sub-issues in `Huythanh0x/course_deals_ComposeMultiplatform`, link via the Sub-issues REST API, and run the GraphQL board auto-cascade cleanup so sub-issues don't clutter the `"Course Deals"` board.
   - In each target repo, checkout latest `main`, pull, and create the branch `<type>/<issue-number>-<slug>`:
     ```bash
     git -C <repo-path> checkout main && git -C <repo-path> pull origin main
     git -C <repo-path> checkout -b <type>/<issue-number>-<slug>
     ```
2. **Initialize Ticket Workspace (`NOTES.md`)**:
   - Create `.agents/tickets/<issue-number>-<slug>/NOTES.md` in each target repo populated with:
     - `## Requirements` (verbatim issue body + labels + locked API Contract)
     - `## Code scan` (files & modules from Phase 1)
     - `## Clarifications` (questions & answers from Checkpoint 1)
     - `## Decisions` (confirmed defaults)
     - `## Progress log` (initial entry)

---

## Phase 3: Parallel Engineering Execution (`backend-engineer` + `android-engineer`)

Read reference prompt(s):
- Backend: [references/backend-engineer.md](references/backend-engineer.md)
- Android/KMP: [references/android-engineer.md](references/android-engineer.md)

1. **Define Subagents** using `define_subagent` (`Name: "backend-engineer"` and `Name: "android-engineer"`, both with `EnableWriteTools: true`).
2. **Invoke Simultaneously**:
   - Invoke both subagents **in the same turn**.
   - Both engineers implement their domain and run their unit tests/linters independently against the locked contract.
   - Subagents commit their changes locally to their respective branches and report commit hashes and test results back.

---

## Phase 4: Parallel Review & Backend Local QA

As soon as commits are ready:
1. **Parallel Audit (`security-arch-reviewer`)**:
   - Defined with `EnableWriteTools: false`. Runs in parallel with QA.
   - Inspects cross-repo DTO contract parity, Android Intent security (`intent-security-review`), KMP readiness (`kmp-readiness`), and Flyway/ADR hygiene.
   - If verdict is `NEEDS_FIX`: fixes are routed back to the appropriate engineer immediately.
2. **Backend Local E2E & Committed Test Harness (`qa-release-engineer`)**:
   - If Backend has changes: starts local dependencies (`docker compose up -d`), runs Spring Boot application with `--spring.profiles.active=local`.
   - **Generates & commits executable test script** at `scripts/test-api/<issue>-<slug>.sh` asserting HTTP status codes and `jq` body schemas against the ticket's DoD matrix.
   - Executes the script, capturing output directly into the PR body.
   - Pushes backend branch and creates Backend PR with mirrored labels.
   - **Signals that Backend is ready for local API integration**.

---

## Phase 5: Gated Mobile Live E2E & Release (`qa-release-engineer`)

If `course_deals_ComposeMultiplatform` was modified:
1. **Gate Check**: Triggers only when:
   - Android engineering implementation is complete (`android-engineer` committed).
   - Backend local server is running and verified locally via committed test script (ready for live API integration).
2. **Live Mobile Verification & Multi-State Capture**:
   - Checks `adb devices` for `emulator-5554`.
   - Runs `./gradlew installLocalDebug`.
   - Captures multi-state UI evidence matching the DoD matrix (Empty, Success, Error states).
   - Uploads screenshots to Cloudflare R2 (`https://r2-image-worker.huythanh0x.workers.dev/upload`), verifies 200 OK, and embeds markdown URLs mapped directly to the DoD table in `NOTES.md`.
3. **Ship Android PR**:
   - Pushes Android branch and opens PR from `.github/PULL_REQUEST_TEMPLATE.md` with test evidence, R2 screenshots mapped to DoD, and mirrored issue labels.

---

## Checkpoint 2: Ready to Merge Gate (Stop #2)

Once `qa-release-engineer` reports back with the opened PR URL(s) and `security-arch-reviewer` passes:

1. Present the **Team Delivery Report**:
   - **PR URL(s)** & Branch(es)
   - **Agent Team Summary**:
     - `backend-engineer`: commit + ktlint/detekt/JUnit summary
     - `android-engineer`: commit + detekt/lint/JUnit summary
     - `security-arch-reviewer`: contract & security audit verdict
     - `qa-release-engineer`: E2E `curl` verification & embedded R2 screenshots
2. Call `ask_question` with:
   - **Merge PR(s) now (run `merge-pr` once CI is green)**
   - **Leave PR(s) open — I will review/merge manually**
   - **Rework something** (user specifies via write-in; route directly to the responsible subagent via `send_message`)
3. If the user selects **Merge PR(s) now**, follow the repo's `merge-pr` skill (verify CI checks green, squash-merge with clean Conventional Commits subject, verify branch auto-deleted and issue auto-closed, clean up local branch).
