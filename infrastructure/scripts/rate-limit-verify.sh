#!/usr/bin/env bash
# Login rate-limit burst against a running portal stack.
# Must run AFTER other authenticated verification steps — the nginx login
# zone is 10r/m and this intentionally exhausts it.
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
REPORT_DIR="${REPORT_DIR:-/tmp/portal-live-evidence}"
mkdir -p "$REPORT_DIR"
EVIDENCE="$REPORT_DIR/rate-limit-verify.txt"
: > "$EVIDENCE"

log() { echo "$@" | tee -a "$EVIDENCE"; }
pass() { log "PASS: $*"; }
fail() { log "FAIL: $*"; exit 1; }

log "=== Login rate-limit verification against ${BASE_URL} ==="

RATE_JAR="$(mktemp)"
trap 'rm -f "$RATE_JAR"' EXIT
saw_429=0
codes=()
for i in $(seq 1 30); do
  body="$(curl -fsS -c "$RATE_JAR" -b "$RATE_JAR" "$BASE_URL/api/v1/auth/csrf" 2>/dev/null || true)"
  tok="$(python3 -c 'import json,sys
try:
    print(json.load(sys.stdin)["token"])
except Exception:
    print("")' <<<"$body")"
  code="$(curl -s -o /dev/null -w '%{http_code}' -c "$RATE_JAR" -b "$RATE_JAR" \
    -X POST "$BASE_URL/api/v1/auth/login" \
    -H 'Content-Type: application/json' \
    -H "X-XSRF-TOKEN: ${tok}" \
    -d '{"username":"rate-limit-probe@portal.local","password":"WrongPassword!12345","rememberDevice":false}')"
  codes+=("$code")
  if [[ "$code" == "429" ]]; then
    saw_429=1
    break
  fi
done
echo "rate_limit_codes=${codes[*]}" | tee -a "$EVIDENCE"
[[ "$saw_429" == "1" ]] && pass "login rate limiting returns HTTP 429 under burst" \
  || fail "expected HTTP 429 from login rate limit within 30 attempts (got: ${codes[*]})"

log "=== RATE LIMIT CHECKS PASSED ==="
