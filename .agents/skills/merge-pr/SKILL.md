---
name: merge-pr
description: Merge an open, CI-green PR for this repo — squash-merge with a rewritten Conventional Commits message, then confirm the branch auto-deleted and the linked issue auto-closed. Use when a PR is ready to land, not automatically as part of ship-pr.
---

# Merge PR

`ship-pr` opens a PR and stops there deliberately — merging to `main` is a
shared-state, hard-to-reverse action, so it's always its own explicit step, never
auto-chained after opening a PR or after CI merely starts running. Only invoke this
when the user actually wants a specific PR merged now.

## Steps

1. **Check the PR is actually mergeable and CI is green**:
   ```bash
   gh pr view <N> -R Huythanh0x/course_deals_backend --json mergeable,mergeStateStatus
   gh pr checks <N> -R Huythanh0x/course_deals_backend
   ```
   If `mergeStateStatus` is `BEHIND`, bring it up to date with a merge:
   ```bash
   git fetch origin
   git merge origin/main
   git push
   ```

2. **Compose a clean squash commit message**:
   Read the PR (`gh pr view <N> -R Huythanh0x/course_deals_backend`). Write one
   Conventional Commits line plus a short body if the PR summary adds context.

3. **Merge**:
   ```bash
   gh pr merge <N> -R Huythanh0x/course_deals_backend \
     --squash \
     --subject "<type>(<scope>): <description>" \
     --body "<optional body>"
   ```

4. **Confirm**:
   ```bash
   gh pr view <N> -R Huythanh0x/course_deals_backend --json state,mergedAt,mergeCommit
   gh issue view <linked-issue-number> -R Huythanh0x/course_deals_backend --json state,stateReason
   ```

5. **Local cleanup**:
   ```bash
   git worktree remove <path>       # if in a worktree
   git branch -d <branch-name>      # if a local branch
   git fetch --prune
   ```

6. Report the merge commit SHA and confirmations back to the user.
