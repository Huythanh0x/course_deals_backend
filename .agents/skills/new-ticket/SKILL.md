---
name: new-ticket
description: Create a GitHub issue with this repo's label taxonomy and the matching branch, following docs/project-management.md and docs/branching.md. Use when the user wants to file a new issue/ticket/bug/chore for this repo, or asks to start work on something that doesn't have an issue yet.
---

# New ticket

Creates a GitHub issue in the right shape, then the branch to work on it — mirrors the
flow this repo's own docs prescribe.

## Steps

1. **Pick the type** from the user's request: `feature`, `bug`, `chore`, `docs`,
   `refactor`, `test`, or `design`. If unclear, ask.
2. **Write the issue body**:
   - For **`feature` issues**:
     - **Mandatory Project**: Must be tracked under GitHub Project [`Course Deals`](https://github.com/users/Huythanh0x/projects/4) (`--project "Course Deals"`).
     - **Solution Architecture Plan**: Top-level feature issues must include an explicit architecture plan covering:
       - Problem statement & core motivation.
       - System architecture diagram / component flow across repos (Backend & Android/KMP).
       - API / DTO contract specification (endpoints, request/response models, error handling).
       - Definition of Done (DoD) verification matrix.
     - **Sub-Issues Breakdown**: For full-stack or multi-component features, generate corresponding sub-issues for each domain (e.g. Mobile and Backend) containing their own detailed architecture & tech stack design (see `create-sub-issues`).
   - For **`bug` issues**:
     - Focus on observed vs expected behavior, reproduction steps, environment/logs.
   - Use `.github/ISSUE_TEMPLATE/bug_report.md` or `.github/ISSUE_TEMPLATE/feature_request.md` structure.
3. **Pick labels**, exactly one from each category (see `docs/project-management.md`
   for the full taxonomy):
   - `type: <feature|bug|chore|docs|refactor|test|design>`
   - `priority: <P0|P1|P2|P3>` — ask if not obvious from context
   - `area: <android|backend|web|design|testing|ci|infra>` — usually `area: backend`
     for this repo
   - `size: <1|2|3|5|8|13>` — if it feels like `13`, say so and suggest splitting
     before creating the issue (per `docs/project-management.md`'s sizing guidance)
4. **Decide top-level issue vs. sub-issue**, per `docs/project-board.md`: only
   top-level work items belong on the shared "Course Deals" board — if the user's
   request is a child task of an already-tracked parent issue (they reference a parent,
   or you're splitting an oversized issue per the `size: 13` guidance), this is a
   sub-issue. Ask if it's ambiguous rather than guessing.
5. **Create the issue**:
   - **Top-level** — add it to the project board at creation (mandatory for features):
     ```bash
     gh issue create -R Huythanh0x/course_deals_backend \
       --title "<title>" \
       --label "type: <x>" --label "priority: <x>" --label "area: <x>" --label "size: <x>" \
       --project "Course Deals" \
       --body "<body>"
     ```
   - **Sub-issue** — hand off to the `create-sub-issues` skill for the actual creation
     (`--parent`, deliberately no `--project`, including detailed tech stack architecture)
     and the board auto-cascade cleanup it documents — don't duplicate those mechanics here.
6. **Create the branch** off the latest `main`, per `docs/branching.md`'s naming
   convention `<type>/<issue-number>-<short-slug>`:
   ```bash
   git checkout main && git pull origin main
   git checkout -b <type>/<issue-number>-<short-slug>
   ```
7. Report the issue URL, branch name, and whether it landed on/off the board back to
   the user before starting any code changes.

## Notes

- Never branch off another feature branch — always from latest `main`
  (`docs/branching.md`).
- Confirm with the user before creating the issue if the title/labels involved any
  real judgment call (priority, size) rather than assuming.
- `gh issue create --project`/`--parent` requires the `project` scope on the
  authenticated `gh` token (`gh auth refresh -s project` if missing).
- Project URL: [Course Deals Project #4](https://github.com/users/Huythanh0x/projects/4).
- Sub-issues are intentionally excluded from the board — only their parent represents
  the work there. See `create-sub-issues` for the full mechanics, including attaching
  an issue that already existed before its parent did.
- If 2+ of the issues being filed here are independent enough to implement in parallel,
  consider `dispatch-ticket-worktree` once their branches exist.
