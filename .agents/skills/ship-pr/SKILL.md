---
name: ship-pr
description: Push a committed branch and open a PR from this repo's template, linking Closes #N, following docs/pull-requests.md. Use when work on a branch is done and ready to open (or update) a pull request for this repo.
---

# Ship PR

Pushes a finished branch and opens a PR in the shape `docs/pull-requests.md` and
`.github/PULL_REQUEST_TEMPLATE.md` expect.

## Before opening

Run the self-review checklist from `docs/pull-requests.md` for real, not as a
rubber stamp:
- Read the full diff top to bottom as if it were someone else's PR
- Does every changed file actually belong in this PR?
- Any leftover debug code, commented-out blocks, stray `println`?
- Do tests actually cover the new behavior (if tests were added)?
- Confirm the branch's commit messages follow Conventional Commits.

## Steps

1. Push the branch: `git push -u origin <branch>` (first push) or `git push` (updates).
2. Confirm which issue this closes — every PR must link one via `Closes #N`
   (`docs/pull-requests.md`).
3. Open the PR filled from `.github/PULL_REQUEST_TEMPLATE.md` — fill in **all** of it,
   including "How this was tested" with what was actually run. If this ticket has a
   workspace at `.agents/tickets/<issue-number>-<slug>/NOTES.md` (from `run-tests`/`capture-evidence`),
   pull real numbers and outputs from its Test evidence section:
   ```bash
   gh pr create -R Huythanh0x/course_deals_backend \
     --title "<type>(<scope>): <description>" \
     --body "$(cat <<'EOF'
   ## Summary
   ...

   ## Changes
   - ...

   ## Related issue
   Closes #<N>

   ## How this was tested
   - [ ] ...

   ## Checklist
   - [ ] Self-reviewed the diff top to bottom
   - [ ] Commit message(s) follow Conventional Commits
   - [ ] Docs/ADR updated if this changes architecture or setup
   - [ ] CI green (ktlint + detekt + build + tests)
   EOF
   )"
   ```
4. **Mirror the linked issue's labels onto the PR**:
   ```bash
   labels=$(gh issue view <N> -R Huythanh0x/course_deals_backend --json labels -q '.labels[].name')
   args=()
   while IFS= read -r l; do args+=(--add-label "$l"); done <<< "$labels"
   gh pr edit <PR-number> -R Huythanh0x/course_deals_backend "${args[@]}"
   ```
5. Report the PR URL back to the user.

## After CI runs

- Check status with `gh pr checks <N> -R Huythanh0x/course_deals_backend`.
- If CI fails, diagnose from the actual failure logs (`gh run view <run-id> --log-failed`)
  before proposing a fix — don't guess.
- Once CI is green and the user wants it landed, use the standalone `merge-pr` skill.
