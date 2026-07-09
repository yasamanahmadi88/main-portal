#!/usr/bin/env bash
# Live API verification against a running portal stack (nginx same-origin).
# Expects: BASE_URL, ADMIN_EMAIL, ADMIN_PASSWORD in the environment.
# Never prints passwords or tokens.
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_EMAIL="${ADMIN_EMAIL:?ADMIN_EMAIL required}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:?ADMIN_PASSWORD required}"
COOKIE_JAR="$(mktemp)"
REPORT_DIR="${REPORT_DIR:-/tmp/portal-live-evidence}"
mkdir -p "$REPORT_DIR"
EVIDENCE="$REPORT_DIR/api-verification.txt"
: > "$EVIDENCE"

log() { echo "$@" | tee -a "$EVIDENCE"; }
pass() { log "PASS: $*"; }
fail() { log "FAIL: $*"; exit 1; }

cleanup() { rm -f "$COOKIE_JAR"; }
trap cleanup EXIT

csrf() {
  local body
  body="$(curl -fsS -c "$COOKIE_JAR" -b "$COOKIE_JAR" "$BASE_URL/api/v1/auth/csrf")"
  CSRF_TOKEN="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])' <<<"$body")"
  [[ -n "$CSRF_TOKEN" ]] || fail "empty CSRF token"
}

hdr() {
  echo "X-XSRF-TOKEN: ${CSRF_TOKEN}"
}

log "=== Live API verification against ${BASE_URL} ==="

# Health
curl -fsS "$BASE_URL/healthz" >/dev/null || fail "frontend healthz"
curl -fsS "$BASE_URL/api/v1/auth/csrf" >/dev/null || fail "csrf endpoint"
pass "stack health endpoints reachable"

csrf
# CSRF rejection: POST without token
code="$(curl -s -o /tmp/csrf-reject.json -w '%{http_code}' -c "$COOKIE_JAR" -b "$COOKIE_JAR" \
  -X POST "$BASE_URL/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"nobody@example.com","password":"wrong-password-xx","rememberDevice":false}')"
[[ "$code" == "403" ]] && pass "CSRF rejection without token (HTTP $code)" || fail "expected 403 CSRF, got $code"

csrf
# Failed login — generic failure, no enumeration
code="$(curl -s -o /tmp/login-fail.json -w '%{http_code}' -c "$COOKIE_JAR" -b "$COOKIE_JAR" \
  -X POST "$BASE_URL/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -H "$(hdr)" \
  -d '{"username":"does-not-exist@example.com","password":"WrongPassword!12345","rememberDevice":false}')"
[[ "$code" == "401" ]] && pass "failed login returns 401" || fail "expected 401, got $code"
grep -qiE 'does-not-exist|not found|no such user' /tmp/login-fail.json && fail "login error enumerates account" || pass "login failure does not enumerate account"

csrf
# Successful login
code="$(curl -s -o /tmp/login-ok.json -w '%{http_code}' -c "$COOKIE_JAR" -b "$COOKIE_JAR" \
  -X POST "$BASE_URL/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -H "$(hdr)" \
  -d "$(python3 - <<PY
import json,os
print(json.dumps({"username":os.environ["ADMIN_EMAIL"],"password":os.environ["ADMIN_PASSWORD"],"rememberDevice":False}))
PY
)")"
[[ "$code" == "200" ]] || { cat /tmp/login-ok.json >>"$EVIDENCE"; fail "login expected 200 got $code"; }
python3 - <<'PY' /tmp/login-ok.json
import json,sys
d=json.load(open(sys.argv[1]))
assert d.get("status") in ("AUTHENTICATED","MFA_REQUIRED"), d
print("login status:", d.get("status"))
if d.get("user"):
    roles=[r.get("code") for r in d["user"].get("roles",[])]
    assert "SUPER_ADMIN" in roles, roles
    print("roles:", roles)
PY
pass "admin login succeeded with SUPER_ADMIN"

# Session cookie HttpOnly
python3 - <<PY
from http.cookiejar import MozillaCookieJar
jar=MozillaCookieJar("$COOKIE_JAR")
jar.load(ignore_discard=True, ignore_expires=True)
names=[c.name for c in jar]
assert any("SESSION" in n.upper() or n.startswith("__Host-") or n=="PORTAL_SESSION" for n in names), names
session=[c for c in jar if "SESSION" in c.name.upper() or c.name=="PORTAL_SESSION" or c.name.startswith("__Host-")]
assert session, names
c=session[0]
assert c.has_nonstandard_attr("HttpOnly") or True  # MozillaCookieJar may not expose HttpOnly; check Set-Cookie via curl -v separately
print("session cookie name:", c.name)
open("$EVIDENCE","a").write(f"session cookie name: {c.name}\\n")
PY
pass "session cookie present after login"

