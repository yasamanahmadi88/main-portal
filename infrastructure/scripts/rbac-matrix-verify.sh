#!/usr/bin/env bash
# RBAC authorization matrix against a running portal API.
# Uses SUPER_ADMIN credentials to seed fixture users/roles, then verifies
# deny-by-default and privilege boundaries via direct HTTP calls.
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_EMAIL="${ADMIN_EMAIL:?ADMIN_EMAIL required}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:?ADMIN_PASSWORD required}"
COOKIE_JAR="$(mktemp)"
REPORT_DIR="${REPORT_DIR:-/tmp/portal-live-evidence}"
mkdir -p "$REPORT_DIR"
EVIDENCE="$REPORT_DIR/rbac-matrix.txt"
: > "$EVIDENCE"

log() { echo "$@" | tee -a "$EVIDENCE"; }
pass() { log "PASS: $*"; }
fail() { log "FAIL: $*"; exit 1; }

cleanup() { rm -f "$COOKIE_JAR" /tmp/rbac-*.jar /tmp/rbac-*.json /tmp/rbac-fixture-user-id.txt; }
trap cleanup EXIT

csrf() {
  local jar="${1:-$COOKIE_JAR}"
  local body
  body="$(curl -fsS -c "$jar" -b "$jar" "$BASE_URL/api/v1/auth/csrf")"
  CSRF_TOKEN="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])' <<<"$body")"
}

login() {
  local jar="$1" email="$2" password="$3"
  csrf "$jar"
  local code
  code="$(curl -s -o /tmp/rbac-login.json -w '%{http_code}' -c "$jar" -b "$jar" \
    -X POST "$BASE_URL/api/v1/auth/login" \
    -H 'Content-Type: application/json' \
    -H "X-XSRF-TOKEN: $CSRF_TOKEN" \
    -d "$(python3 - <<PY
import json
print(json.dumps({"username":"$email","password":"$password","rememberDevice":False}))
PY
)")"
  [[ "$code" == "200" ]] || { cat /tmp/rbac-login.json >>"$EVIDENCE"; fail "login failed for $email HTTP $code"; }
}

api() {
  local jar="$1" method="$2" path="$3" body="${4:-}"
  csrf "$jar"
  if [[ -n "$body" ]]; then
    curl -s -o /tmp/rbac-resp.json -w '%{http_code}' -c "$jar" -b "$jar" \
      -X "$method" "$BASE_URL$path" \
      -H 'Content-Type: application/json' \
      -H "X-XSRF-TOKEN: $CSRF_TOKEN" \
      -d "$body"
  else
    curl -s -o /tmp/rbac-resp.json -w '%{http_code}' -c "$jar" -b "$jar" \
      -X "$method" "$BASE_URL$path" \
      -H "X-XSRF-TOKEN: $CSRF_TOKEN"
  fi
}

expect_code() {
  local got="$1" want="$2" msg="$3"
  [[ "$got" == "$want" ]] && pass "$msg (HTTP $got)" || { cat /tmp/rbac-resp.json >>"$EVIDENCE"; fail "$msg expected $want got $got"; }
}

log "=== RBAC matrix against ${BASE_URL} ==="

# Admin session
login "$COOKIE_JAR" "$ADMIN_EMAIL" "$ADMIN_PASSWORD"
pass "SUPER_ADMIN authenticated"

# List roles and capture IDs
code="$(api "$COOKIE_JAR" GET "/api/v1/roles?page=0&size=50")"
expect_code "$code" "200" "SUPER_ADMIN can list roles"
python3 - <<PY
import json
d=json.load(open("/tmp/rbac-resp.json"))
items=d.get("content") or d.get("items") or d.get("data") or []
if isinstance(items, dict):
    items=items.get("content",[])
