# Project Management Standard

This repo's slice of a shared personal-project standard that also covers
`course_deals_ComposeMultiplatform` (the Kotlin Multiplatform client). Each repo carries
its own tailored copy of this doc set — this one is scoped to what's actually true here
(Spring Boot, Gradle multi-module, Kotlin, Docker-based deployment).

## Where things live

| Layer | Tool | Contains |
|---|---|---|
| Planning | GitHub Projects (board) | Issues, status, priority — the single source of truth for "what's next", shared across this repo and `course_deals_ComposeMultiplatform` |
| Docs | `/docs` folder in this repo | Architecture, business logic, setup guides, design decisions — versioned, diffable |
| Design decisions | `/docs/adr/` | One file per real decision — see [`adr-guide.md`](adr-guide.md) |

---

## Labels

**Type** (every issue gets exactly one)
- `type: feature` · `type: bug` · `type: chore` · `type: docs` · `type: refactor` · `type: test` · `type: design`

**Priority** (every issue gets exactly one)
- `priority: P0` (blocking) · `priority: P1` (this session) · `priority: P2` (backlog) · `priority: P3` (someday)

**Area** (tag with whichever apply — this repo will mostly be `area: backend`, sometimes
`area: infra`, `area: testing`, `area: ci`)
- `area: android` · `area: backend` · `area: web` · `area: design` · `area: testing` · `area: ci` · `area: infra`

**Size — Fibonacci story points, mapped to real hours** (rougher at larger sizes on purpose)

| Points | Rough hours | Meaning |
|---|---|---|
| 1 | ~1 hr | trivial, near-mechanical |
| 2 | ~2 hrs | small, well understood |
| 3 | ~4 hrs (half day) | normal small task |
| 5 | ~1 day | normal task, some unknowns |
| 8 | ~2 days | meaningfully complex |
| 13 | ~3+ days | **split it** — break into smaller linked issues before starting |

Not created on GitHub yet — see [`scripts/gh-setup-labels.sh`](../scripts/gh-setup-labels.sh).

---

## Issue conventions

- **One issue = one reviewable unit of work.** If the title needs "and," it's probably two issues.
- GitHub auto-numbers issues (`#42`) — that number *is* the ticket ID. Cross-repo, reference as `owner/repo#42`.
- Use the templates in `.github/ISSUE_TEMPLATE/` so every issue has the same shape.
- Link issues to the Project board so status is visible in one place.

---

## Quick reference (details in dedicated docs)

| Topic | Quick answer | Full detail |
|---|---|---|
| Branch naming | `<type>/<issue-number>-<slug>` e.g. `feature/42-add-coupon-search-endpoint` | [`branching.md`](branching.md) |
| Commit format | `<type>(<scope>): <description>` e.g. `fix(crawler): correct JobRunr property prefix` | [`commit-conventions.md`](commit-conventions.md) |
| Merge strategy | Squash and merge, default for everything | [`pull-requests.md`](pull-requests.md) |
| Design decisions | Write an ADR in `/docs/adr/` for anything with real tradeoffs | [`adr-guide.md`](adr-guide.md) |
| Versioning | SemVer (`vX.Y.Z`), `CHANGELOG.md` per repo | [`versioning.md`](versioning.md) |
| CI/CD | ktlint + detekt + test + build, Docker images pushed on merge to `main` | [`ci-cd.md`](ci-cd.md) |
| Board columns | `Backlog → Ready → In Progress → In Review → Done` | [`project-board.md`](project-board.md) |

---

## Definition of Done

An issue isn't done when the code works locally. It's done when:
- [ ] PR merged via squash into `main`
- [ ] CI green (ktlint + detekt + build + unit tests)
- [ ] Manually verified locally against Docker Compose (`docker-compose.yml` for
  MySQL/Redis, relevant service run via `./gradlew bootRun`)
- [ ] Docs/ADR updated if the change affects either
- [ ] Issue closed, board reflects it

---

## Setup status

This doc set describes the target process. Implementation status:

- [x] Docs (`README.md` index + `/docs/*`) in place
- [x] Issue/PR templates in place (`.github/`)
- [x] CI & Release CD workflows in place (`.github/workflows/ci-cd.yml`, `.github/workflows/release-cd.yml`)
- [x] Labels created on GitHub — see [`scripts/gh-setup-labels.sh`](../scripts/gh-setup-labels.sh)
- [x] Branch protection on `main` — see [`scripts/gh-setup-branch-protection.sh`](../scripts/gh-setup-branch-protection.sh)
- [x] Shared GitHub Projects board (this repo + `course_deals_ComposeMultiplatform`) — see [`scripts/gh-setup-project-board.sh`](../scripts/gh-setup-project-board.sh)
- [x] Same doc set forked/tailored from `course_deals_ComposeMultiplatform` into this repo
