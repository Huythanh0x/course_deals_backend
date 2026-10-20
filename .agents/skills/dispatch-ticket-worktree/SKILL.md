---
name: dispatch-ticket-worktree
description: Set up an isolated git worktree for one ticket and launch a background agent to implement it end-to-end (through a local commit), for running multiple independent tickets in parallel. Use when 2+ issues need implementing at the same time and don't touch overlapping files.
---

# Dispatch ticket to a worktree agent

Each ticket gets its own git worktree and background agent that works unattended through
implementation and local build/test verification, then stops before anything that touches GitHub.

## Steps

1. **Create the worktree**, one per ticket, off that ticket's own branch:
   ```bash
   ID=$(openssl rand -hex 8)
   git worktree add ".agents/worktrees/agent-$ID" <type>/<issue-number>-<slug>
   ```
2. **Compose a fully self-contained agent prompt**:
   - The exact worktree path as working directory.
   - Pointer to read `AGENTS.md` in full first.
   - The issue's title, labels, and verbatim body.
   - Ticket workspace template (`.agents/tickets/<issue-number>-<slug>/NOTES.md`).
   - Two hard boundaries:
     1. *No interactive `ask_question`* — pick reasonable defaults consistent with existing code and record them in Decisions.
     2. *No `git push`, no `gh pr create`* — stop after a clean local `git commit` (Conventional Commits, `Closes #N`).
   - Build & test commands: `./gradlew clean test`, `./gradlew ktlintCheck detekt`.
3. **Launch the subagent or background task**.
4. **After agent reports back**: review report, then drive `capture-evidence` and `ship-pr`.
