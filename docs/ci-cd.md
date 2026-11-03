# CI/CD & Deployment

## Environments

Start minimal: **local** (`docker-compose.local.yml` for MySQL/Redis, services run via
`./gradlew bootRun` against the `local` Spring profile) → **production** (`docker
compose -f docker-compose.prod.yml up`, pulling images built by CI, see the README's
Quick Start). Don't add a `staging` environment preemptively — it's real ongoing overhead
that only pays for itself once there's a concrete reason to need a pre-prod check step.

## CI pipeline (runs on every PR and push to `main`, see `.github/workflows/ci-cd.yml`)

Single combined job, in order, all required to pass:

1. **Lint** — `./gradlew ktlintCheck detekt`. Pre-existing violations are snapshotted per
   module in `ktlint-baseline.xml`/`detekt-baseline.xml`, so only newly introduced issues
   fail the build.
2. **Unit tests** — `./gradlew clean test`, across every module.
3. **Build & push Docker images** — assembles and pushes both deployables
   (`coupon-api-service`, `coupon-crawler-service`) as separate image tags from the same
   Docker Hub repository (see `versioning.md`'s Docker image tagging section). On a PR,
   images are built (to confirm the `Dockerfile` still produces a valid image) but not
   pushed — `push: ${{ github.event_name != 'pull_request' }}`.

Configure this job as a **required status check** in the repo's branch protection
settings for `main` — this makes the gate structural, not a habit you can forget under
time pressure. See [`scripts/gh-setup-branch-protection.sh`](../scripts/gh-setup-branch-protection.sh).

## CD pipeline (runs on tagging a release `v*.*.*` or manual dispatch, see `.github/workflows/release-cd.yml`)

- **Automated Release Packaging (`.github/workflows/release-cd.yml` & `scripts/release-build.sh`)**:
  - Triggered on pushing a `vX.Y.Z` tag or via `workflow_dispatch`.
  - Builds executable Spring Boot JARs (`coupon-api-service-<ver>.jar`, `coupon-crawler-service-<ver>.jar`).
  - Builds and pushes multi-arch (`linux/amd64`, `linux/arm64`) Docker images (`api-v<ver>`, `crawler-v<ver>`, and `-latest` for stable releases) to Docker Hub when `DOCKER_HUB_USERNAME`/`DOCKER_HUB_TOKEN` secrets are configured.
  - Computes `SHA256SUMS.txt` and publishes the runnable JARs and checksums directly to **GitHub Releases**.
  - Can also be run locally via `bash scripts/release-build.sh --version 1.0.0 --service all`.
- **Production VPS Auto-Deploy**:
  - Actual deployment to the production VPS is a **separate, deliberate pull**, not push-driven from CI: [`scripts/autodeploy.sh`](../scripts/autodeploy.sh) runs on the server (e.g. via cron), does `docker compose -f docker-compose.prod.yml pull`, and only if a newer image was actually downloaded does it restart the stack (`docker compose ... up -d`) and prune old images.

## Secrets

Store `DOCKER_HUB_USERNAME`/`DOCKER_HUB_TOKEN` and any other API keys as GitHub Actions
**encrypted secrets** (repo or environment-level), never committed to the repo, never
hardcoded in a workflow file even "temporarily." Rotate a secret immediately if it's ever
accidentally exposed in a log or committed file — don't assume a quick `git revert` is
sufficient, since the value already existed in the exposed commit's history.

## Rollback plan

Decide this *before* you need it, not while something's actively broken:

- If a bad `api-latest`/`crawler-latest` image reaches production via `autodeploy.sh`,
  the fastest rollback is re-tagging/re-pushing the last known-good image as `-latest`
  (or pointing `docker-compose.prod.yml` at a specific known-good `-vX.Y.Z` tag
  temporarily) and letting the next poll pick it up — don't try to patch forward under
  pressure.
- Since `docker-compose.prod.yml` pins images by tag, keeping the previous `-vX.Y.Z`
  image around (don't prune too aggressively) is what makes a fast rollback possible at
  all.

## Release checklist

- [ ] All linked issues for this version closed
- [ ] `CHANGELOG.md` updated
- [ ] Git tag `vX.Y.Z` pushed on `main`
- [ ] CI green on `main`
- [ ] Smoke-tested locally against `docker-compose.local.yml` (or the pulled `-vX.Y.Z`
  image against `docker-compose.prod.yml` config) before considering it released
- [ ] ADRs written for any real decisions made this cycle
