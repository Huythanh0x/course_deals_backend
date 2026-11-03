#!/usr/bin/env bash
# ==============================================================================
# sync-upstream-skills.sh
# Fetches the latest upstream SKILL.md files from GitHub into a temporary staging
# directory (`references/upstream/.staging/`) so maintainers/agents can diff and
# merge new upstream rules into the 10 Course Deals-aligned skill files in
# `references/upstream/{kmp,apple,backend}/` without overwriting project-specific
# customizations.
#
# Usage:
#   bash scripts/sync-upstream-skills.sh           # Fetch upstream to .staging & show diff summary
#   bash scripts/sync-upstream-skills.sh --apply   # Overwrite with raw upstream (use with care)
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKILL_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
UPSTREAM_DIR="${SKILL_ROOT}/references/upstream"
STAGING_DIR="${UPSTREAM_DIR}/.staging"
APPLY_RAW="${1:-}"

mkdir -p "${STAGING_DIR}/kmp" \
         "${STAGING_DIR}/apple" \
         "${STAGING_DIR}/backend"

echo "🌐 Fetching 10 curated upstream Agent Skills from GitHub..."

fetch_skill() {
  local rel_path="$1"
  local url="$2"
  curl -fsSL "${url}" -o "${STAGING_DIR}/${rel_path}"
  if [[ "${APPLY_RAW}" == "--apply" ]]; then
    cp "${STAGING_DIR}/${rel_path}" "${UPSTREAM_DIR}/${rel_path}"
    echo "  [applied] ${rel_path}"
  else
    echo "  [staged]  ${rel_path} (compare against references/upstream/${rel_path})"
  fi
}

# 1. KMP & Compose Multiplatform (`kmp-engineer` & `desktop-engineer` — 4 files)
fetch_skill "kmp/kmp-app-architecture.md" \
  "https://raw.githubusercontent.com/filippkowalski/kmp-skills-by-filip/main/skills/kmp-app-architecture/SKILL.md"
fetch_skill "kmp/kmp-ktor-networking.md" \
  "https://raw.githubusercontent.com/filippkowalski/kmp-skills-by-filip/main/skills/kmp-ktor-networking/SKILL.md"
fetch_skill "kmp/kmp-testing-strategy.md" \
  "https://raw.githubusercontent.com/filippkowalski/kmp-skills-by-filip/main/skills/kmp-testing-strategy/SKILL.md"
fetch_skill "kmp/kmp-sheets-keyboard.md" \
  "https://raw.githubusercontent.com/filippkowalski/kmp-skills-by-filip/main/skills/kmp-sheets-keyboard/SKILL.md"

# 2. Apple Developer — iOS & watchOS (`apple-developer` — 4 files)
fetch_skill "apple/swiftui-pro.md" \
  "https://raw.githubusercontent.com/twostraws/SwiftUI-Agent-Skill/main/swiftui-pro/SKILL.md"
fetch_skill "apple/kmp-ios-build.md" \
  "https://raw.githubusercontent.com/filippkowalski/kmp-skills-by-filip/main/skills/kmp-ios-build/SKILL.md"
fetch_skill "apple/widgetkit.md" \
  "https://raw.githubusercontent.com/dpearson2699/swift-ios-skills/main/skills/widgetkit/SKILL.md"
fetch_skill "apple/swift-security.md" \
  "https://raw.githubusercontent.com/dpearson2699/swift-ios-skills/main/skills/swift-security/SKILL.md"

# 3. Backend Engineer & Reviewer (`backend-engineer` & `security-arch-reviewer` — 2 files)
fetch_skill "backend/kotlin-backend-jpa-entity-mapping.md" \
  "https://raw.githubusercontent.com/Kotlin/kotlin-agent-skills/main/skills/kotlin-backend-jpa-entity-mapping/SKILL.md"
fetch_skill "backend/spring-kotlin-code-review.md" \
  "https://raw.githubusercontent.com/yalishevant/kotlin-backend-agent-skills/main/.agents/skills/spring-kotlin-code-review/SKILL.md"

rm -rf "${STAGING_DIR}"
echo "✅ Upstream check completed at $(date -u +"%Y-%m-%dT%H:%M:%SZ")."
