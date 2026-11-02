---
name: kmp-app-architecture
description: App-layer rules for Course Deals Kotlin Multiplatform + Compose Multiplatform (`:shared`, `:app`, `:desktopApp`, `iosApp`, `watchApp`) using Koin DI and StateFlow ViewModels. Covers composition roots (`MainActivity`, `MainViewController`, Desktop `Main.kt`), `expect`/`actual` vs. `domain:*` interfaces vs. Swift bridges, coroutine suspension ownership checks, `_uiState.update { }` snapshot merging, and deep-link routing.
source: https://github.com/filippkowalski/kmp-skills-by-filip/blob/main/skills/kmp-app-architecture/SKILL.md (Aligned to Course Deals)
---

# Course Deals KMP App Architecture: Object Graph, State Updates & Entry Points

Aligned to `course_deals_ComposeMultiplatform` (Kotlin 2.4.10, Compose Multiplatform 1.8.0, Koin DI, Clean Architecture across `:core:*`, `:domain:*`, `:data:*`, `:feature:*`, `:shared`).

## 1. Build the Koin / Object Graph Outside Composition Lambdas

- **Symptom:** Duplicate `HttpClient`, `SettingsStore`, or Room driver instances; ViewModel state resets on recomposition or configuration change.
- **Cause:** Lambdas passed to `setContent { }`, `ComposeUIViewController { }`, and Desktop `Window { }` can re-run.
- **Course Deals Pattern:**
  - Initialize Koin (`startKoin { modules(appModules) }`) **once per process** in Android `CourseDealsApplication.onCreate()`, iOS `MainViewController` startup helper, and Desktop `main()` before `application { }`.
  - `MainActivity`, `MainViewController()`, and Desktop `Main.kt` only wire platform capabilities (`PlatformActions`, `customPlatformWebViewRenderer`, CLI `DesktopLaunchConfig`) into `CourseDealsApp(...)`.

## 2. Choosing `expect`/`actual` vs. `commonMain` Interface vs. Swift Bridge

Kotlin/Native imports Objective-C (`@objc`) only; Swift-only Apple frameworks (`WidgetKit`, `ActivityKit`, `WatchConnectivity` Swift helpers, `CryptoKit`) are invisible to Kotlin `iosMain`. And `expect class` with divergent constructors breaks `commonTest`.

| Need in Course Deals | Mechanism |
|---|---|
| Stateless platform fact or helper (`Platform`, dispatcher, base URL, build flavor) | `expect`/`actual` in `:core:common` or `:core:network` |
| Stateful service or repository (`SettingsStore`, `CouponRepository`, `UserRepository`, `PlatformActions`) | **Interface in `commonMain`** (`:domain:*` or `:core:*`), platform class bound in Koin (`jvmMain` / `androidMain` / `iosMain`), fake in `commonTest` |
| Native Swift-only API (WatchConnectivity `WCSession`, WidgetKit reload, Passkeys / Apple Sign-In) | **Kotlin interface in `:shared` / `:core:ui`**, implemented in Swift inside `iosApp/` and passed into `MainViewController(...)` |

## 3. Check Ownership After Every Coroutine Suspension

- **Symptom:** A response for a signed-out user or previous filter chip writes into the new user/filter state; a double tap triggers two concurrent requests and the stale one overwrites the newer one.
- **Cause:** Other coroutines run at every suspension point, including on `Dispatchers.Main`.
- **Course Deals Pattern:**
  - Capture the active owner/filter generation before suspending, or cancel the previous `Job` (`searchJob?.cancel()`) before launching a new query in `viewModelScope`.
  - Verify ownership after `repository.*()` suspends before updating `_uiState`.

## 4. Merge Focused Results Into Live State via `_uiState.update { }`

- **Symptom:** After a background refresh or pagination load completes, a dialog/sheet (`isFilterSheetVisible`, `isReportDialogVisible`) opened by the user while the request was in flight suddenly closes.
- **Cause:** Taking a snapshot `val current = _uiState.value` before a suspend call and assigning `_uiState.value = current.copy(...)` afterward overwrites every UI field that changed during the suspension.
- **Course Deals Pattern:**
  ```kotlin
  private val _uiState = MutableStateFlow(FeatureUiState())
  val uiState = _uiState.asStateFlow()

  // ALWAYS merge inside _uiState.update { current -> ... } after suspension:
  _uiState.update { current ->
      current.copy(
          coupons = result.items,
          isLoading = false,
          errorMessage = null
      )
  }
  ```

## 5. Run Multi-Step Sequences on `viewModelScope`, Not Composition Scopes

- **Symptom:** Leaving a screen mid-save (tab switch, bottom sheet dismiss, rotation) cancels an in-flight bookmark/report/auth mutation.
- **Cause:** `LaunchedEffect` and `rememberCoroutineScope` are cancelled as soon as the composable leaves composition.
- **Course Deals Pattern:**
  - Trigger mutations via `viewModel.onAction(...)` running on `viewModelScope`.
  - Await sequential steps as `suspend` functions inside a single `viewModelScope.launch { }` block rather than spawning sibling `launch` blocks.

## Checklist
- [ ] Koin & network/storage clients initialized once per process outside `setContent` / `ComposeUIViewController`
- [ ] `commonMain` interfaces used for stateful/testable services; Swift bridge interfaces used for Swift-only Apple SDKs
- [ ] Active user/filter generation verified after suspension points
- [ ] All ViewModel state transitions use `_uiState.update { current -> current.copy(...) }`
- [ ] Ordered write/refresh sequences run inside a single `viewModelScope` job
