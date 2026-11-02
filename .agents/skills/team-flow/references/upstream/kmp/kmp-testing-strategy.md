---
name: kmp-testing-strategy
description: Cross-target testing rules for Course Deals (`commonTest`, `jvmTest`, `testLocalDebugUnitTest`, `:desktopApp:test`, and `:shared:linkDebugFrameworkIosSimulatorArm64`). Covers fakes-first repository testing, Ktor `MockEngine`, Kotlin/Native test naming rules, Compose resources in JVM tests, and `kotlinx-coroutines-test`.
source: https://github.com/filippkowalski/kmp-skills-by-filip/blob/main/skills/kmp-testing-strategy/SKILL.md (Aligned to Course Deals)
---

# Course Deals KMP Testing Strategy

Aligned to `course_deals_ComposeMultiplatform` (`commonTest`, `jvmTest`, `testLocalDebugUnitTest`, `:desktopApp:test`, and `:shared:linkDebugFrameworkIosSimulatorArm64`).

## 1. Verify Both JVM/Android Unit Tests and iOS Native Framework Linking

A green JVM test run does not guarantee `iosMain` or Kotlin/Native compiles cleanly. Always run:
```bash
# 1. Android / JVM unit tests across modules
./gradlew testLocalDebugUnitTest
./gradlew :data:coupons:jvmTest :data:user:jvmTest

# 2. Desktop Compose UI & unit tests (when :desktopApp or shared UI changes)
./gradlew :desktopApp:test

# 3. iOS Simulator Kotlin/Native framework compilation & linking gate
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

## 2. Never Put Commas (`,`) in Backticked Test Names

- **Symptom:** `testLocalDebugUnitTest` (JVM) passes, but Kotlin/Native iOS test compilation fails with `error: name contains illegal characters: ","`.
- **Rule:** Never use commas in backticked test names. Use `and`, `when`, or `then` instead:
  ```kotlin
  // GOOD
  @Test
  fun `login with valid email and otp updates state to authenticated`() = runTest { ... }
  ```

## 3. Fakes Behind Interfaces (`:domain:*`) Over `expect class`

- Define repository and service contracts as `interface` in `:domain:coupons` / `:domain:user` (`commonMain`).
- Implement hand-written fakes (`FakeCouponRepository`, `FakeUserRepository`, `InMemorySettingsStore`) for ViewModel and UseCase tests rather than mocking frameworks.

## 4. Test `:data:*` Network Repositories with Ktor `MockEngine`

Test real Ktor data sources and serialization edge cases (200 OK, 400 validation errors, 401 refresh, 500 server errors, unknown enum values) using `HttpClient(MockEngine { ... })` with the same `ContentNegotiation` JSON configuration as `:core:network`.

## 5. Keep Compose Resources (`getString(Res.string.*)`) Out of ViewModel Logic

- Unconfigured JVM unit tests lack an Android `Context` asset stream for `getString(Res.string.*)`.
- Emit typed state/enums or resource keys from ViewModels in `:feature:*` and resolve localized Compose strings inside `@Composable` UI functions.

## Checklist
- [ ] `testLocalDebugUnitTest` and module `jvmTest` suites pass with non-zero test counts
- [ ] `:shared:linkDebugFrameworkIosSimulatorArm64` passes after any `commonMain` or `iosMain` change
- [ ] Zero commas (`,`) in backticked `@Test` function names
- [ ] Fakes used for `:domain:*` interfaces; `MockEngine` used for `:data:*` Ktor tests
- [ ] Every 4-quadrant scenario (Happy, Failed, Error, Related) has unit test coverage
