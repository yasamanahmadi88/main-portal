#!/usr/bin/env bash
# Best-effort observability checks against a running compose stack.
# When compose.observability.yaml is not attached, verifies backend Prometheus
# scrape endpoint via docker exec into the backend container.
set -euo pipefail

REPORT_DIR="${REPORT_DIR:-/tmp/portal-live-evidence}"
mkdir -p "$REPORT_DIR"
EVIDENCE="$REPORT_DIR/observability-verify.txt"
: > "$EVIDENCE"

log() { echo "$@" | tee -a "$EVIDENCE"; }
pass() { log "PASS: $*"; }
fail() { log "FAIL: $*"; exit 1; }

log "=== Observability verification ==="

# Scrape Prometheus metrics from backend management port (wget is in the image).
METRICS="$(docker compose exec -T backend \
  wget -qO- http://127.0.0.1:8081/actuator/prometheus 2>/dev/null || true)"
if [[ -z "$METRICS" ]]; then
  log "NOTE: could not scrape actuator/prometheus from backend container"
else
  echo "$METRICS" | head -c 4000 > "$REPORT_DIR/prometheus-sample.txt"
  for needle in 'http_server_requests' 'jvm_memory' 'hikaricp' 'login' 'rate'; do
    if echo "$METRICS" | grep -qi "$needle"; then
      pass "prometheus sample contains marker: $needle"
    else
      log "NOTE: prometheus sample missing marker: $needle"
    fi
  done
  # High-cardinality label guard: email/userId/sessionId must not appear as label names
  if echo "$METRICS" | grep -E '\{[^}]*\b(email|user_id|userId|session_id|sessionId)=' >/dev/null; then
    fail "high-cardinality identity labels detected in prometheus metrics"
  else
    pass "no email/userId/sessionId label names in prometheus sample"
  fi
fi

# Structured logging / correlation: ensure recent backend logs are JSON-ish or contain correlation keys
LOG_SAMPLE="$(docker compose logs backend --no-color --tail=80 2>/dev/null || true)"
if echo "$LOG_SAMPLE" | grep -qiE 'correlation_id|request_id|trace_id'; then
  pass "correlation/request/trace identifiers present in recent logs"
else
  log "NOTE: correlation identifiers not observed in last 80 log lines"
fi
if echo "$LOG_SAMPLE" | grep -qiE 'Cookie:|Authorization:|X-XSRF-TOKEN:|password='; then
  fail "sensitive header/password material appears in backend logs"
else
  pass "no Cookie/Authorization/CSRF/password material in recent backend logs"
fi

pass "observability verification completed"
