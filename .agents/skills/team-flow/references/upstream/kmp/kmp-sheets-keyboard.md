---
name: kmp-sheets-keyboard
description: Material 3 ModalBottomSheet, window insets, IME soft keyboard, FocusRequester timing, and BackHandler rules for Course Deals Compose Multiplatform screens (`:core:ui` and `:feature:*`) across Android, iOS, and Desktop.
source: https://github.com/filippkowalski/kmp-skills-by-filip/blob/main/skills/kmp-sheets-keyboard/SKILL.md (Aligned to Course Deals)
---

# Course Deals Compose Multiplatform: Sheets, Keyboard, Focus & Insets

Aligned to `course_deals_ComposeMultiplatform` (`:core:ui` and `:feature:home`, `:feature:detail`, `:feature:auth`, `:feature:profile`, `:feature:course`, `:feature:enroll`).

## 1. Cap `ModalBottomSheet` Content Height (~85%) and Make Forms Scrollable

- **Symptom:** Filter, Submit Deal, or Report Course `ModalBottomSheet` jitters or re-solves anchors endlessly when the soft keyboard opens.
- **Fix:** Cap sheet content height (`Modifier.fillMaxWidth().fillMaxHeight(0.85f)`) or wrap form rows in `verticalScroll(rememberScrollState())` with `navigationBarsPadding()` and `imePadding()`.

## 2. Guard `FocusRequester.requestFocus()` Inside `LaunchedEffect`

- **Symptom:** Opening the OTP or Search screen crashes on iOS or Android when `focusRequester.requestFocus()` runs before the `TextField` layout node is attached.
- **Cause:** Compose does not catch exceptions thrown inside `LaunchedEffect`; an unattached `FocusRequester` throws `IllegalStateException`, terminating the process.
- **Fix:** Delay briefly or guard `runCatching { focusRequester.requestFocus() }` after attachment.

## 3. Safe Area & System Bar Insets Across Android, iOS & Desktop

- Always respect top status bar / Dynamic Island (`statusBarsPadding()` or `Scaffold` `contentWindowInsets`) and bottom gesture bar (`navigationBarsPadding()`).
- In `iosApp/iosApp/ContentView.swift`, `ComposeView().ignoresSafeArea(.all)` delegates safe-area handling to Compose Multiplatform—never double-apply safe-area padding in both SwiftUI and Compose.

## 4. Semantic Test Tags (`Modifier.testTag`) Inside Sheets and Dialogs

- Always attach `Modifier.testTag("<tag>")` and clear text labels to interactive controls inside `ModalBottomSheet` and `AlertDialog` so Maestro E2E flows (`.maestro/flows/*.yaml`) and Desktop `runComposeUiTest` can target them reliably across Android, iOS, and Desktop.

## Checklist
- [ ] `ModalBottomSheet` content scrolls cleanly when IME keyboard opens
- [ ] `FocusRequester.requestFocus()` is guarded against unattached node throws
- [ ] `statusBarsPadding()` and `navigationBarsPadding()` verified on Android (`emulator-5554`) and iOS (`iPhone Simulator`)
- [ ] `Modifier.testTag("<tag>")` attached to sheet/dialog CTAs and inputs
