#!/usr/bin/env bash
# Bootstrap local/cloud toolchain for main-portal foundation.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "==> Main Portal setup"
echo "    repo root: $ROOT_DIR"

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "ERROR: required command not found: $1" >&2
    return 1
  fi
}

echo "==> Checking Java"
if command -v java >/dev/null 2>&1; then
  java -version
else
  echo "ERROR: Java is missing. Install OpenJDK 21+." >&2
  exit 1
fi

JAVA_MAJOR="$(java -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1)"
if [[ -z "${JAVA_MAJOR}" || "${JAVA_MAJOR}" -lt 21 ]]; then
  echo "ERROR: Java 21+ required (found major=${JAVA_MAJOR:-unknown}). Java 25 is not required for this phase." >&2
  exit 1
fi

echo "==> Checking Maven"
if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven missing — attempting apt install (requires sudo)..."
  if command -v apt-get >/dev/null 2>&1; then
    sudo apt-get update -qq
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq maven
  else
    echo "ERROR: cannot auto-install Maven on this OS. Install Maven 3.9+ manually." >&2
    exit 1
  fi
fi
mvn -v | head -3

echo "==> Checking Node.js / npm"
need_cmd node
need_cmd npm
node -v
npm -v

if [[ ! -f .env && -f .env.example ]]; then
  cp .env.example .env
  echo "==> Created .env from .env.example (edit locally; do not commit)"
fi

echo "==> Installing frontend dependencies"
(cd frontend && npm install)

echo "==> Setup complete"
echo "    Next: ./scripts/verify.sh"
