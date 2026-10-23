---
name: spec-research
description: >-
  Interactive pre-planning and discovery workflow. Takes a raw feature requirement,
  clarifies ambiguities via structured interactive questions, conducts competitive
  and industry best-practice research, and generates a concrete Architecture & Spec RFC.
---

# Feature Discovery & Spec Research (`spec-research`)

Use this skill as a **pre-planning phase** before creating tickets or writing code. It transforms an initial, rough user idea or single requirement into a thoroughly researched, battle-tested specification.

---

## The 4-Stage Discovery Pipeline

```
Stage 1: Requirement Intake & Ambiguity Triage
                      │
Stage 2: Interactive Clarification (ask_question)
                      │
Stage 3: Market & Competitive Research (search_web)
                      │
Stage 4: Architecture & Spec RFC Synthesis (Artifact)
```

---

## Stage 1: Requirement Intake & Ambiguity Triage

1. **Ingest the Raw Idea**:
   Extract the core user requirement (e.g., "Add course bookmarking", "Offline audio caching", "Referral program").
2. **Identify Ambiguities across 5 Pillars**:
   - **User Experience (UX)**: Empty states, loading indicators, error banners, offline behavior.
   - **Data & State**: Local-first caching vs server-only, sync frequency, pagination/sorting, data retention.
   - **Security & Authorization**: Public vs guest vs authenticated access, rate-limiting, privacy.
   - **System Integration**: Backend REST/GraphQL endpoints, database schemas, third-party APIs.
   - **Edge Cases & Failure Modes**: Network loss, expired tokens, duplicate submissions, schema rollbacks.

---

## Stage 2: Interactive Clarification (`ask_question`)

Never make assumptions on major architectural or product decisions. Use `ask_question` to resolve critical choices with the user:

- Formulate 1 to 3 targeted, concise multiple-choice questions.
- Include a `(Recommended)` option based on project conventions and Clean Architecture standards.
- Format options as direct user responses.
- Examples of questions:
  - *Data model preference* (e.g., Optimistic UI with Room cache vs strict network-first)
  - *Guest/Auth policy* (e.g., Allow guest bookmarking stored locally, or force login modal)
  - *Scope boundaries* (e.g., MVP only vs full multi-device sync)

---

## Stage 3: Competitive & Best-Practice Research (`search_web`)

Conduct external research to discover how market leaders and standard platforms solve this exact feature:

1. **Competitor Teardown**:
   - Search how tier-1 applications (Udemy, Coursera, Duolingo, Medium, Spotify, etc.) handle the flow.
   - Investigate UX teardowns, UI patterns, and user expectations.
2. **Industry Best Practices & RFCs**:
   - Material Design 3 guidelines for the relevant components (e.g., bottom sheets, chips, action badges).
   - Android Architecture Patterns (Offline-first, unidirectional data flow, StateFlow).
   - Spring Boot / MySQL / Redis design patterns (idempotency keys, job queues, indexing).

*Search Query Examples:*
- `"how does Udemy handle course report reason flow mobile"`
- `"best practices offline bookmark synchronization android room"`
- `"material 3 bottom sheet filter chips guidelines"`

---

## Stage 4: Architecture & Spec RFC Synthesis

Produce a comprehensive **Specification & Architecture RFC** artifact in `<appDataDir>/brain/<conversation-id>/SPEC_RFC_<feature-slug>.md` covering:

1. **Executive Summary & Goals**: Problem statement, target user, non-goals (what we are NOT doing).
2. **Competitive Insights**: Key learnings from competitors and why our design choices won or differed.
3. **User Flow & UX Wireframes**: Screen states (Empty, Loading, Error, Content, Action).
4. **API & Data Contract**:
   - Endpoints, HTTP methods, status codes.
   - Request and response JSON schemas with nullability clearly specified.
5. **Database Schema & Migrations**: MySQL table DDL, indexes, and entity relationships.
6. **Mobile Clean Architecture Mapping**:
   - Domain UseCases, Repository interfaces, DataSources, Room entities, and ViewModel state.
7. **Concrete Definition of Done (DoD) Matrix**:
   - Verifiable checklist of test cases (Unit, Lint, Detekt, Live E2E).

---

## Handoff to Implementation

Once the Spec RFC is approved by the user, it directly feeds into:
- [`team-flow`](~/.gemini/config/skills/team-flow/SKILL.md): Automatically satisfies Phase 1 (PM & Architect Discovery) and Checkpoint 1, allowing instant ticket creation and multi-agent dispatch!
