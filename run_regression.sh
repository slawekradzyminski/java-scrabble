#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
FRONTEND_DIR="$ROOT_DIR/apps/frontend"

GREEN="\033[0;32m"
YELLOW="\033[0;33m"
RED="\033[0;31m"
BLUE="\033[0;34m"
BOLD="\033[1m"
RESET="\033[0m"

section() {
  echo ""
  echo -e "${BOLD}${BLUE}==> $1${RESET}"
}

success() {
  echo -e "${GREEN}✔${RESET} $1"
}

failure() {
  echo -e "${RED}✖${RESET} $1"
}

start_ts=$(date +%s)

section "Backend: tests + checks"
if "$ROOT_DIR/gradlew" test check; then
  success "Backend tests and checks passed"
else
  failure "Backend tests or checks failed"
  exit 1
fi

section "Backend: coverage report"
if "$ROOT_DIR/gradlew" jacocoTestReport; then
  success "JaCoCo coverage reports generated"
else
  failure "JaCoCo coverage report failed"
  exit 1
fi

section "Frontend: lint"
if (cd "$FRONTEND_DIR" && npm run lint); then
  success "Frontend lint passed"
else
  failure "Frontend lint failed"
  exit 1
fi

section "Frontend: tests"
if (cd "$FRONTEND_DIR" && npm run test); then
  success "Frontend tests passed"
else
  failure "Frontend tests failed"
  exit 1
fi

section "Frontend: build"
if (cd "$FRONTEND_DIR" && npm run build); then
  success "Frontend build passed"
else
  failure "Frontend build failed"
  exit 1
fi

end_ts=$(date +%s)
elapsed=$((end_ts - start_ts))

echo ""
echo -e "${BOLD}${GREEN}All regression checks passed in ${elapsed}s.${RESET}"