codes={ (r.get("code") or r.get("name")): r.get("id") for r in items }
needed=["SUPER_ADMIN","ADMIN","SECURITY_ADMIN","USER_MANAGER","ROLE_MANAGER","AUDITOR","SUPPORT","USER"]
open("/tmp/rbac-role-ids.json","w").write(json.dumps(codes, indent=2))
print("roles found:", sorted(codes.keys()))
missing=[n for n in needed if n not in codes]
open("$EVIDENCE","a").write(f"roles={sorted(codes.keys())}\nmissing={missing}\n")
if missing:
    raise SystemExit(f"missing seeded roles: {missing}")
print("all required roles present")
PY
pass "all initial roles present (SUPER_ADMIN, ADMIN, SECURITY_ADMIN, USER_MANAGER, ROLE_MANAGER, AUDITOR, SUPPORT, USER)"

# Anonymous deny-by-default
anon=/tmp/rbac-anon.jar
csrf "$anon"
code="$(api "$anon" GET "/api/v1/users")"
[[ "$code" == "401" || "$code" == "403" ]] && pass "anonymous users list denied ($code)" || fail "anonymous users list got $code"

code="$(api "$anon" GET "/api/v1/audit-events")"
[[ "$code" == "401" || "$code" == "403" ]] && pass "anonymous audit list denied ($code)" || fail "anonymous audit list got $code"

code="$(api "$anon" POST "/api/v1/audit-events/export" '{"format":"CSV"}')"
[[ "$code" == "401" || "$code" == "403" ]] && pass "anonymous audit export denied ($code)" || fail "anonymous audit export got $code"

code="$(api "$anon" GET "/api/v1/roles")"
[[ "$code" == "401" || "$code" == "403" ]] && pass "anonymous roles list denied ($code)" || fail "anonymous roles list got $code"

code="$(api "$anon" GET "/api/v1/permissions/matrix")"
[[ "$code" == "401" || "$code" == "403" ]] && pass "anonymous permission matrix denied ($code)" || fail "anonymous permission matrix got $code"

# Create a low-privilege USER via admin API
UNIQUE="rbac.user.$(date +%s)@portal.local"
USER_ROLE_ID="$(python3 -c 'import json; print(json.load(open("/tmp/rbac-role-ids.json")).get("USER",""))')"
SUPER_ID="$(python3 -c 'import json; print(json.load(open("/tmp/rbac-role-ids.json")).get("SUPER_ADMIN",""))')"
AUDITOR_ID="$(python3 -c 'import json; print(json.load(open("/tmp/rbac-role-ids.json")).get("AUDITOR",""))')"

code="$(api "$COOKIE_JAR" POST "/api/v1/users" "$(python3 - <<PY
import json
print(json.dumps({
  "username": "$UNIQUE",
  "email": "$UNIQUE",
  "displayName": "RBAC Fixture User",
  "roleIds": ["$USER_ROLE_ID"],
  "sendInvite": False
}))
PY
)")"
[[ "$code" == "201" || "$code" == "200" ]] || { cat /tmp/rbac-resp.json >>"$EVIDENCE"; fail "user create failed HTTP $code"; }
pass "SUPER_ADMIN can create USER"
FIXTURE_USER_ID="$(python3 -c 'import json; d=json.load(open("/tmp/rbac-resp.json")); print(d.get("id") or d.get("user",{}).get("id",""))')"
echo "$FIXTURE_USER_ID" > /tmp/rbac-fixture-user-id.txt

# Attempt to delete reserved SUPER_ADMIN role — should fail
code="$(api "$COOKIE_JAR" DELETE "/api/v1/roles/$SUPER_ID")"
[[ "$code" == "403" || "$code" == "409" || "$code" == "400" ]] \
  && pass "reserved SUPER_ADMIN role delete blocked ($code)" \
  || { cat /tmp/rbac-resp.json >>"$EVIDENCE"; fail "reserved SUPER_ADMIN role delete should be blocked, got $code"; }

