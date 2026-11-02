---
name: team-flow
description: >-
  Orchestrate a 5-agent specialized team (PM/Architect, Backend Engineer, Android/KMP Engineer,
  Security & Architecture Reviewer, QA & Release Engineer) across course_deals_backend and
  course_deals_ComposeMultiplatform with only 2 human-in-the-loop checkpoints. Use when the user
  describes a feature, bug, or ticket to build with the agent team, or asks to run team-flow.
---

# Course Deals Multi-Agent Team (`team-flow`)

Orchestrates a specialized 5-role engineering team across the two Course Deals repositories (relative to workspace root):
- **Backend repo**: `course_deals_backend` (`./course_deals_backend`) — (`Huythanh0x/course_deals_backend`)
- **Android / KMP repo**: `course_deals_ComposeMultiplatform` (`./course_deals_ComposeMultiplatform`) — (`Huythanh0x/course_deals_ComposeMultiplatform`)

> [!IMPORTANT]
> **Portability Rule: All Repository Paths Must Be Relative**:
> Never use machine-specific absolute paths (e.g. `/Volumes/MacExt/...` or `/Users/...`). All repository references, working directories, ticket files, and script paths MUST be relative to the active workspace root (`./course_deals_backend`, `./course_deals_ComposeMultiplatform`) or relative to the respective repository root.

## ⚡ The Golden Invariants

### 1. Clarify ➔ Ticket ➔ Implement
When the user gives a general or raw requirement (e.g. "Add Course Reports", "Offline Bookmarks", "Push Notifications"):
1. **NEVER start writing code or launching engineer subagents directly.**
2. **Clarify First (Pre-Planning via `spec-research`)**:
   - Run ambiguity triage across UX, Data/State, Security, and Edge Cases.
   - Define dedicated test scenarios across **all 4 quadrants**: Happy Path, Failed Path, Error Path, and Related Changes.
   - Define structured **Use Cases (UCs)** and the **GUI Framework Test Specification** (Maestro E2E flows) for the feature.
   - Use `ask_question` to resolve architectural forks and product ambiguities with the user.
   - Run competitive/best-practice research (`search_web`) if needed.
3. **Publish to GitHub Issue(s) & Initialize Workspace Memory (`STATE.json` & `NOTES.md`)**:
   - Create parent issue and sub-issues on GitHub containing the locked API contract, Use Cases, 4-quadrant test scenarios, GUI framework test plan, and Definition of Done matrix.
   - Stamp the active sprint onto the issue.
   - Checkout branches `<type>/<issue-number>-<slug>`.
4. **Implement in Parallel**:
   - Only AFTER the issue is live and approved, launch `backend-engineer` and `android-engineer` subagents in parallel.

### 2. Living Ticket Memory & Step Resumption: Never Lose Context Across Sessions
- Every ticket maintains a living state machine in `.agents/tickets/<issue-number>-<slug>/STATE.json` and `NOTES.md`.
- **Fast-Forward on Resume**: When starting or resuming a session, check `STATE.json`. Skip already completed and approved steps; resume precisely from the first pending or interrupted step.
- **Continuous Step Injection**: If the user introduces an adjustment, rework, or adds an extra step mid-stream, trace back the memory log, perform delta analysis, inject the new step into `STATE.json` without discarding completed work, and proceed smoothly.

---

## The 5 Specialized Agent Roles

