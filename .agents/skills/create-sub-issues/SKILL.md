---
name: create-sub-issues
description: Create one or more GitHub sub-issues under an existing parent issue (new or already-existing issues), link them via the Sub-issues API, and clean up GitHub's board auto-cascade. Use when the user wants to break a tracked issue into child tasks, or attach existing standalone issues as children of a parent.
---

# Create sub-issues

Sub-issues are **intentionally excluded from the "Course Deals" board** — only a
parent issue represents the work there (see `docs/project-board.md`). Every path below
ends with the same board-cascade check for that reason.

## Case A: creating brand-new sub-issues

```bash
gh issue create -R Huythanh0x/course_deals_backend \
  --title "<title>" \
  --label "type: <x>" --label "priority: <x>" --label "area: <x>" --label "size: <x>" \
  --parent <parent-issue-number> \
  --body "<body>"
```

Deliberately **omit `--project`** — adding a sub-issue to the board separately from
its parent defeats the point of grouping the work under one card.

## Case B: attaching an already-existing issue as a sub-issue

```bash
SUB_ID=$(gh api repos/Huythanh0x/course_deals_backend/issues/<child-number> -q '.id')

gh api repos/Huythanh0x/course_deals_backend/issues/<parent-number>/sub_issues \
  -X POST -f sub_issue_id="$SUB_ID"
```

## Board cascade cleanup (run after either case)

GitHub auto-adds a sub-issue to the board the instant its parent is already a board
item, regardless of how the sub-issue was created or whether `--project` was passed.
Check for it and remove it if it happened:

```bash
NUM="$(gh project list --owner Huythanh0x --format json -q '.projects[] | select(.title=="Course Deals") | .number')"
ITEM_ID="$(gh api graphql -f query='
  query($num: Int!) { user(login: "Huythanh0x") { projectV2(number: $num) { items(first: 100) {
    nodes { id content { ... on Issue { number repository { name } } } } } } } }' \
  -F num="$NUM" \
  -q ".data.user.projectV2.items.nodes[] | select(.content.repository.name == \"course_deals_backend\" and .content.number == <child-issue-number>) | .id")"
[ -n "$ITEM_ID" ] && gh project item-delete "$NUM" --owner Huythanh0x --id "$ITEM_ID"
```

## Branch creation

Sub-issues get branches the same way any other issue does — see `new-ticket`'s branch
step (`<type>/<issue-number>-<slug>` off latest `main`).
