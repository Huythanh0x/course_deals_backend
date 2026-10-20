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
2. **Write the issue body** using the shape from `.github/ISSUE_TEMPLATE/bug_report.md`
   or `.github/ISSUE_TEMPLATE/feature_request.md` (whichever fits) — keep the same
   sections even for other types (what/why, environment/module, definition of done).
   **Describe only the observed problem, not a solution you've already worked out** —
   even if you (the assistant) already diagnosed the root cause or have a fix ready in
   this same session, the issue is the problem statement; investigation and the fix
   belong in the PR that closes it. An issue that already states "root cause is X, fix
   is Y" reads as pre-solved and duplicates what the PR description should say.
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
   - **Top-level** — add it to the board at creation, no separate step needed:
     ```bash
     gh issue create -R Huythanh0x/course_deals_backend \
       --title "<title>" \
       --label "type: <x>" --label "priority: <x>" --label "area: <x>" --label "size: <x>" \
       --project "Course Deals" \
       --body "<body>"
     ```
   - **Sub-issue** — link it under its parent, and deliberately **omit `--project`**
     (adding a sub-issue to the board separately from its parent defeats the point of
     grouping — only the parent should represent the work there):
     ```bash
     gh issue create -R Huythanh0x/course_deals_backend \
       --title "<title>" \
       --label "type: <x>" --label "priority: <x>" --label "area: <x>" --label "size: <x>" \
       --parent <parent-issue-number> \
       --body "<body>"
     ```
     GitHub auto-cascades a new sub-issue onto the project board anyway if its parent is
     already a board item, regardless of the `--project` flag. Check for this and remove
     it if it happened:
     ```bash
     NUM="$(gh project list --owner Huythanh0x --format json -q '.projects[] | select(.title=="Course Deals") | .number')"
     ITEM_ID="$(gh api graphql -f query='
       query($num: Int!) { user(login: "Huythanh0x") { projectV2(number: $num) { items(first: 100) {
         nodes { id content { ... on Issue { number repository { name } } } } } } } }' \
       -F num="$NUM" \
       -q ".data.user.projectV2.items.nodes[] | select(.content.repository.name == \"course_deals_backend\" and .content.number == <new-issue-number>) | .id")"
     [ -n "$ITEM_ID" ] && gh project item-delete "$NUM" --owner Huythanh0x --id "$ITEM_ID"
     ```
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
- Sub-issues are intentionally excluded from the board — only their parent represents
  the work there. This mirrors the manual cleanup done when the board was first
  backfilled (see `docs/project-board.md`).
- This skill's shape mirrors `course_deals_ComposeMultiplatform`'s `new-ticket` skill —
  keep them in sync if either changes (e.g. board mechanics, label taxonomy).