| Role | Agent Type / Name | Scope & Responsibility | Reference Prompt |
|---|---|---|---|
| **1. PM & Solutions Architect** | Main Agent + `research` subagents | Scans code in parallel; defines cross-repo REST/DTO contract; specifies **4-quadrant test scenarios** (Happy, Failed, Error, Related Changes), structured **Use Cases**, and **GUI framework test specification** (Maestro); creates GitHub parent/sub-issues, stamps active sprint & initializes `NOTES.md`. | Built into this `SKILL.md` |
| **2. Backend Engineer** | `backend-engineer` | Implements Spring Boot / Kotlin / Flyway / JobRunr changes & unit/integration tests (covering happy, failed, and error paths) in `course_deals_backend`, runs `./gradlew ktlintCheck detekt test`, commits locally. | [references/backend-engineer.md](references/backend-engineer.md) |
| **3. Android / KMP Engineer** | `android-engineer` | Implements Clean Architecture in `course_deals_ComposeMultiplatform` across shared KMP/Android/iOS modules, exposes UI testability (semantic tags/labels for Maestro), handles multi-state UI (empty/success/error), runs `:app:detekt`, lint, and unit tests, commits locally. | [references/android-engineer.md](references/android-engineer.md) |
| **4. Security & Arch Reviewer** | `security-arch-reviewer` | Read-only auditor (runs in parallel with QA): verifies Backend↔Client DTO JSON contract parity, Android Intent security (`intent-security-review`), KMP readiness (`kmp-readiness`), ADR criteria (`adr`), and verifies 4-quadrant test coverage. | [references/security-arch-reviewer.md](references/security-arch-reviewer.md) |
| **5. QA & Release Engineer** | `qa-release-engineer` | Runs live E2E verification: Backend `curl -i` / test script locally ➔ **Cross-platform Kotlin Multiplatform (Android + iOS) GUI framework tests (`maestro`)** ➔ visual pixel inspection (`read_file`) ➔ Cloudflare R2 uploads ➔ **Explicit Side-by-Side Comparison Matrix (Android vs. iOS)** ➔ opens labeled PRs (`ship-pr`). | [references/qa-release-engineer.md](references/qa-release-engineer.md) |

---

## Execution Workflow Overview