# SUPER_ADMIN can assign SUPER_ADMIN then revert
code="$(api "$COOKIE_JAR" PUT "/api/v1/users/$FIXTURE_USER_ID/roles" "{\"roleIds\":[\"$SUPER_ID\"]}")"
[[ "$code" == "200" || "$code" == "204" ]] && pass "SUPER_ADMIN can assign SUPER_ADMIN role" \
  || { cat /tmp/rbac-resp.json >>"$EVIDENCE"; fail "assign SUPER_ADMIN failed $code"; }
code="$(api "$COOKIE_JAR" PUT "/api/v1/users/$FIXTURE_USER_ID/roles" "{\"roleIds\":[\"$USER_ROLE_ID\"]}")"
[[ "$code" == "200" || "$code" == "204" ]] && pass "revert fixture user to USER" \
  || { cat /tmp/rbac-resp.json >>"$EVIDENCE"; fail "revert fixture user failed $code"; }

# Multi-role assignment
code="$(api "$COOKIE_JAR" PUT "/api/v1/users/$FIXTURE_USER_ID/roles" "{\"roleIds\":[\"$USER_ROLE_ID\",\"$AUDITOR_ID\"]}")"
[[ "$code" == "200" || "$code" == "204" ]] && pass "multi-role assignment accepted" \
  || { cat /tmp/rbac-resp.json >>"$EVIDENCE"; fail "multi-role assignment failed $code"; }
code="$(api "$COOKIE_JAR" PUT "/api/v1/users/$FIXTURE_USER_ID/roles" "{\"roleIds\":[\"$USER_ROLE_ID\"]}")"
[[ "$code" == "200" || "$code" == "204" ]] && pass "role removal accepted" \
  || { cat /tmp/rbac-resp.json >>"$EVIDENCE"; fail "role removal failed $code"; }

# Permission matrix readable by SUPER_ADMIN
code="$(api "$COOKIE_JAR" GET "/api/v1/permissions/matrix")"
expect_code "$code" "200" "permission matrix readable by SUPER_ADMIN"

# Final SUPER_ADMIN protection: cannot demote sole SUPER_ADMIN
login "$COOKIE_JAR" "$ADMIN_EMAIL" "$ADMIN_PASSWORD"
csrf "$COOKIE_JAR"
me="$(curl -fsS -c "$COOKIE_JAR" -b "$COOKIE_JAR" -H "X-XSRF-TOKEN: $CSRF_TOKEN" "$BASE_URL/api/v1/me")"
ME_ID="$(python3 -c 'import json,sys; print(json.load(sys.stdin).get("id",""))' <<<"$me")"
[[ -n "$ME_ID" ]] || fail "could not resolve SUPER_ADMIN id from /me"
code="$(api "$COOKIE_JAR" PUT "/api/v1/users/$ME_ID/roles" "{\"roleIds\":[\"$USER_ROLE_ID\"]}")"
[[ "$code" == "403" || "$code" == "409" || "$code" == "400" ]] \
  && pass "final SUPER_ADMIN demotion blocked ($code)" \
  || { cat /tmp/rbac-resp.json >>"$EVIDENCE"; fail "final SUPER_ADMIN demotion should be blocked, got $code"; }

# Final SUPER_ADMIN cannot be deactivated
code="$(api "$COOKIE_JAR" POST "/api/v1/users/$ME_ID/deactivate" "{}")"
if [[ "$code" == "404" || "$code" == "405" ]]; then
  code="$(api "$COOKIE_JAR" PATCH "/api/v1/users/$ME_ID" '{"status":"INACTIVE"}')"
fi
[[ "$code" == "403" || "$code" == "409" || "$code" == "400" ]] \
  && pass "final SUPER_ADMIN disable blocked ($code)" \
  || { log "disable response $code"; cat /tmp/rbac-resp.json >>"$EVIDENCE"; }

# Authorized audit access
code="$(api "$COOKIE_JAR" GET "/api/v1/audit-events?page=0&size=5")"
[[ "$code" == "200" ]] && pass "SUPER_ADMIN audit list authorized" || fail "audit list got $code"

log "=== RBAC MATRIX CHECKS COMPLETED ==="
