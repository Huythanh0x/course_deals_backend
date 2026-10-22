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

Unlike `ticket-flow` (which runs 1 agent sequentially with 8 manual stops), `team-flow` delegates specialized work to dedicated subagents in parallel and **only stops at 2 human-in-the-loop checkpoints**:
1. **Checkpoint 1 — Spec, API Contract & Edge Cases Approval** (before creating issues/branches and spawning coding agents)
2. **Checkpoint 2 — Ready to Merge** (after coding, cross-repo security/architecture audit, E2E/emulator evidence capture, and PR creation)

---

## The 5 Specialized Agent Roles

| Role | Agent Type / Name | Scope & Responsibility | Reference Prompt |
|---|---|---|---|
| **1. PM & Solutions Architect** | Main Agent + `research` subagents | Scans code in parallel, defines cross-repo REST/DTO contract, resolves edge cases, creates GitHub parent/sub-issues & branches, initializes `NOTES.md`. | Built into this `SKILL.md` |
| **2. Backend Engineer** | `backend-engineer` | Implements Spring Boot / Kotlin / Flyway / JobRunr changes & unit tests in `course_deals_backend`, runs `./gradlew ktlintCheck detekt test`, commits locally. | [references/backend-engineer.md](references/backend-engineer.md) |
| **3. Android / KMP Engineer** | `android-engineer` | Implements Clean Architecture (Retrofit, Room/DataStore, Hilt, ViewModel `StateFlow`, XML/Compose UI) & unit tests in `course_deals_ComposeMultiplatform`, runs `:app:detekt`, lint, and unit tests, commits locally. | [references/android-engineer.md](references/android-engineer.md) |
| **4. Security & Arch Reviewer** | `security-arch-reviewer` | Read-only auditor: verifies Backend↔Android DTO JSON contract parity, Android Intent security (`intent-security-review`), KMP readiness (`kmp-readiness`), and ADR criteria (`adr`). | [references/security-arch-reviewer.md](references/security-arch-reviewer.md) |
| **5. QA & Release Engineer** | `qa-release-engineer` | Runs sequential E2E verification (`docker compose` + `bootRun` + `curl -i` on Backend; `emulator-5554` + Cloudflare R2 screenshot upload on Android) and opens labeled PRs (`ship-pr`). | [references/qa-release-engineer.md](references/qa-release-engineer.md) |

---

## Phase 1: PM & Architect Discovery (Main Agent)

1. **Determine Repository Scope**:
   - **Backend-only**: touches only `/Users/thanh0x/StudioProjects/course_deals_backend`
   - **Android/KMP-only**: touches only `/Users/thanh0x/StudioProjects/course_deals_ComposeMultiplatform`
   - **Full-Stack (Cross-Repo)**: touches both repositories (e.g., new or updated API endpoint + client integration)

2. **Parallel Codebase Research (`scan-related-code`)**:
   - Invoke `invoke_subagent` with `TypeName: "research"` in parallel (one subagent per target repo) to:
     - Read the target repo's `AGENTS.md` (and existing GitHub issue if the user gave an issue `#N`).
     - Identify exact modules, files, entities, DTOs, ViewModels, or controllers impacted.
     - Identify existing patterns and potential edge cases.

3. **Draft the Spec, API Contract & Edge Cases (`confirm-edge-cases`)**:
   - **Cross-Repo API Contract** (required whenever both repos are involved or an API is touched):
     - Exact HTTP method & path (e.g., `GET /api/v1/user/profile`)
     - Request headers/query/body JSON schema & field nullability
     - Response JSON schema, field names, types, nullability, and status codes
   - **Edge Cases**:
     - For low-risk items with an obvious repo-consistent default: decide and record the default directly.
     - For genuine product/architectural ambiguities: batch them into a single `ask_question` call before or at Checkpoint 1.
   - **GitHub Issue & Branch Plan**:
     - Propose issue title(s), labels (`type: <x>`, `priority: <P0-P3>`, `area: <backend|android|...>`, `size: <1|2|3|5|8|13>`), and structure:
       - Single-repo task → 1 top-level issue on `"Course Deals"` board.
       - Full-stack task → 1 Parent umbrella issue on `"Course Deals"` board + Backend sub-issue (`area: backend`) + Android sub-issue (`area: android`), with sub-issues removed from the board cascade per `create-sub-issues`.

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