```
Phase 1: PM & Architect Discovery (Main Agent + parallel research subagents)
   • 4-Quadrant Test Scenarios (Happy, Failed, Error, Related Changes)
   • Structured Use Cases (UC-01, UC-02...)
   • GUI Framework Test Specification (Maestro flows for Android & iOS)
   • Cross-Repo API Contract & Cross-Platform DoD Matrix
                    │
         [Checkpoint 1: Spec Approval]
                    │
Phase 2: Ticket & Workspace Setup (Branches, Project Sprint Stamping & NOTES.md)
                    │
Phase 3: Parallel Implementation (backend-engineer & android-engineer run simultaneously)
                    ├───┐
                    │   │
Phase 4: Parallel Review & Backend QA
  • security-arch-reviewer: Audits DTO contract, intent security, KMP readiness, 4-quadrant coverage
  • qa-release-engineer (Backend): docker compose up -> bootRun -> curl -i / test script -> ship backend PR
                    │   │
                    └───┼───> Phase 5: Gated Cross-Platform Mobile Live E2E & Release
                              (Triggered once BE is verified locally & Mobile is ready)
                              • Run GUI framework tests (Maestro) on BOTH Android & iOS
                              • Android (emulator-5554) + iOS (iPhone Simulator)
                              • Visual pixel inspection (read_file) & R2 upload
                              • Side-by-side comparison matrix (Android vs. iOS)
                              • Ship Mobile PR with comparative evidence
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

3. **Define Dedicated Test Scenarios Across 4 Quadrants**:
   Before drafting tickets or writing code, the PM must explicitly specify test scenarios in all 4 categories:
   - **Quadrant 1: Happy Path Scenarios**:
     - The ideal end-to-end user and system journey.
     - Valid inputs, expected successful API responses (200/201), correct state transitions, UI rendering, and database persistence.
   - **Quadrant 2: Failed Path Scenarios (Domain & Business Validation)**:
     - Expected business rejections, validation failures, and permission denials (e.g. invalid OTP, expired coupon, duplicate submission, password mismatch, invalid email format, unauthorized 401/403, rate limiting 429).
     - Inline validation messages, error banners, input focus reset, and state recovery.
   - **Quadrant 3: Error Path Scenarios (System, Network & Environmental)**:
     - Unexpected infrastructure failures, network offline, connection timeouts, backend 500/502/503, malformed JSON response, missing fields, or service crashes.
     - Loading timeouts, empty states, offline caching fallbacks, retry CTAs, and crash resilience.
   - **Quadrant 4: Related Changes & Regression Scenarios**:
     - Impact on surrounding screens/components, shared DTOs/models, navigation routes, bottom bar tabs, cache invalidation (Room/Redis), session state, dark mode, system insets, and backward compatibility.

4. **Define Structured Use Cases (UCs)**:
   Map the feature into structured Use Cases:
   - **ID & Title**: e.g., `UC-01: Passwordless OTP Authentication`
   - **Actor & Preconditions**: User role, authentication state, initial app screen.
   - **Trigger & User Actions**: User taps button, submits input, switches mode.
   - **Primary Scenario (Happy Path)**: Step-by-step nominal flow with expected outputs.
   - **Alternative & Exception Scenarios**: Failed and error paths handled gracefully.
   - **Postconditions & State Changes**: Persisted credentials, modified state, navigation target.

5. **Define GUI Framework Test Specification for the Feature**:
   The PM must define the exact **GUI framework test specification** (specifically **Maestro E2E flows** under `.maestro/flows/<feature>.yaml`) covering the Use Cases:
   - User interactions: `tapOn`, `inputText`, `scrollUntilVisible`, `eraseText`.
   - Accessibility & semantic selectors required on UI components (`Modifier.testTag`, content descriptions, text labels).
   - Assertion checkpoints: `assertVisible`, `assertNotVisible`, `extendedWaitUntil`.
   - Visual capture checkpoints: `takeScreenshot` for each critical state.
   - This test specification ensures engineers build UI with testable semantics and the QA agent executes deterministic GUI tests on both Android and iOS.

6. **Draft Cross-Repo API Contract & Cross-Platform DoD Matrix**:
   - **Cross-Repo API Contract** (REST path, HTTP verbs, request/response JSON schema, nullability, HTTP status codes).
   - **Cross-Platform DoD Matrix**: Explicit verifiable criteria for happy path, failed path, error path, migrations, detekt/lint checks, and **product documentation updates** (updating affected `docs/product/` user flows or route specs).
   - **Issue Plan**: Propose issue titles, labels, and parent/sub-issue structure.

---

## Checkpoint 1: Spec & Plan Gate (Stop #1)

Present a concise **Team Execution Brief** to the user:
- **Scope**: Backend / Android / Full-Stack
- **Issue Plan**: Proposed issue title(s) and labels (or existing issue `#N`)
- **Use Cases & 4-Quadrant Test Scenarios**: Summary of Happy, Failed, Error, and Related scenarios
- **GUI Framework Test Plan**: Maestro E2E flow outline for Android and iOS
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
     - **Top-level feature issue**: MUST be added to GitHub Project [`Course Deals`](https://github.com/users/Huythanh0x/projects/4) (`--project "Course Deals"`). The issue body MUST include the **solution architecture plan** (problem & motivation, Use Cases, 4-quadrant test scenarios, GUI framework test specification, API/DTO contract, and DoD matrix).
       ```bash
       gh issue create -R Huythanh0x/<repo> --title "..." --label "type: ..." --label "priority: ..." --label "area: ..." --label "size: ..." --project "Course Deals" --body "..."
       ```
     - **Mandatory Active Sprint Stamping**: Immediately after creating the issue (or linking an existing one), stamp it to the currently active sprint on `Sprint` field (`PVTIF_lAHOBEROFc4BhjJ2zhn_l1Q`) so no board item is left as "No Sprint":
       ```bash
       gh api graphql -f query='
         mutation($projectId: ID!, $itemId: ID!, $fieldId: ID!, $iterationId: String!) {
           updateProjectV2ItemFieldValue(input: {
             projectId: $projectId,
             itemId: $itemId,
             fieldId: $fieldId,
             value: { iterationId: $iterationId }
           }) { projectV2Item { id } }
         }
       ' -f projectId="PVT_kwHOBEROFc4BhjJ2" -f itemId="<PROJECT_ITEM_ID>" -f fieldId="PVTIF_lAHOBEROFc4BhjJ2zhn_l1Q" -f iterationId="<ACTIVE_ITERATION_ID>"
       ```
     - **Sub-issues (for full-stack parent + child issues)**: Create parent (`--project "Course Deals"`) and both BE (`area: backend`) & App (`area: android`) sub-issues in `Huythanh0x/course_deals_ComposeMultiplatform` (or target repos). Each sub-issue body **MUST** specify its own concrete, domain-specific architecture and tech stack details (e.g. Spring Boot/Flyway/JPA/JobRunr for BE; Jetpack Compose/Clean Architecture/Hilt/Coroutines for App). Link via the Sub-issues REST API, and run the GraphQL board auto-cascade cleanup so sub-issues don't clutter the `"Course Deals"` board.
   - In each target repo, checkout latest `main`, pull, and create the branch `<type>/<issue-number>-<slug>`:
     ```bash
     git -C <repo-path> checkout main && git -C <repo-path> pull origin main
     git -C <repo-path> checkout -b <type>/<issue-number>-<slug>
     ```
2. **Initialize Ticket Workspace Memory (`STATE.json` & `NOTES.md`)**:
   - In each target repo, create `.agents/tickets/<issue-number>-<slug>/`:
     - **`STATE.json`**: Machine-readable state machine storing current phase, step-by-step progress, approval flags, and memory trace:
       ```json
       {
         "ticket": {
           "issue_number": "<issue-number>",
           "slug": "<slug>",
           "title": "<title>",
           "branch": "<type>/<issue-number>-<slug>",
           "scope": "backend | android | full-stack"
         },
         "current_phase": "PHASE_2_SETUP",
         "active_step": "STEP_1_INIT",
         "phases": {
           "PHASE_1_DISCOVERY": { "status": "COMPLETED", "completed_at": "<ISO-timestamp>" },
           "CHECKPOINT_1_SPEC_APPROVAL": { "status": "APPROVED", "approved_at": "<ISO-timestamp>" },
           "PHASE_2_SETUP": { "status": "COMPLETED", "completed_at": "<ISO-timestamp>" },
           "PHASE_3_IMPLEMENTATION": { "status": "PENDING" },
           "PHASE_4_REVIEW": { "status": "PENDING" },
           "PHASE_5_MOBILE_E2E": { "status": "PENDING" },
           "CHECKPOINT_2_MERGE_APPROVAL": { "status": "PENDING" }
         },
         "steps_log": [
           { "step_id": "STEP_1", "title": "...", "status": "PENDING", "approved": false, "evidence": {} }
         ],
         "memory_trace": [
           { "timestamp": "<ISO-timestamp>", "event": "INIT", "details": "Ticket initialized" }
         ]
       }
       ```
     - **`NOTES.md`**: Human-readable living documentation containing:
       - `## Requirements` (verbatim issue body + labels + locked API Contract)
       - `## Use Cases & Test Scenarios` (Happy, Failed, Error, Related Changes)
       - `## GUI Framework Test Specification` (Maestro flow steps & assertions)
       - `## Step Progress & Approvals Tracker` (table tracking status & approvals)
       - `## Memory Trace & Injected Changes Log` (record of incremental adjustments)
       - `## Code scan` (files & modules from Phase 1)
       - `## Clarifications & Decisions` (questions, answers, approved choices)
       - `## Progress log` (historical chronological log)

---

## 🧠 Persistent Ticket Memory, Resumption & Continuous Step Evolution

### 1. Fast-Forward Resumption (Checking Memory First)
Whenever an agent starts or resumes `/team-flow` for an ongoing ticket:
1. **Always inspect `.agents/tickets/<issue-number>-<slug>/STATE.json` first.**
2. Check `phases` and `steps_log`:
   - If a phase or step is marked `"COMPLETED"` or `"DONE"` with `"approved": true`, **DO NOT re-execute it**.
   - Announce the resumed state:
     > *"Resuming ticket #<N>. Steps 1 through K already verified and approved. Fast-forwarding directly to Step X..."*
   - Jump directly to the current `active_step` or first `"PENDING"` step.

### 2. Continuous Step Injection (Mid-Flight Changes & Tracing Back Memory)
When the user adds new requirements, adjusts an edge case, or introduces a new step (e.g. *"Also add step 6 for biometric fallback"* or *"Rework the error state in step 2"*):
1. **Trace Back the Memory**:
   - Read `STATE.json` and `NOTES.md` to review the established dependency graph, completed steps, and user approvals.
2. **Delta Analysis**:
   - Identify unaffected completed steps: keep their status as `"DONE"` and preserve their test evidence.
   - Identify impacted steps: mark them as `"NEEDS_REWORK"`.
   - Insert new step(s) into `steps_log` (e.g. `STEP_2B`, `STEP_6`).
3. **Log the Event in `memory_trace`**:
   - Append an entry to `memory_trace` in `STATE.json` and `## Memory Trace & Injected Changes Log` in `NOTES.md`:
     ```json
     {
       "timestamp": "<ISO-timestamp>",
       "event": "STEP_ADDED",
       "details": "User injected Step 6: Biometric fallback handling. Unaffected steps 1..5 preserved."
     }
     ```
4. **Continuously Apply and Execute**:
   - Seamlessly launch or message the relevant subagent (`backend-engineer`, `android-engineer`, or `qa-release-engineer`) to implement and verify ONLY the newly injected or reworked steps.
   - Update evidence and PR documentation incrementally.

---

## Phase 3: Parallel Engineering Execution (`backend-engineer` + `android-engineer`)

Read reference prompt(s):
- Backend: [references/backend-engineer.md](references/backend-engineer.md)
- Android/KMP: [references/android-engineer.md](references/android-engineer.md)

1. **Define Subagents** using `define_subagent` (`Name: "backend-engineer"` and `Name: "android-engineer"`, both with `EnableWriteTools: true`).
2. **Invoke Simultaneously**:
   - Invoke both subagents **in the same turn**.
   - **Backend Engineer**: Implements endpoints, services, repositories, and unit/integration tests covering happy, failed, and error scenarios.
   - **Android / KMP Engineer**: Implements Clean Architecture in Compose Multiplatform across shared KMP/Android/iOS modules, adds semantic test tags / labels conforming to the GUI framework test specification, handles empty/success/error UI states, and verifies unit tests and linters.
   - Subagents commit their changes locally to their respective branches and report commit hashes and test results back.

---

## Phase 4: Parallel Review & Backend Local QA

As soon as commits are ready:
1. **Parallel Audit (`security-arch-reviewer`)**:
   - Defined with `EnableWriteTools: false`. Runs in parallel with QA.
   - Inspects cross-repo DTO contract parity, Android Intent security (`intent-security-review`), KMP readiness (`kmp-readiness`), Flyway/ADR hygiene, AND verifies that all 4 scenario quadrants (happy, failed, error, regression) are covered.
   - If verdict is `NEEDS_FIX`: fixes are routed back to the appropriate engineer immediately.
2. **Backend Local E2E & Committed Test Harness (`qa-release-engineer`)**:
   - If Backend has changes: starts local dependencies (`docker compose up -d`), runs Spring Boot application with `--spring.profiles.active=local`.
   - **Generates & commits executable test script** at `scripts/test-api/<issue>-<slug>.sh` asserting HTTP status codes (200, 400, 404, etc.) and `jq` body schemas against the ticket's DoD matrix.
   - Executes the script, capturing output directly into the PR body.
   - Pushes backend branch and creates Backend PR with mirrored labels.
   - **Signals that Backend is ready for local API integration**.

---

## Phase 5: Gated Cross-Platform Mobile Live E2E & Release (`qa-release-engineer`)

If `course_deals_ComposeMultiplatform` was modified:
1. **Gate Check**: Triggers only when:
   - Android/KMP engineering implementation is complete (`android-engineer` committed).
   - Backend local server is running and verified locally via committed test script (ready for live API integration on port 8081).
2. **Execute Automated GUI Framework Tests with Embedded Screenshot Capture (Maestro)**:
   - The declarative Maestro test flow (`.maestro/flows/<flow>.yaml`) **MUST declare step-by-step `- takeScreenshot: <step_name>` checkpoints** for every significant interaction and screen state transition along the user journey.
   - Run the automated suite on **BOTH platforms**:
     ```bash
     # Android Emulator (emulator-5554)
     ~/.maestro/bin/maestro --device emulator-5554 test -e APP_ID=com.thanh0x.coursedeals .maestro/flows/<flow>.yaml

     # iOS Simulator (booted iPhone Simulator)
     ~/.maestro/bin/maestro --device booted test -e APP_ID=com.thanh0x.coursedeals.iosApp .maestro/flows/<flow>.yaml
     ```
   - (Or capture step screenshots directly via `adb exec-out screencap` and `xcrun simctl io booted screenshot` if augmenting the automated runner).
3. **Mandatory Visual Pixel Inspection (`read_file`)**:
   - Agent **MUST invoke `read_file`** on every captured `.png` image across both Android and iOS before uploading to verify that:
     - All text, headers, and buttons are crisply rendered without truncation or missing strings.
     - System bar insets (status bar, dynamic island/notch, gesture navigation bar) are respected without overlaps.
     - Expected validation errors, dialogs, or populated feeds are properly rendered.
4. **Cloudflare R2 Upload**:
   - Upload each step screenshot to Cloudflare R2 worker (`https://r2-image-worker.huythanh0x.workers.dev/upload`), verify HTTP 200, and construct public URLs.
5. **Feature-Rich Step-by-Step Flow Evidence in GitHub PR**:
   - The GitHub PR **MUST NOT** contain a few isolated or unstructured screenshots.
   - It **MUST** present a **feature-rich, step-by-step user journey walkthrough** comparing Android and iOS side-by-side using `<img src="..." width="300" />` (or markdown image links):
     ```markdown
     ## Screenshots

     | Step | Android Evidence (`emulator-5554`) | iOS Evidence (`iPhone Simulator`) |
     | :--- | :---: | :---: |
     | **1. Unauthenticated Login Screen**<br>• Clean email input<br>• "Continue with Email" CTA<br>• Social & Guest auth options | <img src="<android_url>" width="300" /> | <img src="<ios_url>" width="300" /> |
     | **2. OTP Verification Screen**<br>• Sent-to email recipient notification<br>• 6-digit code input & "Verify OTP"<br>• "Change Email" and "Resend Code" | <img src="<android_url>" width="300" /> | <img src="<ios_url>" width="300" /> |
     | **3. Terms & Privacy Consent Dialog**<br>• Triggered for new user onboarding<br>• "Agree & Continue" & "Decline" actions<br>• Native Compose overlay | <img src="<android_url>" width="300" /> | <img src="<ios_url>" width="300" /> |
     | **4. Authenticated Home Screen**<br>• Navigated upon consent<br>• Live deals served from backend port 8081 | <img src="<android_url>" width="300" /> | <img src="<ios_url>" width="300" /> |
     | **5. Profile & Settings**<br>• Verified email displayed<br>• Feature flags respected (Fingerprint hidden)<br>• Dark / Light mode parity | <img src="<android_url>" width="300" /> | <img src="<ios_url>" width="300" /> |
     ```
   - Actively audits for discrepancies (insets clipping, dialog differences, theme discrepancies). If any defect is found, logs it and routes back to `android-engineer` for resolution before shipping.
6. **Ship Android/KMP PR**:
   - Pushes mobile branch and opens PR from `.github/PULL_REQUEST_TEMPLATE.md` with:
     - Feature-rich step-by-step walkthrough table comparing Android vs. iOS
     - Maestro E2E test execution results on both platforms
     - Mirrored issue labels and `Closes #<N>` reference.

---

## Checkpoint 2: Ready to Merge Gate (Stop #2)

Once `qa-release-engineer` reports back with the opened PR URL(s) and `security-arch-reviewer` passes:

1. Present the **Team Delivery Report**:
   - **PR URL(s)** & Branch(es)
   - **Cross-Platform Parity Summary**: Android vs. iOS comparison verdict
   - **Agent Team Summary**:
     - `backend-engineer`: commit + ktlint/detekt/JUnit summary
     - `android-engineer`: commit + detekt/lint/JUnit summary
     - `security-arch-reviewer`: contract, security & 4-quadrant audit verdict
     - `qa-release-engineer`: Maestro E2E runs, Backend test script output & embedded side-by-side R2 screenshots
2. Call `ask_question` with:
   - **Merge PR(s) now (run `merge-pr` once CI is green)**
   - **Leave PR(s) open — I will review/merge manually**
   - **Rework something** (user specifies via write-in; route directly to the responsible subagent via `send_message`)
3. If the user selects **Merge PR(s) now**, follow the repo's `merge-pr` skill (verify CI checks green, squash-merge with clean Conventional Commits subject, verify branch auto-deleted and issue auto-closed, clean up local branch).
