---
name: kmp-ios-build
description: Build, link, and bridge the iOS host (`iosApp/`) and KMP `ComposeApp.framework` (`:shared`) in `course_deals_ComposeMultiplatform`. Covers `MainViewControllerKt.MainViewController()`, Swift↔Kotlin `suspend` `completionHandler` rules, JSON string bridging, Keychain reinstall persistence, `UILaunchScreen`, and Debug/Release entitlements.
source: https://github.com/filippkowalski/kmp-skills-by-filip/blob/main/skills/kmp-ios-build/SKILL.md & felipechaux/kmp-compose-multiplatform-skill (Aligned to Course Deals)
---

# Course Deals iOS Host (`iosApp/`) & KMP `ComposeApp.framework` Interop

Aligned to `course_deals_ComposeMultiplatform` (`iosApp/iosApp.xcodeproj`, `:shared` -> `ComposeApp.framework`, `MainViewControllerKt.MainViewController()`).

## 1. Building & Linking `:shared` for iOS Simulator and Xcode

- Before running `xcodebuild` or launching `iosApp` in the iOS Simulator, always verify the Kotlin/Native framework links cleanly:
  ```bash
  ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
  ```
- Build `iosApp` from CLI for the iOS Simulator:
  ```bash
  xcodebuild -project iosApp/iosApp.xcodeproj \
    -scheme iosApp \
    -configuration Debug \
    -sdk iphonesimulator \
    -destination 'generic/platform=iOS Simulator' \
    CODE_SIGNING_ALLOWED=NO build
  ```

## 2. Swift Implementing a Kotlin `suspend fun`: Always Complete the Handler

- **Symptom:** The Kotlin coroutine caller in `:shared` hangs forever, or a soft unavailable feature throws an unhandled Kotlin exception.
- **Cause:** Kotlin exports `suspend fun fetchToken(): String?` to Swift as `fetchToken(completionHandler: @escaping (String?, Error?) -> Void)`. The coroutine only resumes when `completionHandler` is invoked.
- **Rule:** Call `completionHandler` **exactly once on every execution path** (including `guard` early returns and simulator-unsupported branches, passing `(nil, nil)` for optional fallbacks).

## 3. Crossing the Swift ↔ Kotlin Boundary Safely

- **Top-level Kotlin functions:** `MainViewController()` in `shared/src/iosMain/kotlin/.../MainViewController.kt` is called in Swift as `MainViewControllerKt.MainViewController()`.
- **Kotlin `object` singletons:** Accessed in Swift via `.shared` (`AppGraph.shared`).
- **Avoid `Any` with `JSONSerialization.isValidJSONObject`:** Swift `Codable` structs/enums return `false` from `JSONSerialization.isValidJSONObject`. Encode Swift structs with `JSONEncoder` to a UTF-8 JSON `String` when passing structured payloads to Kotlin, or use typed KMP bridge interfaces.
- **Avoid exposing `Unit` return types in public iOS callbacks:** Wrap functions so Swift closures don't have to return `KotlinUnit()`.

## 4. Keychain Persistence Across iOS App Reinstalls

- **Trap:** Deleting an iOS app removes `UserDefaults` (`NSUserDefaults` / `SettingsStore`), but **Keychain items survive uninstall**.
- **Rule:**
  - In code: if `SettingsStore` indicates a fresh install (`hasCompletedOnboarding == false`), clear or reconcile any leftover Keychain auth tokens so a reinstalled app does not silently use a stale session.
  - In QA / Simulator E2E tests: run `xcrun simctl keychain booted reset` alongside `xcrun simctl uninstall` when testing fresh-user onboarding.

## 5. App Store Connect & Entitlements Hygiene

- Keep `UILaunchScreen` configured in `iosApp/iosApp/Info.plist` (avoids `ITMS-90476`).
- Provide accurate `NS*UsageDescription` purpose strings and `PrivacyInfo.xcprivacy` declarations for every referenced privacy-sensitive API (avoids `ITMS-90683`).
- Keep `Debug` (`aps-environment = development`) and `Release` (`aps-environment = production`) entitlements in separate `.entitlements` files.

## Checklist
- [ ] `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` passes with zero linker errors
- [ ] Every Swift `completionHandler` for a Kotlin `suspend` bridge runs once on all paths
- [ ] Swift↔Kotlin payloads use primitives, JSON strings, or typed KMP interfaces
- [ ] Fresh-install onboarding reconciles Keychain vs. `NSUserDefaults`
- [ ] `Info.plist` has `UILaunchScreen` and required usage descriptions
