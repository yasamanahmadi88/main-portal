#!/usr/bin/env bash
# Audit integrity checks against PostgreSQL for a running compose stack.
set -euo pipefail

REPORT_DIR="${REPORT_DIR:-/tmp/portal-live-evidence}"
mkdir -p "$REPORT_DIR"
EVIDENCE="$REPORT_DIR/audit-integrity.txt"
: > "$EVIDENCE"

log() { echo "$@" | tee -a "$EVIDENCE"; }
pass() { log "PASS: $*"; }
fail() { log "FAIL: $*"; exit 1; }

PSQL=(docker compose exec -T postgres psql -U "${POSTGRES_SUPERUSER:-portal_superuser}" -d "${POSTGRES_DB:-portal}" -v ON_ERROR_STOP=1)

log "=== Audit integrity DB verification ==="

# Append-only: runtime role cannot UPDATE/DELETE
APP_USER="${POSTGRES_USER:-portal_app}"

# Prefer privilege catalog inspection
PRIVS="$("${PSQL[@]}" -tAc "SELECT has_table_privilege('${APP_USER}', 'audit_events', 'UPDATE')::text || ',' || has_table_privilege('${APP_USER}', 'audit_events', 'DELETE')::text;" | tr -d '[:space:]')"
log "portal_app audit_events privileges UPDATE,DELETE=$PRIVS"
if [[ "$PRIVS" == "false,false" ]]; then
  pass "runtime role lacks UPDATE/DELETE on audit_events"
else
  log "WARN: expected false,false for UPDATE/DELETE; got $PRIVS — check init grants"
  fail "runtime DB role must not UPDATE/DELETE audit_events (got $PRIVS)"
fi

# Chain cursor must allow UPDATE (advance last_hash/sequence) but never DELETE.
CHAIN_PRIVS="$("${PSQL[@]}" -tAc "SELECT has_table_privilege('${APP_USER}', 'audit_event_chain', 'UPDATE')::text || ',' || has_table_privilege('${APP_USER}', 'audit_event_chain', 'DELETE')::text;" | tr -d '[:space:]')"
log "portal_app audit_event_chain privileges UPDATE,DELETE=$CHAIN_PRIVS"
[[ "$CHAIN_PRIVS" == "true,false" ]] && pass "runtime role can UPDATE chain cursor but not DELETE" \
  || fail "audit_event_chain privileges expected UPDATE=true,DELETE=false (got $CHAIN_PRIVS)"

# Attempt mutation of immutable audit_events as app role — must fail
set +e
"${PSQL[@]}" <<SQL >/tmp/audit-tamper.txt 2>&1
SET ROLE ${APP_USER};
UPDATE audit_events SET outcome = outcome WHERE FALSE;
SQL
upd=$?
"${PSQL[@]}" <<SQL >>/tmp/audit-tamper.txt 2>&1
SET ROLE ${APP_USER};
DELETE FROM audit_events WHERE FALSE;
SQL
del=$?
"${PSQL[@]}" <<SQL >>/tmp/audit-tamper.txt 2>&1
SET ROLE ${APP_USER};
DELETE FROM audit_event_chain WHERE FALSE;
SQL
chain_del=$?
set -e
cat /tmp/audit-tamper.txt >>"$EVIDENCE" || true
# Privilege revoke means statements error; either non-zero exit or ERROR in output is success
if grep -qiE 'permission denied|must be owner|ERROR' /tmp/audit-tamper.txt || [[ $upd -ne 0 || $del -ne 0 || $chain_del -ne 0 ]]; then
  pass "UPDATE/DELETE on audit_events and DELETE on chain rejected for runtime role"
else
  fail "audit tamper attempts as runtime role unexpectedly succeeded"
fi

# Hash chain fields populated
ROW="$("${PSQL[@]}" -tAc "SELECT COUNT(*) FROM audit_events WHERE current_hash IS NULL OR current_hash = '';" | tr -d '[:space:]')"
[[ "$ROW" == "0" ]] && pass "all audit rows have current_hash" || fail "$ROW audit rows missing current_hash"

TOTAL="$("${PSQL[@]}" -tAc "SELECT COUNT(*) FROM audit_events;" | tr -d '[:space:]')"
log "audit_events count=$TOTAL"
[[ "$TOTAL" != "0" ]] && pass "audit events present after live traffic" || log "NOTE: no audit events yet"

# Sensitive values not present in payload_json
SENS="$("${PSQL[@]}" -tAc "SELECT COUNT(*) FROM audit_events WHERE payload_json::text ~* '(password_hash|recovery.?code|totp.?secret|\"password\"\\s*:|csrf.?token|session.?id\\s*:\\s*\"[a-f0-9]{16})';" | tr -d '[:space:]')"
[[ "$SENS" == "0" ]] && pass "no obvious secrets in audit payload_json" || fail "possible secrets in audit payload count=$SENS"

# Chain continuity sample
"${PSQL[@]}" -c "SELECT sequence_number, left(coalesce(previous_hash,''),12) AS prev, left(current_hash,12) AS curr FROM audit_events ORDER BY sequence_number NULLS LAST, recorded_at LIMIT 5;" >>"$EVIDENCE"

pass "audit integrity DB checks completed"
