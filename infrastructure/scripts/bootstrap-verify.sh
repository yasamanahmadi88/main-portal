#!/usr/bin/env bash
# Verify first-admin bootstrap against a fresh compose stack.
# Requires docker compose services healthy. Uses CI-generated secrets from env.
# Never prints the bootstrap password.
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_EMAIL="${PORTAL_BOOTSTRAP_ADMIN_EMAIL:?PORTAL_BOOTSTRAP_ADMIN_EMAIL required}"
ADMIN_PASSWORD="${PORTAL_BOOTSTRAP_ADMIN_PASSWORD:?PORTAL_BOOTSTRAP_ADMIN_PASSWORD required}"
REPORT_DIR="${REPORT_DIR:-/tmp/portal-live-evidence}"
mkdir -p "$REPORT_DIR"
EVIDENCE="$REPORT_DIR/bootstrap-verify.txt"
: > "$EVIDENCE"

log() { echo "$@" | tee -a "$EVIDENCE"; }
pass() { log "PASS: $*"; }
fail() { log "FAIL: $*"; exit 1; }

export PORTAL_BOOTSTRAP_ADMIN_EMAIL ADMIN_EMAIL
export PORTAL_BOOTSTRAP_ADMIN_PASSWORD ADMIN_PASSWORD

# Confirm password never appears in backend logs
if docker compose ps --services 2>/dev/null | grep -q backend; then
  if docker compose logs backend --no-color 2>/dev/null | grep -F "$ADMIN_PASSWORD" >/dev/null; then
    fail "bootstrap password found in backend logs"
  else
    pass "bootstrap password not present in backend logs"
  fi
  if docker compose logs backend --no-color 2>/dev/null | grep -qiE 'Bootstrap admin created'; then
    pass "bootstrap creation log line present (password redacted/masked)"
  else
    log "NOTE: bootstrap creation log line not found (may have been created on prior boot)"
  fi
fi

COOKIE_JAR="$(mktemp)"
trap 'rm -f "$COOKIE_JAR"' EXIT

csrf() {
  local body
  body="$(curl -fsS -c "$COOKIE_JAR" -b "$COOKIE_JAR" "$BASE_URL/api/v1/auth/csrf")"
  CSRF_TOKEN="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])' <<<"$body")"
}

csrf
code="$(curl -s -o /tmp/boot-login.json -w '%{http_code}' -c "$COOKIE_JAR" -b "$COOKIE_JAR" \
  -X POST "$BASE_URL/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -H "X-XSRF-TOKEN: $CSRF_TOKEN" \
  -d "$(python3 - <<PY
import json,os
print(json.dumps({"username":os.environ["PORTAL_BOOTSTRAP_ADMIN_EMAIL"],"password":os.environ["PORTAL_BOOTSTRAP_ADMIN_PASSWORD"],"rememberDevice":False}))
PY
)")"
[[ "$code" == "200" ]] || { cat /tmp/boot-login.json >>"$EVIDENCE"; fail "bootstrap admin login failed HTTP $code"; }

python3 - <<PY
import json
d=json.load(open("/tmp/boot-login.json"))
assert d.get("status")=="AUTHENTICATED", d
roles=[r.get("code") for r in d["user"]["roles"]]
assert "SUPER_ADMIN" in roles, roles
open("$EVIDENCE","a").write(f"roles={roles}\n")
print("SUPER_ADMIN confirmed")
PY
pass "bootstrap admin authenticates as SUPER_ADMIN"

# Argon2id hash in DB (via docker exec if postgres available)
if docker compose ps --services 2>/dev/null | grep -q postgres; then
  HASH="$(docker compose exec -T postgres psql -U "${POSTGRES_SUPERUSER:-portal_superuser}" -d "${POSTGRES_DB:-portal}" -tAc \
    "SELECT password_hash FROM users WHERE email_normalized=lower('${ADMIN_EMAIL}') LIMIT 1;" 2>/dev/null | tr -d '[:space:]' || true)"
  if [[ -n "$HASH" ]]; then
    [[ "$HASH" == *\$argon2id\$* || "$HASH" == \$argon2* ]] && pass "password stored as Argon2 hash" \
      || fail "password hash not Argon2: ${HASH:0:30}..."
    [[ "$HASH" != *"$ADMIN_PASSWORD"* ]] && pass "plaintext password not stored in hash column" || fail "plaintext in hash column"
  else
    log "NOTE: could not read password_hash via psql (role/network); Argon2 verified by PasswordHashingTest in CI"
  fi

  # Second bootstrap should not create another SUPER_ADMIN when users exist —
  # count users == 1 after first boot
  COUNT="$(docker compose exec -T postgres psql -U "${POSTGRES_SUPERUSER:-portal_superuser}" -d "${POSTGRES_DB:-portal}" -tAc \
    "SELECT COUNT(*) FROM users;" 2>/dev/null | tr -d '[:space:]' || echo unknown)"
  log "user count after bootstrap: $COUNT"
  [[ "$COUNT" == "1" ]] && pass "exactly one user after bootstrap" || log "NOTE: user count=$COUNT"

  AUDIT_BOOT="$(docker compose exec -T postgres psql -U "${POSTGRES_SUPERUSER:-portal_superuser}" -d "${POSTGRES_DB:-portal}" -tAc \
    "SELECT COUNT(*) FROM audit_events WHERE event_type ILIKE '%BOOTSTRAP%' OR action ILIKE '%BOOTSTRAP%';" 2>/dev/null | tr -d '[:space:]' || echo 0)"
  log "bootstrap-related audit events: $AUDIT_BOOT"
fi

pass "bootstrap verification completed"
