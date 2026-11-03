---
name: team-flow
description: >-
  Orchestrate a 7-role specialized engineering team (PM/Architect, Backend Engineer, KMP Engineer,
  Apple Developer [iOS & watchOS], Desktop CMP Engineer, Security & Architecture Reviewer,
  QA & Release Engineer) across course_deals_backend and course_deals_ComposeMultiplatform with
  up-to-date GitHub skill references and only 2 human-in-the-loop checkpoints. Use when the user
  describes a feature, bug, or ticket to build with the agent team, or asks to run team-flow.
---

# Course Deals Multi-Agent Team (`team-flow`)

Orchestrates a specialized 7-role engineering team across the two Course Deals repositories (relative to workspace root):
- **Backend repo**: `course_deals_backend` (`./course_deals_backend`) — (`Huythanh0x/course_deals_backend`)
- **KMP / Multiplatform repo (Android, iOS, watchOS, Desktop)**: `course_deals_ComposeMultiplatform` (`./course_deals_ComposeMultiplatform`) — (`Huythanh0x/course_deals_ComposeMultiplatform`)

> [!IMPORTANT]
> **Portability Rule: All Repository Paths Must Be Relative**:
> Never use machine-specific absolute paths (e.g. `/Volumes/MacExt/...` or `/Users/...`). All repository references, working directories, ticket files, and script paths MUST be relative to the active workspace root (`./course_deals_backend`, `./course_deals_ComposeMultiplatform`) or relative to the respective repository root.

## ⚡ The Golden Invariants

### 1. Clarify ➔ Ticket ➔ Implement
When the user gives a general or raw requirement (e.g. "Add Course Reports", "Offline Bookmarks", "Apple Watch Deal Complications", "Push Notifications"):
1. **NEVER start writing code or launching engineer subagents directly.**
2. **Clarify First (Pre-Planning via `spec-research`)**:
   - Run ambiguity triage across UX, Data/State, Security, and Edge Cases.
   - Define dedicated test scenarios across **all 4 quadrants**: Happy Path, Failed Path, Error Path, and Related Changes.
   - Define structured **Use Cases (UCs)** and the **GUI Framework Test Specification** (Maestro E2E flows / XCTest / Compose UI Test) for the feature.
   - Use `ask_question` to resolve architectural forks and product ambiguities with the user.
   - Run competitive/best-practice research (`search_web`) if needed.
