---
name: create-sub-issues
description: Create one or more GitHub sub-issues under an existing parent issue (new or already-existing issues), link them via the Sub-issues API, and clean up GitHub's board auto-cascade. Use when the user wants to break a tracked issue into child tasks, or attach existing standalone issues as children of a parent.
---

# Create sub-issues

Pulled out of `new-ticket` into its own skill since it's invoked on its own at least as
often as from a fresh `new-ticket` flow — e.g. splitting an existing umbrella issue
after the fact, or attaching issues that already existed before the parent did.

Sub-issues are **intentionally excluded from the "Course Deals" board** — only a
parent issue represents the work there (see `docs/project-board.md`). Every path below
ends with the same board-cascade check for that reason.

## Multi-Platform Sub-Issue Architecture Requirements

Every top-level feature epic in Course Deals is broken down into domain-specific sub-issues across the 5 platforms:
- **`+ Android`** (Required for client features)
- **`+ iOS`** (Required for client features)
- **`+ WatchOS`** (If feature has watch complications, glance cards, or deal notifications)
- **`+ BE`** (If feature requires backend endpoints, database migrations, or cron/worker jobs)
- **`+ Desktop`** (If feature involves desktop windowing, JavaFX WebView, or desktop hotkeys)

Each sub-issue body **MUST** specify its own concrete, domain-specific architecture and tech stack details:

### 1. `+ Android Sub-Issue` Architecture Spec
- **Tech Stack**: Jetpack Compose / Compose Multiplatform (`commonMain` + `androidMain`), Clean Architecture (`presentation`, `domain`, `data`), Koin DI, Ktor Client, Room offline cache, Jetpack Glance (AppWidgets).
- **Components & Files**:
  - Presentation: ViewModel (`StateFlow<UiState>`), Composable screens/components, semantic tags/labels for testing (`Modifier.semantics`, `testTag`).
  - Domain & Data: UseCases, Repository implementations, Room Entities/DAOs, network API services.
  - Android Platform: `:app` target, `AndroidManifest.xml` intent filters / permissions, Jetpack Glance AppWidgets, push receiver.
- **Verification Plan**:
  - Unit tests (`./gradlew testLocalDebugUnitTest`).
  - Static analysis (`./gradlew :app:detekt lintLocalDebug`).
  - Android Maestro GUI test flow (`.maestro/flows/XX-android.yaml` on `emulator-5554`).

### 2. `+ iOS Sub-Issue` Architecture Spec
- **Tech Stack**: Compose Multiplatform (`commonMain` + `iosMain`), Swift & SwiftUI in `iosApp`, Xcode project configuration, SwiftUI WidgetKit, ActivityKit (Dynamic Island / Live Activities), App Intents.
- **Components & Files**:
  - Multiplatform Bridge: `shared/src/iosMain/kotlin/`, `MainViewController.kt`, `PlatformActions.ios.kt`.
  - Native iOS App: `iosApp/iosApp/`, `ContentView.swift`, `Info.plist` (custom URL schemes, APNs).
  - iOS Widgets & Live Activities: SwiftUI `WidgetBundle`, `ActivityConfiguration`, `AppIntent` handlers.
- **Verification Plan**:
  - iOS Framework compilation (`./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`).
  - iOS Maestro GUI test flow (`.maestro/flows/XX-ios.yaml` on iOS Simulator).

### 3. `+ WatchOS Sub-Issue` (If Needed) Architecture Spec
- **Tech Stack**: SwiftUI WatchApp target (`iosApp/WatchApp`), WatchConnectivity framework (`WCSession`), WidgetKit Complications, watchOS Smart Stack.
- **Components & Files**:
  - Watch App: `iosApp/WatchApp/` (`ContentView.swift`, `WatchConnectivityClient.swift`).
  - Phone Bridge: `iosApp/iosApp/PhoneWatchConnectivityManager.swift` bridging shared KMP domain.
  - Watch Complications: SwiftUI `WidgetConfiguration` for `.accessoryCircular`, `.accessoryInline`, `.accessoryCorner`.
