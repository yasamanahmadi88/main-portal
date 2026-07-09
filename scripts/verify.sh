#!/usr/bin/env bash
# Verify foundation builds and tests for main-portal.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "==> Backend: mvn test"
(cd backend && mvn -B -q test)

echo "==> Frontend: lint (tsc)"
(cd frontend && npm run lint)

echo "==> Frontend: unit tests"
(cd frontend && npm test)

echo "==> Frontend: production build"
(cd frontend && npm run build)

echo "==> Backend: package"
(cd backend && mvn -B -q -DskipTests package)

echo "==> Verification passed"
