---
name: confirm-edge-cases
description: Identify ambiguities and edge cases in a ticket's requirements and code scan, and confirm them with the user before any code is written. Use after scan-related-code, standalone or as part of ticket-flow.
---

# Confirm edge cases

1. Read the workspace's Requirements and Code scan sections together.
2. Identify genuine ambiguities — missing acceptance criteria, unstated behavior for an
   edge case the existing code already has to handle elsewhere (e.g. database constraints,
   null safety, API error formats, Redis connection fallbacks). Don't manufacture questions
   about things a reasonable default clearly covers.
3. For each one found, decide:
   - **Low-risk, obvious default exists** → don't ask; record it directly in Decisions
     with the "why".
   - **Real consequences, or genuinely unclear** → ask via `ask_question`. Batch
     related questions into one call rather than several round-trips.
4. Append every question asked (and its answer) to Clarifications as it happens — not
   reconstructed afterward from memory:
   ```markdown
   - Q: Should expired coupons be excluded from crawler results?
     A: Yes, drop coupons whose expiration timestamp has passed before persisting.
   ```
5. Append one line to Progress log: `<date> confirmed N edge cases, M asked / K defaulted`.

## Output

Report the resolved list (question → answer/default) — `implement-change` should not
need to re-derive any of these.
