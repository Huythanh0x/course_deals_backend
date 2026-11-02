---
name: swiftui-pro
description: SwiftUI, Swift 6+ Concurrency, and watchOS (`watchApp` + `WatchConnectivity` `WCSession`) rules for Course Deals (`iosApp/` & `watchApp/`). Distilled from Paul Hudson's `twostraws/SwiftUI-Agent-Skill`, Antoine van der Lee's `avdlee/swiftui-agent-skill`, and `dpearson2699/swift-ios-skills`, tailored for Course Deals iOS and Apple Watch targets.
source: https://github.com/twostraws/SwiftUI-Agent-Skill & https://github.com/avdlee/swiftui-agent-skill (Aligned to Course Deals)
---

# Course Deals SwiftUI, Swift Concurrency & watchOS Guide (`iosApp` & `watchApp`)

Aligned to `course_deals_ComposeMultiplatform` (`iosApp/` Xcode host wrapping `ComposeApp.framework`, native iOS extensions, and `watchApp/` Apple Watch companion).

## 1. Modern SwiftUI APIs & View Invalidation Boundaries

- Treat each `View` struct as an invalidation boundary: pass only the properties it reads.
- `@State` and `@FocusState` properties MUST be `private`.
- Use `@Observable` classes with `@State` (when owned by the view) and `@Bindable` (when injected and needing `$` bindings).
- Replace deprecated modifiers:
  - `.foregroundStyle(...)` instead of `.foregroundColor(...)`
  - `.clipShape(.rect(cornerRadius: ...))` instead of `.cornerRadius(...)`
  - `NavigationStack` instead of `NavigationView`
  - `.animation(_:value:)` (always specify `value:`)
- Never construct `Binding(get:set:)` with side effects inside `body`; bind directly to state and trigger side effects in `.onChange(of:)`.
- Give every icon button a descriptive VoiceOver label (`Button("Bookmark Deal", systemImage: "bookmark", action: ...)`), support Dynamic Type, and set `.accessibilityIdentifier("<tag>")` matching `.maestro/flows/*.yaml`.

## 2. Swift 6+ Concurrency & Actor Isolation

- Annotate UI state holders and `WCSessionDelegate` / view callbacks that mutate UI state with `@MainActor`.
- Prefer immutable `struct` / `enum` value types conforming to `Sendable` when crossing task boundaries.
- Offload heavy parsing or image work with `@concurrent` or background actors, never blocking the main thread.

## 3. Apple Watch (`watchApp/`) & `WatchConnectivity` (`WCSession`)

- **Glanceable watchOS UX:**
  - Use `NavigationStack` and vertical paging `TabView(.verticalPage)` for fast browsing of active course deals and expiring coupons on 40mm–49mm Apple Watch displays.
  - Support `.digitalCrownRotation(...)` where scrolling/filtering applies.
  - Respect Always-On Display (AOD) reduced luminance (`@Environment(\.isLuminanceReduced)`) and `TimelineView(.everyMinute)` for countdown badges.
- **Bidirectional iPhone ↔ Apple Watch Sync (`WCSession`):**
  - Always check `WCSession.isSupported()` before configuring `WCSession.default.delegate` and calling `WCSession.default.activate()` in both `iosApp` and `watchApp`.
  - Use `updateApplicationContext(_:)` for latest state snapshots (authenticated user status, bookmarked/top deals list).
  - Use `transferUserInfo(_:)` for guaranteed queued events (e.g. bookmark toggle or deal claim queued while iPhone is unreachable).
  - Use `sendMessage(_:replyHandler:errorHandler:)` only when `WCSession.default.isReachable == true` for immediate foreground sync.
  - Hop to `Task { @MainActor in ... }` inside `WCSessionDelegate` methods before updating `@Observable` state.

## 4. Swift Testing (`@Test`, `#expect`, `#require`)

- Write native iOS/watchOS unit tests using the modern `Testing` framework (`import Testing`, `@Test`, `#expect(...)`, `#require(...)`).

## Checklist
- [ ] `@State` and `@FocusState` are `private`; `@Observable` + `@Bindable` used for native SwiftUI state
- [ ] Zero deprecated SwiftUI modifiers (`foregroundColor`, `cornerRadius`, `NavigationView`, parameterless `animation`)
- [ ] `.accessibilityIdentifier("<tag>")` and VoiceOver labels present on native controls
- [ ] `WCSession.isSupported()` guarded and delegate mutations isolated to `@MainActor`
- [ ] `xcodebuild` succeeds for `iosApp` (and `watchApp` when modified)
