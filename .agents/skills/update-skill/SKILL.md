---
name: update-skill
description: >-
  Evolve and update agent skills, workflows, or instructions after ticket completion, learnings,
  or post-mortems, linking back to the originating GitHub issue/PR for auditability.
---

# Update Skill Workflow (`update-skill`)

Use this skill whenever:
1. A defect, missed step, or inefficiency in agent execution is identified during a ticket (e.g. skipping GitHub issue creation, subagent looping, missing security config).
2. The user requests an update or rule addition to an existing skill (such as `team-flow`, `ticket-flow`, `local-be-server`).
3. Retrospectives or learnings need to be codified permanently into project or global skills.

---

## Evolution Protocol

```
1. Identify Defect/Improvement & Root Cause
                   │
2. Trace Back to Originating Issue / PR
                   │
3. Codify Strict Gate / Safeguard in Skill.md
                   │
4. Mirror across Global (~/.gemini/config/skills) & Local (.agents/skills)
                   │
5. Post Changelog Comment on the Originating Issue
```

### Step 1: Identify Defect & Root Cause
- Document what step was skipped or what failure mode occurred (e.g. "Main agent bypassed Phase 2 ticket creation and invoked subagents immediately").
- Pinpoint why it occurred (e.g. ambiguity in prompt transition, absence of a hard blocking prerequisite check).

### Step 2: Trace Back to Originating Issue
- Find the parent or design issue that established the workflow:
  - For `team-flow`: [Huythanh0x/course_deals_ComposeMultiplatform#70](https://github.com/Huythanh0x/course_deals_ComposeMultiplatform/issues/70)
  - For `sprint-flow`: [Huythanh0x/course_deals_ComposeMultiplatform#82](https://github.com/Huythanh0x/course_deals_ComposeMultiplatform/issues/82)
  - Or the currently active ticket issue.

### Step 3: Codify Strict Gate in SKILL.md
- Add an explicit **`[HARD PREREQUISITE GATE]`** or **`[BLOCKING RULE]`**.
- Specify exact validation commands (e.g. `gh issue view <issue-id>` must return 0 before subagents are launched).
- Prevent repetitive subagent loops by prescribing timeouts or max file view iterations.

### Step 4: Mirror Across All Locations
When modifying a skill that exists both globally and within repos, ensure full synchronization:
1. `~/.gemini/config/skills/<skill-name>/SKILL.md`
2. `course_deals_backend/.agents/skills/<skill-name>/SKILL.md` (if present)
3. `course_deals_ComposeMultiplatform/.agents/skills/<skill-name>/SKILL.md` (if present)

### Step 5: Post Evolution Log to GitHub
Document the enhancement on the originating GitHub issue or ticket retro:
```bash
gh issue comment <origin-issue-id> -R Huythanh0x/course_deals_ComposeMultiplatform \
  --body "### 🔄 Skill Evolution Update: \`<skill-name>\`
- **Trigger**: [Issue #<current-issue-id>]
- **Defect/Gap**: <what went wrong or was improved>
- **Fix Codified**: <what hard gate was added to SKILL.md>"
```
