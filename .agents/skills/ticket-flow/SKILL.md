---
name: ticket-flow
description: Runs the full ticket lifecycle as a gated, stateful sequence of steps — read-requirements, scan-related-code, confirm-edge-cases, implement-change, run-tests, capture-evidence, ship-pr — stopping after every step to report what was done and asking whether to continue or rework it. Use when the user wants to implement an existing GitHub issue end-to-end with checkpoints.
---

# Ticket flow

Orchestrates the 6 step-skills below plus `ship-pr`, in order, over one persistent
per-ticket workspace file. Each step-skill is also independently invocable on its own —
this skill's job is only the sequencing, the stop-and-report gate, and the state that
makes the whole thing resumable.

## The sequence

1. `new-ticket` — **only if no issue exists yet** for what the user described (skip if
   they gave you an existing issue number)
2. `read-requirements`
3. `scan-related-code`
4. `confirm-edge-cases`
5. `implement-change`
6. `run-tests`
7. `capture-evidence`
8. `ship-pr`

## Ticket workspace (the state)

One file per ticket: `.agents/tickets/<issue-number>-<slug>/NOTES.md` (git-ignored —
working memory, not project documentation). **Always read it first if it already
exists** and resume from the last completed step rather than restarting. Create it from
this template if it doesn't exist:

```markdown
# Issue #<N>: <title>

## Requirements
<filled by read-requirements>

## Code scan
<filled by scan-related-code>

## Clarifications
- Q: ...
  A: ...
<filled by confirm-edge-cases>

## Decisions
<filled by implement-change, and confirm-edge-cases for chosen defaults>

## Progress log
- <timestamp via `date`> <what was done>
<appended to by every step>

## Test evidence
### Unit tests
<filled by run-tests>
### E2E / manual verification
<filled by capture-evidence>

## PR
<filled by ship-pr>
```

## Running each step

For every step in the sequence:

1. Follow that step's own `SKILL.md` — it defines the procedure and what it writes to
   the workspace file.
2. **Stop and report**, in this exact shape:

   ```markdown
   ### Step: <step-name> — done

   | Tool | Target | Outcome |
   |---|---|---|
   | <tool used> | <file/command> | <one-line result> |
   ... one row per tool call made during this step ...

   **Changed:** <what changed in the workspace file / repo, one line>
   **Confirmed:** <decisions made or questions answered this step, or "none">
   **Next:** <next step name> — <one line on what it will do>
   ```

3. Then use `ask_question` with exactly these options (plus the automatic write-in
   option for free text):
   - **Continue to `<next step>`** — proceed
   - **Rework this step** — the write-in free-text answer carries the detailed
     instruction for what to redo; go back into the current step's procedure with that
     instruction applied, produce a fresh report for the *same* step, and ask again.
     Do not silently advance past a rework request.
4. Only advance once the user picks "Continue".

The final step, `ship-pr`, gates the same way: report the opened PR URL in the same
table format, then ask if anything else is needed before considering the ticket done.
**`merge-pr` is deliberately not part of this sequence** — landing to `main` needs its
own explicit go-ahead once CI is actually green, not an automatic "Continue" after
opening the PR. Point the user at it once CI passes if they want it merged now.

## Resuming an interrupted flow

If asked to continue a ticket that already has a workspace file, read it, find the
last step with content, and resume from the *next* one — don't re-run completed steps.
