---
name: new-ticket
description: Create a GitHub issue with this repo's label taxonomy and the matching branch, following docs/project-management.md and docs/branching.md. Use when the user wants to file a new issue/ticket/bug/chore for this repo, or asks to start work on something that doesn't have an issue yet.
---

# New ticket

Creates a GitHub issue in the right shape, then the branch to work on it — mirrors the
flow this repo's own docs prescribe.

## Multi-Platform Issue Architecture: Parent & Sub-Issues

Course Deals is an interconnected multiplatform ecosystem spanning **Android, iOS, WatchOS, Backend, and Desktop**. To maintain clear ownership, verification, and board hygiene, all feature tickets **MUST** be structured as a **Top-Level Parent Issue** on the ["Course Deals" Project Board](https://github.com/users/Huythanh0x/projects/4) with explicit platform sub-issues created under it:

```
[ Parent Issue (Epic on Board #4) ]
   ├── + Android (Sub-issue: Compose Multiplatform, :app, Glance, Maestro)
   ├── + iOS (Sub-issue: SwiftUI, :iosApp, WidgetKit, App Intents, Maestro)
   ├── + WatchOS (if needed) (Sub-issue: SwiftUI WatchApp, Smart Stack, WCSession)
   ├── + BE (if needed) (Sub-issue: Spring Boot, Flyway, JobRunr, REST API)
   └── + Desktop (if needed) (Sub-issue: Compose Desktop :desktopApp, JavaFX WebView)
```

---

## Steps

1. **Pick the type** from the user's request: `feature`, `bug`, `chore`, `docs`,
   `refactor`, `test`, or `design`. If unclear, ask.
2. **Write the issue body**:
   - For **`feature` issues**:
     - **Mandatory Project**: Must be tracked under GitHub Project [`Course Deals`](https://github.com/users/Huythanh0x/projects/4) (`--project "Course Deals"`).
     - **Parent Solution Architecture Plan**: Top-level feature issues must include an explicit architecture plan covering:
       - Problem statement & core motivation.
       - System architecture diagram / component flow across repos (Backend & Android/KMP).
       - API / DTO contract specification (endpoints, request/response models, error handling).
       - **Multi-Platform Scope Breakdown**:
         - `+ Android`: Shared KMP + Android platform targets (`:app`, Glance AppWidgets, push receiver, semantic test tags).
         - `+ iOS`: Shared KMP + iOS platform targets (`:iosApp`, SwiftUI WidgetKit, ActivityKit/Dynamic Island, App Intents).
         - `+ WatchOS (if needed)`: SwiftUI WatchApp target, WatchConnectivity (`WCSession`), complications, Smart Stack.
         - `+ BE (if needed)`: Spring Boot services, Flyway schema migrations, JobRunr async queues, REST endpoints.
         - `+ Desktop (if needed)`: Compose Multiplatform `:desktopApp` windowing, JavaFX WebKit platform browser, desktop keyboard shortcuts.
       - Definition of Done (DoD) verification matrix.
     - **Sub-Issues Generation**: Immediately generate and attach the corresponding platform sub-issues (Android, iOS, and when applicable WatchOS, BE, Desktop) using `create-sub-issues`. Sub-issues must NOT be added to the project board.
   - For **`bug` issues**:
     - Focus on observed vs expected behavior, reproduction steps, affected platform(s), environment/logs.
   - Use `.github/ISSUE_TEMPLATE/bug_report.md` or `.github/ISSUE_TEMPLATE/feature_request.md` structure.
3. **Pick labels**, exactly one from each category (see `docs/project-management.md`
   for the full taxonomy):
   - `type: <feature|bug|chore|docs|refactor|test|design>`
   - `priority: <P0|P1|P2|P3>` — ask if not obvious from context
   - `area: <android|backend|web|design|testing|ci|infra|desktop>` — usually `area: android` for core client features or `area: desktop` for desktop-focused work
   - `size: <1|2|3|5|8|13>` — if it feels like `13`, say so and suggest splitting
     before creating the issue (per `docs/project-management.md`'s sizing guidance)
4. **Decide top-level issue vs. sub-issue**, per `docs/project-board.md`: only
   top-level work items belong on the "Course Deals" board — if the user's request is a
   child task of an already-tracked parent issue (they reference a parent, or you're
   splitting an oversized issue per the `size: 13` guidance), this is a sub-issue. Ask if
   it's ambiguous rather than guessing.
5. **Create the issue**:
   - **Top-level Parent Issue** — add it to the project board at creation (mandatory for features):
     ```bash
     gh issue create -R Huythanh0x/course_deals_ComposeMultiplatform \
       --title "<title>" \
       --label "type: <x>" --label "priority: <x>" --label "area: <x>" --label "size: <x>" \
       --project "Course Deals" \
       --body "<body>"
     ```
   - **Sub-issues** — hand off to the `create-sub-issues` skill to generate:
     - `+ Android` sub-issue
     - `+ iOS` sub-issue
     - `+ WatchOS` sub-issue (if needed)
     - `+ BE` sub-issue (if needed)
     - `+ Desktop` sub-issue (if needed)
     and perform the board auto-cascade cleanup.
6. **Create the branch** off the latest `main`, per `docs/branching.md`'s naming
   convention `<type>/<issue-number>-<short-slug>`:
   ```bash
   git checkout main && git pull origin main
   git checkout -b <type>/<issue-number>-<short-slug>
   ```
7. Report the issue URL, branch name, and platform sub-issues list back to the user before starting any code changes.

## Notes

- Never branch off another feature branch — always from latest `main` (`docs/branching.md`).
- Confirm with the user before creating the issue if the title/labels involved any real judgment call (priority, size) rather than assuming.
- Sub-issues are intentionally excluded from the board — only their parent represents the work there. See `create-sub-issues` for the full mechanics.
- If 2+ of the issues/sub-issues being filed here are independent enough to implement in parallel, consider `dispatch-ticket-worktree` or `team-flow`.
