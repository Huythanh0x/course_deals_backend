---
name: run-tests
description: Run unit tests, detekt, and ktlint for a ticket's changed modules and record real results into its workspace. Use after implement-change, standalone or as part of ticket-flow.
---

# Run tests

1. Identify affected modules from the workspace's Code scan section (or from
   `git diff --stat` if resuming without a fresh scan).
2. Run, per `AGENTS.md`'s Build & verify commands:
   - Module unit tests: `./gradlew :modules:<module-name>:test` or full `./gradlew clean test`
   - Static analysis: `./gradlew ktlintCheck detekt` (or `./gradlew ktlintFormat` to fix formatting)
3. Record real pass/fail counts and report paths (`**/build/reports/tests/**`,
   `**/build/reports/detekt/**`) into Test evidence → Unit tests. Don't round
   up or assume — quote the actual numbers from the command output.
4. If a real failure surfaces: fix it and re-run, don't report a failure as if it were
   acceptable to carry forward. If a fix isn't straightforward, stop here and report the
   failure honestly rather than proceeding to the next step with red tests.
5. Append one line to Progress log: `<date> ran tests — <pass/fail summary>`.

## Output

Report the pass/fail summary — `capture-evidence` runs next, and shouldn't proceed past
a red test suite.