- **Verification Plan**:
  - Xcode watchOS build (`xcodebuild -scheme WatchApp ...`).
  - Local API fallback smoke test.

### 4. `+ Backend Sub-Issue` (If Needed) Architecture Spec
- **Tech Stack**: Spring Boot (Kotlin), Gradle multi-module (`coupon-domain`, `coupon-api-service`, `coupon-crawler-service`, `notification-service`), Spring Data JPA, Flyway, JobRunr, Redis, MySQL.
- **Components & Files**:
  - Flyway migration script (e.g. `V<N>__create_...sql`).
  - Entity & Spring Data Repository definitions.
  - Service layer logic & background jobs (JobRunr queue, Redis caching/invalidation).
  - REST Controller endpoints, DTO models, and Spring Security configurations (`SecurityConfig.kt`).
- **Verification Plan**:
  - Static analysis & tests (`./gradlew ktlintCheck detekt test`).
  - Port 8081 health check and curl endpoint verification.

### 5. `+ Desktop Sub-Issue` (If Needed) Architecture Spec
- **Tech Stack**: Compose Multiplatform Desktop target (`:desktopApp`), JavaFX WebKit browser (`PlatformWebView.jvm.kt`), desktop window management, keyboard shortcuts.
- **Components & Files**:
  - Desktop App: `desktopApp/src/main/kotlin/com/thanh0x/coursedeals/desktop/` (`Main.kt`, `DesktopWebView.kt`, `DesktopLaunchConfig.kt`).
  - KMP Shared: `shared/src/jvmMain/kotlin/` platform bindings.
- **Verification Plan**:
  - Desktop unit and integration tests (`./gradlew :desktopApp:test`).
  - Desktop Maestro headless test runner (`DesktopFullAppInteractiveE2eTest.kt`).

---

## Case A: Creating Brand-New Sub-Issues

Use a temporary markdown file to avoid shell expansion issues, create the issue without `--project`, then attach via the Sub-issues API:

```bash
# 1. Create the sub-issue (deliberately OMIT --project)
gh issue create -R Huythanh0x/course_deals_ComposeMultiplatform \
  --title "<title>" \
  --label "type: <x>" --label "priority: <x>" --label "area: <x>" --label "size: <x>" \
  --body-file /path/to/sub_issue_body.md

# 2. Get the new sub-issue's numeric database id
SUB_ID=$(gh api repos/Huythanh0x/course_deals_ComposeMultiplatform/issues/<new-child-number> -q '.id')

# 3. Attach it to the parent issue
gh api repos/Huythanh0x/course_deals_ComposeMultiplatform/issues/<parent-number>/sub_issues \
  -X POST -F sub_issue_id="$SUB_ID"
```

## Case B: Attaching an Already-Existing Issue as a Sub-Issue

```bash
SUB_ID=$(gh api repos/Huythanh0x/course_deals_ComposeMultiplatform/issues/<child-number> -q '.id')

gh api repos/Huythanh0x/course_deals_ComposeMultiplatform/issues/<parent-number>/sub_issues \
  -X POST -F sub_issue_id="$SUB_ID"
```

## Board Cascade Cleanup (Run After Every Sub-Issue Addition)

GitHub auto-adds a sub-issue to the board the instant its parent is already a board item. Always verify and delete it from the board:

```bash
ITEM_ID="$(gh api graphql -f query='
  query { user(login: "Huythanh0x") { projectV2(number: 4) { items(first: 100) {
    nodes { id content { ... on Issue { number repository { name } } } } } } } }' \
  -q ".data.user.projectV2.items.nodes[] | select(.content.repository.name == \"course_deals_ComposeMultiplatform\" and .content.number == <child-issue-number>) | .id")"

[ -n "$ITEM_ID" ] && gh project item-delete 4 --owner Huythanh0x --id "$ITEM_ID"
```

## Branch Creation

Sub-issues get branches following standard naming: `<type>/<issue-number>-<slug>` branched from latest `main`.
