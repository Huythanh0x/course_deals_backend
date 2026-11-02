#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# CourseDeals Backend Release Build Script
# Targets: JAR (Spring Boot executable), Image (Docker OCI tarball / registry)
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
OUTPUT_DIR="${REPO_ROOT}/build/release-artifacts"

VERSION=""
TARGET="all" # all, jar, image
DOCKER_REGISTRY="" # e.g. docker.io/username or ghcr.io/username
PUSH_DOCKER=false

print_usage() {
  cat <<EOF
Usage: $(basename "$0") [OPTIONS]

Options:
  -v, --version <ver>      Release version tag (e.g. v1.0.0 or 1.0.0) [Required]
  -t, --target <target>    Target artifact: all, jar, image [Default: all]
  -r, --registry <reg>     Docker registry prefix (e.g. thanh0x)
  -p, --push               Push Docker images to registry (default: false, saves tarball)
  -o, --output <dir>       Artifact output directory [Default: build/release-artifacts]
  -h, --help               Show this help message

Examples:
  $(basename "$0") --version v1.0.0
  $(basename "$0") --version v1.0.0 --target jar
  $(basename "$0") --version v1.0.0 --target image
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -v|--version)
      VERSION="$2"
      shift 2
      ;;
    -t|--target)
      TARGET="$2"
      shift 2
      ;;
    -r|--registry)
      DOCKER_REGISTRY="$2"
      shift 2
      ;;
    -p|--push)
      PUSH_DOCKER=true
      shift 1
      ;;
    -o|--output)
      OUTPUT_DIR="$2"
      shift 2
      ;;
    -h|--help)
      print_usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      print_usage
      exit 1
      ;;
  esac
done

if [[ -z "${VERSION}" ]]; then
  echo "Error: --version is required (e.g. --version v1.0.0)" >&2
  exit 1
fi

CLEAN_VERSION="${VERSION#v}"
mkdir -p "${OUTPUT_DIR}"

echo "=========================================================="
echo " Building CourseDeals Backend Release"
echo " Version:  ${VERSION} (clean: ${CLEAN_VERSION})"
echo " Target:   ${TARGET}"
echo " Output:   ${OUTPUT_DIR}"
echo "=========================================================="

cd "${REPO_ROOT}"

# ------------------------------------------------------------------------------
# 1. Spring Boot Executable JARs
# ------------------------------------------------------------------------------
build_jars() {
  echo ""
  echo "==> [1/2] Building Spring Boot Executable JARs..."
  ./gradlew :modules:coupon-api-service:bootJar :modules:coupon-crawler-service:bootJar -x test

  local api_jar="${REPO_ROOT}/modules/coupon-api-service/build/libs"
  local crawler_jar="${REPO_ROOT}/modules/coupon-crawler-service/build/libs"

  local api_file
  api_file=$(find "${api_jar}" -maxdepth 1 -name "*.jar" ! -name "*-plain.jar" | head -n 1)
  local crawler_file
  crawler_file=$(find "${crawler_jar}" -maxdepth 1 -name "*.jar" ! -name "*-plain.jar" | head -n 1)

  if [[ -f "${api_file}" ]]; then
    local target_api="${OUTPUT_DIR}/coupon-api-service-${CLEAN_VERSION}.jar"
    cp -v "${api_file}" "${target_api}"
    echo "✔ API Service JAR: ${target_api}"
  else
    echo "❌ API Service JAR not found in ${api_jar}" >&2
    return 1
  fi

  if [[ -f "${crawler_file}" ]]; then
    local target_crawler="${OUTPUT_DIR}/coupon-crawler-service-${CLEAN_VERSION}.jar"
    cp -v "${crawler_file}" "${target_crawler}"
    echo "✔ Crawler Service JAR: ${target_crawler}"
  else
    echo "❌ Crawler Service JAR not found in ${crawler_jar}" >&2
    return 1
  fi
}

# ------------------------------------------------------------------------------
# 2. Docker Images (OCI Tarball or Registry Push)
# ------------------------------------------------------------------------------
build_images() {
  echo ""
  echo "==> [2/2] Building Docker Images..."
  if ! command -v docker >/dev/null 2>&1; then
    echo "⚠️ Docker not found. Skipping Docker image build."
    return 0
  fi

  local services=("coupon-api-service" "coupon-crawler-service")
  for svc in "${services[@]}"; do
    local tag_name="${svc}:${CLEAN_VERSION}"
    if [[ -n "${DOCKER_REGISTRY}" ]]; then
      tag_name="${DOCKER_REGISTRY}/${svc}:${CLEAN_VERSION}"
    fi

    echo "Building Docker image: ${tag_name}..."
    docker build \
      --build-arg SERVICE_NAME="${svc}" \
      -t "${tag_name}" \
      -t "${svc}:latest" \
      -f "${REPO_ROOT}/Dockerfile" \
      "${REPO_ROOT}"

    if [[ "${PUSH_DOCKER}" == "true" ]]; then
      echo "Pushing image to registry: ${tag_name}..."
      docker push "${tag_name}"
      echo "✔ Pushed: ${tag_name}"
    else
      local tar_path="${OUTPUT_DIR}/${svc}-${CLEAN_VERSION}.docker.tar.gz"
      echo "Exporting image tarball to: ${tar_path}..."
      docker save "${tag_name}" | gzip > "${tar_path}"
      echo "✔ Exported Docker image tarball: ${tar_path}"
    fi
  done
}

case "${TARGET}" in
  all)
    build_jars
    build_images
    ;;
  jar)
    build_jars
    ;;
  image)
    build_images
    ;;
  *)
    echo "Unknown target: ${TARGET}" >&2
    exit 1
    ;;
esac

echo ""
echo "==> Generating SHA256 checksums..."
(
  cd "${OUTPUT_DIR}"
  if ls ./* 2>/dev/null | grep -v "SHA256SUMS.txt" >/dev/null; then
    shasum -a 256 ./* 2>/dev/null | grep -v "SHA256SUMS.txt" > SHA256SUMS.txt || true
    echo "✔ SHA256SUMS.txt generated:"
    cat SHA256SUMS.txt
  fi
)

echo ""
echo "=========================================================="
echo " Finished Backend Release Artifacts:"
ls -lh "${OUTPUT_DIR}"
echo "=========================================================="
