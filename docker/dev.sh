#!/usr/bin/env bash
# Runs backend/frontend build/test inside official images (JDK 21 for the api,
# node:22 for the web), matching CI exactly, so nothing beyond Docker needs to
# be installed locally (no Java/Gradle/Node needed on the host).
# Usage: ./docker/dev.sh <api-test|web-test|web-build|shell>
set -euo pipefail
export MSYS_NO_PATHCONV=1
cd "$(dirname "$0")/.."
ROOT="$(pwd -W 2>/dev/null || pwd)"

JDK_IMAGE="eclipse-temurin:21-jdk"
NODE_IMAGE="node:22"
CMD="${1:-api-test}"
DOCKER_TTY=""
[[ "$CMD" == "shell" ]] && DOCKER_TTY="-it"

run_api() {
  # -v docker.sock: tests use Testcontainers (Postgres/Redis/Kafka), which needs
  # to talk to the host's Docker daemon to spin up its own throwaway containers.
  docker run --rm ${DOCKER_TTY:-} \
    -v "$ROOT/api":/workspace -w /workspace \
    -v spring-mvc-angular-gradle-cache:/root/.gradle \
    -v /var/run/docker.sock:/var/run/docker.sock \
    "$JDK_IMAGE" bash -c "chmod +x gradlew && $1"
}

run_web() {
  docker run --rm ${DOCKER_TTY:-} \
    -v "$ROOT/web":/workspace -w /workspace \
    -v spring-mvc-angular-npm-cache:/root/.npm \
    "$NODE_IMAGE" bash -c "$1"
}

case "$CMD" in
  api-test)  run_api "./gradlew test jacocoTestReport jacocoTestCoverageVerification" ;;
  web-test)  run_web "npm ci && npm run lint && npm run test:coverage" ;;
  web-build) run_web "npm ci && npm run build" ;;
  shell)     run_api "bash" ;;
  *) echo "Usage: $0 {api-test|web-test|web-build|shell}" >&2; exit 1 ;;
esac