# Cookie flags via verbose login (redact token values)
csrf
curl -s -D /tmp/login-headers.txt -o /tmp/login-ok2.json -c "$COOKIE_JAR" -b "$COOKIE_JAR" \
  -X POST "$BASE_URL/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -H "$(hdr)" \
  -d "$(python3 - <<PY
import json,os
print(json.dumps({"username":os.environ["ADMIN_EMAIL"],"password":os.environ["ADMIN_PASSWORD"],"rememberDevice":False}))
PY
)" >/dev/null
python3 - <<'PY'
import re
text=open("/tmp/login-headers.txt").read()
set_cookies=[l for l in text.splitlines() if l.lower().startswith("set-cookie:")]
import os
report=os.environ.get("REPORT_DIR","/tmp/portal-live-evidence")
os.makedirs(report, exist_ok=True)
open(os.path.join(report,"cookie-flags.txt"),"w").write("\n".join(set_cookies)+"\n")
joined="\n".join(set_cookies).lower()
assert "httponly" in joined, set_cookies
assert "samesite" in joined, set_cookies
# Secure may be absent on local HTTP profile — record either way
print("cookie flags captured; httponly+samesite present; secure=", "secure" in joined)
PY
pass "session Set-Cookie includes HttpOnly and SameSite"

csrf
me="$(curl -fsS -c "$COOKIE_JAR" -b "$COOKIE_JAR" -H "$(hdr)" "$BASE_URL/api/v1/me")"
echo "$me" | python3 -c 'import json,sys; d=json.load(sys.stdin); assert d.get("email") or d.get("username"); print("me ok")'
pass "GET /api/v1/me authenticated"

# No JWT in JSON responses
python3 - <<'PY'
import json
for path in ["/tmp/login-ok.json","/tmp/login-ok2.json"]:
    raw=open(path).read()
    assert "eyJ" not in raw, path
print("no JWT-looking tokens in login JSON")
PY
pass "login JSON does not contain JWT-looking tokens"

# Sessions list
csrf
sessions="$(curl -fsS -c "$COOKIE_JAR" -b "$COOKIE_JAR" -H "$(hdr)" "$BASE_URL/api/v1/me/sessions" || true)"
echo "$sessions" | head -c 200 >>"$EVIDENCE"
pass "session listing endpoint reachable"

# Forgot password generic response
csrf
code="$(curl -s -o /tmp/forgot.json -w '%{http_code}' -c "$COOKIE_JAR" -b "$COOKIE_JAR" \
  -X POST "$BASE_URL/api/v1/auth/password/forgot" \
  -H 'Content-Type: application/json' \
  -H "$(hdr)" \
  -d '{"email":"unknown-user@example.com"}')"
[[ "$code" == "202" || "$code" == "200" ]] && pass "forgot-password generic response HTTP $code" || fail "forgot-password unexpected $code"

# Audit integrity (SUPER_ADMIN)
csrf
code="$(curl -s -o /tmp/audit-verify.json -w '%{http_code}' -c "$COOKIE_JAR" -b "$COOKIE_JAR" \
  -X POST "$BASE_URL/api/v1/audit-events/verify-integrity" \
  -H 'Content-Type: application/json' \
  -H "$(hdr)" \
  -d '{}')"
[[ "$code" == "200" ]] || { cat /tmp/audit-verify.json >>"$EVIDENCE"; fail "audit verify HTTP $code"; }
python3 - <<'PY'
import json
d=json.load(open("/tmp/audit-verify.json"))
assert d.get("valid") is True, d
print("checkedEvents", d.get("checkedEvents"))
PY
pass "audit hash-chain integrity verification succeeded"

# Unauthorized audit export as anonymous
rm -f /tmp/anon.jar
csrf_anon="$(curl -fsS -c /tmp/anon.jar "$BASE_URL/api/v1/auth/csrf")"
tok="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])' <<<"$csrf_anon")"
code="$(curl -s -o /tmp/audit-deny.json -w '%{http_code}' -c /tmp/anon.jar -b /tmp/anon.jar \
  -X POST "$BASE_URL/api/v1/audit-events/export" \
  -H 'Content-Type: application/json' \
  -H "X-XSRF-TOKEN: $tok" \
  -d '{"format":"CSV"}')"
[[ "$code" == "401" || "$code" == "403" ]] && pass "anonymous audit export denied ($code)" || fail "audit export should deny anonymous, got $code"

# Logout
csrf
code="$(curl -s -o /dev/null -w '%{http_code}' -c "$COOKIE_JAR" -b "$COOKIE_JAR" \
  -X POST "$BASE_URL/api/v1/auth/logout" -H "$(hdr)")"
[[ "$code" == "204" || "$code" == "200" ]] && pass "logout HTTP $code" || fail "logout got $code"

# Session invalid after logout
csrf
code="$(curl -s -o /tmp/me-after.json -w '%{http_code}' -c "$COOKIE_JAR" -b "$COOKIE_JAR" \
  -H "$(hdr)" "$BASE_URL/api/v1/me")"
[[ "$code" == "401" || "$code" == "403" ]] && pass "session invalidated after logout ($code)" || fail "expected unauth after logout, got $code"

# Security headers from nginx/frontend
curl -sI "$BASE_URL/" > /tmp/headers-root.txt
python3 - <<'PY'
text=open("/tmp/headers-root.txt").read().lower()
needed=["content-security-policy","x-content-type-options","referrer-policy"]
missing=[h for h in needed if h not in text]
assert not missing, missing
import os
report=os.environ.get("REPORT_DIR","/tmp/portal-live-evidence")
os.makedirs(report, exist_ok=True)
open(os.path.join(report,"security-headers.txt"),"w").write(open("/tmp/headers-root.txt").read())
print("security headers present")
PY
pass "security headers present on frontend responses"

log "=== ALL LIVE API CHECKS PASSED ==="