3. **Synchronize GitHub Issue Description & Propose Plan (Stop #1)**:
   - Update existing issue description (`gh issue edit <id> --body "..."`) or create the top-level issue containing the solution architecture plan, Use Cases, 4-quadrant test scenarios, Maestro test plan, and DoD matrix.
   - Render the complete proposal brief in the visible chat response and create the `TEAM_EXECUTION_BRIEF.md` artifact before requesting approval.
4. **Publish Workspace Memory & Multi-Platform Sub-Issues (Phase 2)**:
   - Stamp the active sprint onto the parent issue.
   - Create platform sub-issues under the parent issue via `create-sub-issues`:
     - `+ Android` (Mandatory for client features — owned by `kmp-engineer`)
     - `+ iOS` (Mandatory for client features — owned by `apple-developer` & `kmp-engineer`)
     - `+ WatchOS` (if needed — owned by `apple-developer`)
     - `+ BE` (if needed — owned by `backend-engineer`)
     - `+ Desktop` (if needed — owned by `desktop-engineer`)
   - Checkout branches `<type>/<issue-number>-<slug>`.
   - Initialize `.agents/tickets/<issue-number>-<slug>/STATE.json` and `NOTES.md`.
5. **Implement in Parallel (Phase 3)**:
   - Only AFTER the parent + sub-issues are live and approved, launch the relevant engineer subagents (`backend-engineer`, `kmp-engineer`, `apple-developer`, `desktop-engineer`) in parallel.

### 2. Living Ticket Memory & Step Resumption: Never Lose Context Across Sessions
- Every ticket maintains a living state machine in `.agents/tickets/<issue-number>-<slug>/STATE.json` and `NOTES.md`.
- **Fast-Forward on Resume**: When starting or resuming a session, check `STATE.json`. Skip already completed and approved steps; resume precisely from the first pending or interrupted step.
- **Continuous Step Injection**: If the user introduces an adjustment, rework, or adds an extra step mid-stream, trace back the memory log, perform delta analysis, inject the new step into `STATE.json` without discarding completed work, and proceed smoothly.

---

## The 7 Specialized Agent Roles

| Role | Agent Type / Name | Scope & Responsibility | Reference Prompt & Upstream Skills |
|---|---|---|---|
| **1. PM & Solutions Architect** | Main Agent + `research` subagents | Scans code in parallel; defines cross-repo REST/DTO contract; specifies **4-quadrant test scenarios** (Happy, Failed, Error, Related Changes), structured **Use Cases**, and **GUI framework test specification** (Maestro / Compose UI Test / Swift Testing); creates GitHub parent/sub-issues, stamps active sprint & initializes `NOTES.md`. | Built into this `SKILL.md` |
| **2. Backend Engineer** | `backend-engineer` | Implements Spring Boot / Kotlin / Flyway / JPA / JobRunr changes & unit/integration tests (covering happy, failed, and error paths) in `course_deals_backend`, exports `openapi/openapi.json`, runs `./gradlew ktlintCheck detekt test`, commits locally. | [references/backend-engineer.md](references/backend-engineer.md) |
| **3. KMP Engineer** *(migrated from `android-engineer`)* | `kmp-engineer` | Implements Clean Architecture in `course_deals_ComposeMultiplatform` across shared KMP/CMP modules (`commonMain`, `androidMain`, `iosMain`, `jvmMain`), `:app` (Android entry point), `:shared`, `:core:*`, `:domain:*`, `:data:*`, `:feature:*`; runs OpenAPI codegen (`generateCourseDealsApi`), exposes Maestro semantic test tags, runs `:app:detekt`, `:app:lintLocalDebug`, `testLocalDebugUnitTest`, and `:shared:linkDebugFrameworkIosSimulatorArm64`, commits locally. | [references/kmp-engineer.md](references/kmp-engineer.md) |
| **4. Apple Developer (iOS & watchOS)** *(New)* | `apple-developer` | Implements native Apple platform targets in `course_deals_ComposeMultiplatform`: **iOS (`iosApp/`)** (Xcode project/schemes, SwiftUI, `ComposeApp.framework` / SKIE interop, Keychain, Sign in with Apple / Passkeys, APNs, Universal Links, WidgetKit, Live Activities / `ActivityKit`, App Intents, `PrivacyInfo.xcprivacy`) and **watchOS (`watchApp/`)** (native Apple Watch SwiftUI, `WatchConnectivity` `WCSession` sync, Watch Complications via WidgetKit, Digital Crown & Always-On Display); verifies via `xcodebuild` & Swift Testing, commits locally. | [references/apple-developer.md](references/apple-developer.md) |
| **5. Desktop CMP Engineer** | `desktop-engineer` | Implements Compose Multiplatform Desktop (`:desktopApp` & `jvmMain`) features: Window State & native menus/shortcuts, Swing/JavaFX interop (`SwingPanel`/`JFXPanel`), Skia `GraphicsLayer` screenshot sync, and **Compose Desktop UI Framework Tests (`runComposeUiTest` / `createComposeRule`)**; runs `:desktopApp:detekt` & `:desktopApp:test`, commits locally. | [references/desktop-engineer.md](references/desktop-engineer.md) |
| **6. Security & Arch Reviewer** | `security-arch-reviewer` | Read-only auditor (runs in parallel with QA): verifies Backend↔Client DTO JSON contract parity, Android Intent security (`intent-security-review`), Apple iOS/watchOS Keychain & bridge safety (`swift-security`), KMP readiness & coroutine/Ktor hygiene (`kmp-readiness`), JPA entity & Flyway rules, ADR criteria (`adr`), and 4-quadrant test coverage. | [references/security-arch-reviewer.md](references/security-arch-reviewer.md) |
| **7. QA & Release Engineer** | `qa-release-engineer` | Runs live E2E verification: Backend `curl -i` / test script locally ➔ **Cross-platform KMP (Android `emulator-5554` + iOS `iPhone Simulator` + watchOS `Apple Watch Simulator` [when applicable] + Desktop `:desktopApp`)** ➔ visual pixel inspection (`read_file`) ➔ Cloudflare R2 uploads ➔ **Explicit Side-by-Side Comparison Matrix** ➔ opens labeled PRs (`ship-pr`) ➔ reclaims host RAM/CPU (`release-resource`). | [references/qa-release-engineer.md](references/qa-release-engineer.md) |

---

## 🌐 Curated Upstream Skill Catalog (Aligned to Course Deals)

Each role prompt in `references/*.md` bakes in high-signal production rules and links to **10 curated, Course Deals-aligned skill files** under `references/upstream/{kmp,apple,backend}/` (pruned of bloated migration guides, broken external links, and non-project stacks):

- **KMP & Compose Multiplatform (`kmp-engineer` & `desktop-engineer` — 4 files)**:
  - [`references/upstream/kmp/kmp-app-architecture.md`](references/upstream/kmp/kmp-app-architecture.md) — Koin object graph outside composition lambdas, `commonMain` interfaces vs. `expect`/`actual` vs. Swift bridges, `_uiState.update { }` snapshot merging, coroutine ownership checks.
  - [`references/upstream/kmp/kmp-ktor-networking.md`](references/upstream/kmp/kmp-ktor-networking.md) — `:core:network` Ktor 3 (`OkHttp` + `Darwin`), `./gradlew generateCourseDealsApi` OpenAPI codegen, `requestTimeoutMillis` + `socketTimeoutMillis`, `runSuspendCatching`, host-scoped Bearer Auth.
  - [`references/upstream/kmp/kmp-testing-strategy.md`](references/upstream/kmp/kmp-testing-strategy.md) — `testLocalDebugUnitTest`, `jvmTest`, `:desktopApp:test`, `:shared:linkDebugFrameworkIosSimulatorArm64`, fakes in `:domain:*`, Ktor `MockEngine`, no commas in backticked test names.
  - [`references/upstream/kmp/kmp-sheets-keyboard.md`](references/upstream/kmp/kmp-sheets-keyboard.md) — `:core:ui` Material 3 `ModalBottomSheet` 85% height cap, `FocusRequester` safety inside `LaunchedEffect`, safe-area insets, `Modifier.testTag`.
- **Apple Ecosystem — iOS & watchOS (`apple-developer` — 4 files)**:
  - [`references/upstream/apple/swiftui-pro.md`](references/upstream/apple/swiftui-pro.md) — Modern SwiftUI (`@Observable`, `@Bindable`, view invalidation boundaries), Swift 6+ `@MainActor` concurrency, VoiceOver & `.accessibilityIdentifier`, **watchOS (`watchApp` SwiftUI, `WatchConnectivity` `WCSession`, Digital Crown, Always-On Display)**, Swift Testing (`@Test`, `#expect`).
  - [`references/upstream/apple/kmp-ios-build.md`](references/upstream/apple/kmp-ios-build.md) — `iosApp.xcodeproj`, `MainViewControllerKt.MainViewController()`, `:shared:linkDebugFrameworkIosSimulatorArm64`, Swift `completionHandler` single-invocation rule, JSON string bridging, Keychain reinstall persistence (`xcrun simctl keychain booted reset`).
  - [`references/upstream/apple/widgetkit.md`](references/upstream/apple/widgetkit.md) — iOS Home/Lock Screen deal widgets + **watchOS Complications (`.accessoryCircular`, `.accessoryRectangular`, `.accessoryInline`, `.accessoryCorner`)** + App Group `group.com.thanh0x.coursedeals`.
  - [`references/upstream/apple/swift-security.md`](references/upstream/apple/swift-security.md) — Keychain Services (`SecItemAdd`/`SecItemUpdate`, `OSStatus` checks), Sign in with Apple / Passkeys (`ASAuthorizationController`), `PrivacyInfo.xcprivacy`.
- **Spring Boot + Kotlin Backend & Reviewer (`backend-engineer` & `security-arch-reviewer` — 2 files)**:
  - [`references/upstream/backend/kotlin-backend-jpa-entity-mapping.md`](references/upstream/backend/kotlin-backend-jpa-entity-mapping.md) — `modules:coupon-domain` regular `class` `@Entity` (never `data class`), ID-based `equals`, mandatory Flyway `V<timestamp>__<desc>.sql` parity, DB `UNIQUE` + service guard, `@Transactional` vs. JobRunr/Redis boundaries.
  - [`references/upstream/backend/spring-kotlin-code-review.md`](references/upstream/backend/spring-kotlin-code-review.md) — `@field:` Jakarta validation on Kotlin DTOs, `openapi/openapi.json` export, 4-quadrant `@RestControllerAdvice` error envelopes, `./gradlew ktlintCheck detekt test`.

### Checking Upstream GitHub Updates (`sync-upstream-skills.sh`)
To stage and compare the latest upstream `SKILL.md` files from GitHub without overwriting Course Deals customizations:
```bash
bash .agents/skills/team-flow/scripts/sync-upstream-skills.sh
```

---

## Execution Workflow Overview

```
Phase 1: PM & Architect Discovery (Main Agent + parallel research subagents)
   • 4-Quadrant Test Scenarios (Happy, Failed, Error, Related Changes)
   • Structured Use Cases (UC-01, UC-02...)
   • GUI Framework Test Specification (Maestro flows for Android, iOS & Desktop + watchOS checks)
   • Cross-Repo API Contract & Cross-Platform DoD Matrix
                    │
         [Checkpoint 1: Spec Approval]
                    │
Phase 2: Ticket & Workspace Setup (Branches, Project Sprint Stamping & NOTES.md)
                    │
Phase 3: Parallel Implementation (backend-engineer, kmp-engineer, apple-developer, desktop-engineer)
                    ├───┐
                    │   │
Phase 4: Parallel Review & Backend QA
  • security-arch-reviewer: Audits DTO contract, Android intent security, Apple Keychain/bridge safety, KMP readiness, 4-quadrant coverage
  • qa-release-engineer (Backend): docker compose up -> bootRun -> curl -i / test script -> ship backend PR
                    │   │
                    └───┼───> Phase 5: Gated Cross-Platform Live E2E, Release & Resource Cleanup
                              (Triggered once BE is verified locally & Client commits are ready)
                              • Run GUI framework tests (Maestro) across Android, iOS & Desktop (+ watchOS Simulator when applicable)
                              • Visual pixel inspection (read_file) & R2 upload
                              • Side-by-side comparison matrix (Android vs. iOS vs. Desktop [& watchOS])
                              • Ship KMP/Multiplatform PR with comparative evidence
                              • Execute release-resource (stop Gradle/Kotlin daemons & test JVMs, log RAM/CPU stats)
                                        │
                              [Checkpoint 2: Merge Gate]
```

---

## Phase 1: PM & Architect Discovery (Main Agent)

1. **Determine Repository Scope & Task Type**:
   - Scope: Backend-only, KMP/Multiplatform (`area: android` / `area: ios` / `area: watchos` / `area: desktop`), or Full-Stack (Cross-Repo).
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
     - Impact on surrounding screens/components, shared DTOs/models, navigation routes, bottom bar tabs, cache invalidation (Room/Redis), session state, dark mode, system insets, WatchConnectivity sync, and backward compatibility.

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
   - Accessibility & semantic selectors required on UI components (`Modifier.testTag`, `.accessibilityIdentifier`, content descriptions, text labels).
   - Assertion checkpoints: `assertVisible`, `assertNotVisible`, `extendedWaitUntil`.
   - Visual capture checkpoints: `takeScreenshot` for each critical state.

6. **Draft Cross-Repo API Contract & Cross-Platform DoD Matrix**:
   - **Cross-Repo API Contract** (REST path, HTTP verbs, request/response JSON schema, nullability, HTTP status codes).
   - **Cross-Platform DoD Matrix**: Explicit verifiable criteria for happy path, failed path, error path, migrations, detekt/lint/xcodebuild checks, and **product documentation updates** (updating affected `docs/product/` user flows or route specs).
   - **Multi-Platform Issue Plan**: Propose top-level parent issue title and the platform sub-issue matrix:
     - `+ Android` (Compose Multiplatform / Android target — `kmp-engineer`)
     - `+ iOS` (Compose Multiplatform / iOS SwiftUI target — `apple-developer` & `kmp-engineer`)
     - `+ WatchOS` (if needed — `apple-developer`)
     - `+ BE` (if needed — `backend-engineer`)
     - `+ Desktop` (if needed — `desktop-engineer`)

---

## Checkpoint 1: Spec & Plan Gate (Stop #1)

> [!CRITICAL]
> **MANDATORY PROTOCOL AT STOP #1: UPDATE GITHUB ISSUE & DELIVER PROPOSAL PLAN**:
> 1. **Update or Create GitHub Issue Description BEFORE Calling `ask_question`**:
>    - **For existing issues** (e.g. `#158`): Agent MUST immediately update the GitHub issue description to reflect the full, elaborated proposal plan:
>      ```bash
>      gh issue edit <issue-number> -R Huythanh0x/<repo> --body "<comprehensive solution architecture plan>"
>      ```
>    - **For raw requirements without an existing issue**: Agent MUST create the top-level issue on GitHub containing the full proposal plan and add it to the Project board (`Course Deals`):
>      ```bash
>      gh issue create -R Huythanh0x/<repo> --title "..." --label "type: ..." --label "priority: ..." --label "area: ..." --label "size: ..." --project "Course Deals" --body "<comprehensive solution architecture plan>"
>      ```
> 2. **Explicit Chat Response & Artifact Generation (No Blank Calls)**:
>    - **NEVER** call `ask_question` with empty message content or `content: None`.
>    - Agent MUST generate a user-facing artifact `<appDataDir>/brain/<conversation-id>/TEAM_EXECUTION_BRIEF.md` containing the complete proposal plan.
>    - In the **same turn** as calling `ask_question`, agent MUST output the full markdown **Team Execution Brief** directly in the visible chat response so the user can review all details and click the updated GitHub issue link right alongside the confirmation buttons.

### Team Execution Brief Structure:
- **GitHub Issue Link**: Clickable link to updated/created issue (e.g. `[#158](https://github.com/Huythanh0x/course_deals_ComposeMultiplatform/issues/158)`)
- **Scope**: Backend / KMP / Apple (iOS & watchOS) / Desktop / Full-Stack
- **Assigned Engineers**: Which of `backend-engineer`, `kmp-engineer`, `apple-developer`, `desktop-engineer` will execute
- **Multi-Platform Sub-Issue Plan**: `+ Android`, `+ iOS`, `+ WatchOS (if needed)`, `+ BE (if needed)`, `+ Desktop (if needed)`
- **Use Cases & 4-Quadrant Test Scenarios**: Summary of Happy, Failed, Error, and Related scenarios
- **GUI Framework Test Plan**: Maestro E2E flow outline for Android, iOS, Desktop (& watchOS if applicable)
- **API Contract** (if applicable): Endpoint(s) and JSON DTO schema
- **Target Files**: Bullet list per repo from the research scan
- **Edge-Case Decisions**: Defaults chosen + any open questions

Then call `ask_question` with:
- **(Recommended) Approve plan & Merge on complete (launch team, verify KMP/Apple/Desktop evidence, wait for green CI, and squash-merge)**
- **Approve plan & Stop for review at PR (gated merge)**
- **Adjust requirements or API contract** (user provides feedback via write-in)

Do **not** proceed to Phase 2 until the user approves.

---

## Phase 2: Ticket & Workspace Setup (Main Agent)

> [!CRITICAL]
> **HARD PREREQUISITE GATE FOR PHASE 3**:
> Under NO circumstances should `backend-engineer`, `kmp-engineer`, `apple-developer`, or `desktop-engineer` subagents be defined or invoked before this phase is 100% complete!
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
     - **Multi-Platform Sub-Issues Generation**: Create parent (`--project "Course Deals"`) and generate dedicated platform sub-issues via `create-sub-issues`:
       - `+ Android` (`area: android` — `kmp-engineer`): CMP, `:app`, Glance AppWidgets, Room, Android Maestro test flow.
       - `+ iOS` (`area: android` or `area: design` — `apple-developer` & `kmp-engineer`): CMP `iosApp`, SwiftUI WidgetKit, App Intents, iOS Maestro flow.
       - `+ WatchOS (if needed)` (`area: android` — `apple-developer`): SwiftUI WatchApp, Smart Stack complications, `WCSession`.
       - `+ BE (if needed)` (`area: backend` — `backend-engineer`): Spring Boot, Flyway, JobRunr, REST API.
       - `+ Desktop (if needed)` (`area: desktop` — `desktop-engineer`): CMP `:desktopApp`, JavaFX WebView, Desktop test runner.
       Each sub-issue body **MUST** specify its own concrete, domain-specific architecture and tech stack details per `create-sub-issues`. Link via the Sub-issues REST API, and run the GraphQL board auto-cascade cleanup so sub-issues don't clutter the `"Course Deals"` board.
   - In each target repo, checkout latest `main`, pull, and create the branch `<type>/<issue-number>-<slug>`:
     ```bash
     git -C <repo-path> checkout main && git -C <repo-path> pull origin main
     git -C <repo-path> checkout -b <type>/<issue-number>-<slug>
     ```
2. **Initialize Ticket Workspace Memory (`STATE.json` & `NOTES.md`)**:
   - In each target repo, create `.agents/tickets/<issue-number>-<slug>/`:
     - **`STATE.json`**: Machine-readable state machine storing current phase, step-by-step progress, approval flags, and memory trace.
     - **`NOTES.md`**: Human-readable living documentation containing `## Requirements`, `## Use Cases & Test Scenarios`, `## GUI Framework Test Specification`, `## Step Progress & Approvals Tracker`, `## Memory Trace & Injected Changes Log`, `## Code scan`, `## Clarifications & Decisions`, and `## Progress log`.

---

## 🧠 Persistent Ticket Memory, Resumption & Continuous Step Evolution

### 1. Fast-Forward Resumption (Checking Memory First)
Whenever an agent starts or resumes `/team-flow` for an ongoing ticket:
1. **Always inspect `.agents/tickets/<issue-number>-<slug>/STATE.json` first.**
2. Check `phases` and `steps_log`:
   - If a phase or step is marked `"COMPLETED"` or `"DONE"` with `"approved": true`, **DO NOT re-execute it**.
   - Announce the resumed state and jump directly to the current `active_step` or first `"PENDING"` step.

### 2. Continuous Step Injection (Mid-Flight Changes & Tracing Back Memory)
When the user adds new requirements, adjusts an edge case, or introduces a new step:
1. **Trace Back the Memory**: Read `STATE.json` and `NOTES.md`.
2. **Delta Analysis**: Keep unaffected steps `"DONE"`, mark impacted steps `"NEEDS_REWORK"`, and insert new step(s) into `steps_log`.
3. **Log the Event in `memory_trace`**: Append an entry to `memory_trace` in `STATE.json` and `NOTES.md`.
4. **Continuously Apply and Execute**: Seamlessly launch or message the relevant subagent (`backend-engineer`, `kmp-engineer`, `apple-developer`, `desktop-engineer`, or `qa-release-engineer`) to implement and verify ONLY the newly injected or reworked steps.

---

## Phase 3: Parallel Engineering Execution (`backend-engineer` + `kmp-engineer` + `apple-developer` + `desktop-engineer`)

Read the relevant reference prompt(s):
- Backend: [references/backend-engineer.md](references/backend-engineer.md)
- KMP (Shared CMP + Android): [references/kmp-engineer.md](references/kmp-engineer.md)
- Apple (iOS `iosApp` & watchOS `watchApp`): [references/apple-developer.md](references/apple-developer.md)
- Desktop (`:desktopApp` & `jvmMain`): [references/desktop-engineer.md](references/desktop-engineer.md)

1. **Define Subagents** using `define_subagent` (`Name: "backend-engineer"`, `Name: "kmp-engineer"`, `Name: "apple-developer"`, and/or `Name: "desktop-engineer"`, with `EnableWriteTools: true`).
2. **Invoke Simultaneously** (when touching orthogonal repos or directories; if `apple-developer` or `desktop-engineer` depends on a new shared `:shared` interface from `kmp-engineer`, either lock the interface signature upfront in the prompt or run `kmp-engineer` first followed by `apple-developer`/`desktop-engineer`):
   - **Backend Engineer (`backend-engineer`)**: Implements endpoints, services, repositories, Flyway migrations, OpenAPI export, and unit/integration tests covering happy, failed, and error scenarios.
   - **KMP Engineer (`kmp-engineer`)**: Implements Clean Architecture in Compose Multiplatform across shared KMP modules (`:shared`, `:core:*`, `:domain:*`, `:data:*`, `:feature:*`) and `:app` (Android), adds semantic test tags / labels conforming to the GUI framework test specification, handles empty/success/error UI states, and verifies `:app:detekt`, `:app:lintLocalDebug`, `testLocalDebugUnitTest`, and `:shared:linkDebugFrameworkIosSimulatorArm64`.
   - **Apple Developer (`apple-developer`)**: Implements native iOS (`iosApp/`) and watchOS (`watchApp/`) SwiftUI views, KMP `ComposeApp.framework` / SKIE bridges, `WatchConnectivity` (`WCSession`) sync, WidgetKit & Watch Complications, Live Activities (`ActivityKit`), Keychain storage, and verifies `xcodebuild` & Swift Testing.
   - **Desktop CMP Engineer (`desktop-engineer`)**: Implements `:desktopApp` and `jvmMain` features, Swing/JavaFX interop, Skia screenshot sync, and Compose Desktop UI tests (`:desktopApp:test` & `:desktopApp:detekt`).
   - Subagents commit their changes locally to their respective branches and report commit hashes and test results back.

---

## Phase 4: Parallel Review & Backend Local QA

As soon as commits are ready:
1. **Parallel Audit (`security-arch-reviewer`)**:
   - Defined with `EnableWriteTools: false`. Runs in parallel with QA.
   - Inspects cross-repo DTO contract parity, Android Intent security (`intent-security-review`), Apple iOS/watchOS Keychain & bridge safety (`swift-security`), KMP readiness & coroutine/Ktor safety (`kmp-readiness`), JPA entity & Flyway hygiene, AND verifies that all 4 scenario quadrants (happy, failed, error, regression) are covered.
   - If verdict is `NEEDS_FIX`: fixes are routed back to the appropriate engineer (`backend-engineer`, `kmp-engineer`, `apple-developer`, or `desktop-engineer`) immediately.
2. **Backend Local E2E & Committed Test Harness (`qa-release-engineer`)**:
   - If Backend has changes: starts local dependencies (`docker compose up -d`), runs Spring Boot application with `--spring.profiles.active=local`.
   - **Generates & commits executable test script** at `scripts/test-api/<issue>-<slug>.sh` asserting HTTP status codes (200, 400, 404, etc.) and `jq` body schemas against the ticket's DoD matrix.
   - Executes the script, capturing output directly into the PR body.
   - Pushes backend branch and creates Backend PR with mirrored labels.
   - **Signals that Backend is ready for local API integration**.

---

## Phase 5: Gated Cross-Platform Live E2E & Release (`qa-release-engineer`)

If `course_deals_ComposeMultiplatform` was modified:
1. **Gate Check**: Triggers only when:
   - KMP / Apple / Desktop engineering implementation is complete (`kmp-engineer`, `apple-developer`, `desktop-engineer` committed).
   - Backend local server is running and verified locally via committed test script (ready for live API integration on port 8081).
2. **Execute Automated GUI Framework Tests with Embedded Screenshot Capture (Maestro / Simulator)**:
   - The declarative Maestro test flow (`.maestro/flows/<flow>.yaml`) **MUST declare step-by-step `- takeScreenshot: <step_name>` checkpoints** for every significant interaction and screen state transition along the user journey.
   - Run the automated suite across **all affected platforms** (Android + iOS + Desktop, plus watchOS Simulator when `watchApp/` is modified):
     ```bash
     # Android Emulator (emulator-5554)
     ~/.maestro/bin/maestro --device emulator-5554 test -e APP_ID=com.thanh0x.coursedeals .maestro/flows/<flow>.yaml

     # iOS Simulator (booted iPhone Simulator)
     ~/.maestro/bin/maestro --device booted test -e APP_ID=com.thanh0x.coursedeals.iosApp .maestro/flows/<flow>.yaml

     # Compose Desktop (:desktopApp Maestro Flow Runner)
     ./gradlew :desktopApp:runMaestroFlow -Pflow=.maestro/flows/<flow>.yaml -PscreenshotDir=/tmp/desktop-e2e

     # watchOS Simulator (when watchApp / Watch Complications are modified)
     xcrun simctl io "<watch-simulator-udid>" screenshot /tmp/watchos_step.png
     ```
3. **Mandatory Visual Pixel Inspection (`read_file`)**:
   - Agent **MUST invoke `read_file`** on every captured `.png` image across Android, iOS, Desktop (and watchOS) before uploading to verify crisp text rendering, proper system bar/watch bezel insets, and expected UI states.
4. **Cloudflare R2 Upload**:
   - Upload each step screenshot to Cloudflare R2 worker (`https://r2-image-worker.huythanh0x.workers.dev/upload`), verify HTTP 200, and construct public URLs.
5. **Feature-Rich Step-by-Step Flow Evidence in GitHub PR**:
   - Present a **feature-rich, step-by-step user journey walkthrough** comparing Android, iOS, and Desktop (plus watchOS when applicable) side-by-side using `<img src="..." width="250" />`.
   - Actively audits for discrepancies. If any defect is found, logs it and routes back to `kmp-engineer`, `apple-developer`, or `desktop-engineer` for resolution before shipping.
6. **Ship KMP/Multiplatform PR**:
   - Pushes branch and opens PR from `.github/PULL_REQUEST_TEMPLATE.md` with the side-by-side walkthrough table, Maestro E2E test execution results, mirrored issue labels, and `Closes #<N>` reference.
7. **Post-Ticket Resource Reclamation (`release-resource`) — `[BLOCKING RULE]`**:
   - Immediately after PR(s) are opened and E2E verification is complete, execute `.agents/skills/release-resource/SKILL.md`:
     1. Capture **Before** system stats (`top -l 1 -n 0 | grep -E "^CPU|^Load|^PhysMem"`, `sysctl vm.swapusage`, `memory_pressure`, `jps -l`).
     2. Stop all Gradle & Kotlin compile daemons across `./course_deals_backend` and `./course_deals_ComposeMultiplatform` (`./gradlew --stop`).
     3. Terminate orphaned `gradle-wrapper.jar`, `KotlinCompileDaemon`, `:desktopApp` (`MainKt`), and ephemeral Spring Boot instances on ports `8080`/`8081`/`8082`.
     4. Capture **After** system stats and include the **Before vs. After Resource Reclamation Summary** in the delivery report.

---

## Checkpoint 2: Ready to Merge Gate (Stop #2 / Auto-Merge on Complete)

Once `qa-release-engineer` reports back with the opened PR URL(s), `release-resource` reclamation stats, and `security-arch-reviewer` passes:

### A. Auto-Merge on Complete Path (When instructed or selected at Checkpoint 1)
If the user instructed "on complete, merge for me" or `"auto_merge_on_complete": true` in `STATE.json`:
1. **Do not stop or prompt with an interactive question**.
2. Monitor CI checks (`gh pr checks <PR-number>`) until all checks pass green.
3. Verify `mergeStateStatus == "CLEAN"` (if `BEHIND`, merge `origin/main` into the branch).
4. Execute `merge-pr` immediately (`gh pr merge <PR-number> --squash ...`).
5. Confirm branch auto-deleted and issue auto-closed.
6. Update `STATE.json` (`phases.CHECKPOINT_2_MERGE_APPROVAL.status = "MERGED"`).
7. Clean up local branch (`git checkout main && git pull origin main && git branch -d <branch>`) and ensure `release-resource` has been executed.
8. Present the final **Team Delivery & Merge Confirmation Report** (including the `release-resource` Before/After RAM & CPU table) to the user.

### B. Interactive Review Gate (If user chose manual gate at Checkpoint 1)
1. Present the **Team Delivery Report**:
   - **PR URL(s)** & Branch(es)
   - **Cross-Platform Parity Summary**: Android vs. iOS vs. Desktop (& watchOS) comparison verdict
   - **Agent Team Summary**:
     - `backend-engineer`: commit + ktlint/detekt/JUnit summary
     - `kmp-engineer`: commit + detekt/lint/JUnit/iOS-framework-link summary
     - `apple-developer`: commit + xcodebuild/Swift-Testing summary (if invoked)
     - `desktop-engineer`: commit + desktop detekt/Compose-UI-test summary (if invoked)
     - `security-arch-reviewer`: contract, security & 4-quadrant audit verdict
     - `qa-release-engineer`: Maestro E2E runs, Backend test script output, embedded side-by-side R2 screenshots & **`release-resource` Before/After RAM & CPU Reclamation Summary**
2. Call `ask_question` with:
   - **Merge PR(s) now (run `merge-pr` once CI is green)**
   - **Leave PR(s) open — I will review/merge manually**
   - **Rework something** (user specifies via write-in; route directly to the responsible subagent via `send_message`)
3. If the user selects **Merge PR(s) now**, follow steps 2-8 of the Auto-Merge path above.
