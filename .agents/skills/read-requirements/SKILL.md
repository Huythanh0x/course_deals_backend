---
name: read-requirements
description: Read a GitHub issue's requirements and labels and record them verbatim into that ticket's workspace file. First step of implementing an existing issue — use standalone to (re)load an issue into its workspace, or as part of the ticket-flow skill.
---

# Read requirements

1. Locate `.agents/tickets/<issue-number>-<slug>/NOTES.md`. If it doesn't exist yet,
   create it from the template in the `ticket-flow` skill.
2. `gh issue view <N> -R Huythanh0x/course_deals_backend` — copy the
   **verbatim** body and labels into the workspace's Requirements section. Don't
   paraphrase away detail; later steps need the exact wording, not a summary that might
   drop an edge case the issue actually specified.
3. Note the labels explicitly (type/priority/area/size) — `size: 13` is a signal the
   issue should have been split before starting; flag that rather than proceeding
   silently.
4. Append one line to Progress log: `<date> read requirements for #<N>`.

## Output

Report back the requirements as read (title, body, labels) so whoever's driving this
step — the user directly, or `ticket-flow`'s next step — has them without re-fetching.
