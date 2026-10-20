---
name: adr
description: Draft a new Architecture Decision Record under docs/adr/ following this repo's adr-guide.md template and naming convention. Use when a real architectural tradeoff has been decided or needs recording.
---

# ADR

Drafts a new ADR matching `docs/adr-guide.md`.

## Steps

1. **Check it's ADR-worthy** per `docs/adr-guide.md`.
2. **Find the next number**: list `docs/adr/*.md`, take the highest `NNNN-` prefix, add one, zero-pad to 4 digits.
3. **Write `docs/adr/NNNN-short-title.md`** using the template from `docs/adr-guide.md`:
   ```markdown
   # NNNN: <short title>

   ## Status
   Proposed

   ## Context
   <the problem/situation forcing a decision, and constraints>

   ## Decision
   <what was decided, stated plainly>

   ## Consequences
   <tradeoffs accepted, including negative ones>
   ```
4. Status starts as `Proposed` unless the user says the decision is already final (`Accepted`).
5. If this ADR touches something covered in `AGENTS.md`, flag that `AGENTS.md` may need a matching update.