Once approved at Checkpoint 1:

1. **Create GitHub Issue(s) & Branches** (skip issue creation if the user provided existing issue number(s)):
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

Read the reference prompt(s) for the needed engineer role(s):
- Backend: [references/backend-engineer.md](references/backend-engineer.md)
- Android/KMP: [references/android-engineer.md](references/android-engineer.md)

1. **Define the Subagent(s)** using `define_subagent`:
   - `Name`: `"backend-engineer"` and/or `"android-engineer"`
   - `EnableWriteTools`: `true`
   - `SystemPrompt`: Use the system prompt from the corresponding reference file.
2. **Invoke in Parallel** using `invoke_subagent`:
   - If the task is Full-Stack, call `invoke_subagent` for `backend-engineer` and `android-engineer` **in the same turn** so both repositories are implemented and unit-tested simultaneously against the locked API Contract.
   - Pass each subagent:
     - Repository path and active branch name
     - Ticket workspace path (`.agents/tickets/<issue-number>-<slug>/NOTES.md`)
     - Exact API Contract and acceptance criteria
   - Wait for both subagents to complete and report their local commit hashes and unit test results.

---

## Phase 4: Automated Security & Architecture Review (`security-arch-reviewer`)

Read [references/security-arch-reviewer.md](references/security-arch-reviewer.md).

1. **Define & Invoke `security-arch-reviewer`**:
   - Call `define_subagent` with `Name: "security-arch-reviewer"`, `EnableWriteTools: false` (strictly read-only auditor).
   - Call `invoke_subagent` passing the repo path(s), branch name(s), `NOTES.md` path(s), and commit hash(es).
2. **Handle Review Verdict**:
   - The reviewer checks:
     1. **Cross-repo DTO/Endpoint parity** (field names, `@SerializedName`/`@JsonProperty`, nullability, HTTP methods/paths)
     2. **Android Intent security** (`AndroidManifest.xml` exported components, `getIntent()` extras sanitization, `PendingIntent.FLAG_IMMUTABLE`)
     3. **KMP readiness** (no unnecessary Android-only imports in `domain:*` or `data:*`)
     4. **ADR & code hygiene** (no stray debug logs, Flyway migration consistency, whether an ADR is needed)
   - If verdict is **`NEEDS_FIX`**: send the reviewer's exact findings back to `backend-engineer` and/or `android-engineer` via `send_message`, let them fix and amend/commit + re-run tests, and re-verify before proceeding.
   - If verdict is **`PASS`**: proceed immediately to Phase 5 without stopping for user input.

---

## Phase 5: Sequential E2E QA, Evidence Capture & PR Shipping (`qa-release-engineer`)

Read [references/qa-release-engineer.md](references/qa-release-engineer.md).

1. **Define & Invoke `qa-release-engineer`**:
   - Call `define_subagent` with `Name: "qa-release-engineer"`, `EnableWriteTools: true`.
   - Call `invoke_subagent` passing the repo path(s), branch name(s), issue number(s), and `NOTES.md` path(s).
2. **What `qa-release-engineer` executes**:
   - **Backend E2E (`course_deals_backend`)**: Starts local infra if needed, verifies endpoints via `curl -i` (or integration tests), records real HTTP responses in `NOTES.md`.
   - **Android E2E (`course_deals_ComposeMultiplatform`)**: Installs `localDebug` on `emulator-5554` (against local backend if full-stack), captures screenshots into `.agents/tickets/<N>-<slug>/evidence/`, uploads key screenshots to Cloudflare R2 (`https://r2-image-worker.huythanh0x.workers.dev/upload`), verifies 200 OK, and records markdown image URLs in `NOTES.md`.
   - **Ship PR (`ship-pr`)**: Pushes branch(es), creates PR(s) from `.github/PULL_REQUEST_TEMPLATE.md` with `Closes #<N>`, real test counts, `curl` outputs, and R2 screenshots, and mirrors issue labels onto the PR(s).

---

## Checkpoint 2: Ready to Merge Gate (Stop #2)

Once `qa-release-engineer` reports back with the opened PR URL(s):

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
