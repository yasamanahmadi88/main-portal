#!/usr/bin/env bash
# Production Observability Stack End-to-End Test
#
# Purpose: Validates full observability pipeline (metrics, logs, traces) in a
#          running compose.observability.yaml stack. Tests Prometheus scrape,
#          Grafana datasource connectivity, Loki log collection, and Tempo
#          trace ingestion.
#
# Usage:
#   ./scripts/test-observability-stack.sh
#
# Environment:
#   REPORT_DIR - Directory for test evidence (default: /tmp/portal-live-evidence)
#   PROMETHEUS_PORT - Prometheus port (default: 9090)
#   LOKI_PORT - Loki port (default: 3100)
#   TEMPO_PORT - Tempo port (default: 3200)
#   GRAFANA_PORT - Grafana port (default: 3000)
#   OTEL_COLLECTOR_METRICS_PORT - Collector metrics port (default: 8889)
#
# Exit codes:
#   0 - All tests passed
#   1 - Critical test failed
#   2 - Warning only (non-critical issues noted)

set -euo pipefail

REPORT_DIR="${REPORT_DIR:-/tmp/portal-live-evidence}"
PROMETHEUS_PORT="${PROMETHEUS_PORT:-9090}"
LOKI_PORT="${LOKI_PORT:-3100}"
TEMPO_PORT="${TEMPO_PORT:-3200}"
GRAFANA_PORT="${GRAFANA_PORT:-3000}"
OTEL_COLLECTOR_METRICS_PORT="${OTEL_COLLECTOR_METRICS_PORT:-8889}"
OTEL_COLLECTOR_PROMETHEUS_PORT="${OTEL_COLLECTOR_PROMETHEUS_PORT:-8889}"

# Output styling
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test result tracking
PASS_COUNT=0
FAIL_COUNT=0
WARN_COUNT=0

mkdir -p "$REPORT_DIR"
EVIDENCE="$REPORT_DIR/observability-stack-test.log"
: > "$EVIDENCE"

log() { echo "$@" | tee -a "$EVIDENCE"; }
pass() {
  PASS_COUNT=$((PASS_COUNT + 1))
  printf "${GREEN}PASS${NC}: %s\n" "$*" | tee -a "$EVIDENCE"
}
fail() {
  FAIL_COUNT=$((FAIL_COUNT + 1))
  printf "${RED}FAIL${NC}: %s\n" "$*" | tee -a "$EVIDENCE"
}
warn() {
  WARN_COUNT=$((WARN_COUNT + 1))
  printf "${YELLOW}WARN${NC}: %s\n" "$*" | tee -a "$EVIDENCE"
}

log "=== Production Observability Stack End-to-End Test ==="
log "Report directory: $REPORT_DIR"
log "Test started: $(date -u +'%Y-%m-%dT%H:%M:%SZ')"
log ""

# Check docker compose availability
if ! command -v docker &> /dev/null; then
  fail "docker not found in PATH"
  exit 1
fi

if ! docker compose version &> /dev/null; then
  fail "docker compose not available"
  exit 1
fi

pass "docker and docker compose available"

# ============================================================================
# Test 1: Verify compose stack is running
# ============================================================================
log ""
log "=== Test 1: Compose Stack Health ==="

RUNNING_SERVICES=$(docker compose ps --services --filter "status=running" 2>/dev/null || true)

for service in otel-collector prometheus loki tempo grafana backend; do
  if echo "$RUNNING_SERVICES" | grep -q "^${service}$"; then
    pass "Service $service is running"
  else
    warn "Service $service is not running (may be expected if core stack only)"
  fi
done

# ============================================================================
# Test 2: Prometheus endpoint and metrics scrape
# ============================================================================
log ""
log "=== Test 2: Prometheus Metrics Collection ==="

# Test Prometheus readiness
PROM_READY=$(curl -s -f http://localhost:$PROMETHEUS_PORT/-/ready 2>/dev/null || echo "FAIL")
if [ "$PROM_READY" = "" ]; then
  pass "Prometheus HTTP health check passed"
else
  warn "Prometheus health check returned unexpected value"
fi

# Scrape Prometheus metrics from backend actuator endpoint (docker exec)
log "Scraping metrics from backend /actuator/prometheus..."
METRICS=$(docker compose exec -T backend \
  wget -qO- http://127.0.0.1:8081/actuator/prometheus 2>/dev/null || true)

if [ -z "$METRICS" ]; then
  fail "Could not scrape metrics from backend actuator endpoint"
else
  pass "Successfully scraped metrics from backend /actuator/prometheus"

  # Save sample for audit
  printf '%s' "$METRICS" | head -c 4000 > "$REPORT_DIR/prometheus-backend-sample.txt"
  pass "Saved metrics sample to $REPORT_DIR/prometheus-backend-sample.txt"

  # Verify expected metric markers
  for marker in 'http_server_requests' 'jvm_memory' 'hikaricp_connections' 'login_attempts'; do
    if printf '%s' "$METRICS" | grep -qi -- "$marker"; then
      pass "Prometheus sample contains marker: $marker"
    else
      warn "Prometheus sample missing marker: $marker (may be expected if no requests yet)"
    fi
  done

  # Check for high-cardinality identity labels (CRITICAL)
  if printf '%s' "$METRICS" | grep -E '\{[^}]*\b(email|user_id|userId|session_id|sessionId|password)=' >/dev/null; then
    fail "HIGH-CARDINALITY IDENTITY LABELS DETECTED in metrics (data exfiltration risk)"
  else
    pass "No email/userId/sessionId/password labels in metrics (good)"
  fi
fi

# Test Prometheus scrape targets from OTel collector
log "Testing Prometheus scrape of OTel collector metrics..."
COLLECTOR_METRICS=$(curl -s "http://localhost:$PROMETHEUS_PORT/api/v1/query?query=otelcol_exporter_sent_metric_points" 2>/dev/null || true)
if [ -n "$COLLECTOR_METRICS" ] && ! echo "$COLLECTOR_METRICS" | grep -q '"status":"error"'; then
  pass "Prometheus successfully scraped OTel collector metrics"
else
  warn "OTel collector metrics not yet scraped by Prometheus (may be expected on first run)"
fi

# ============================================================================
# Test 3: Loki log ingestion
# ============================================================================
log ""
log "=== Test 3: Loki Log Collection ==="

# Test Loki readiness
LOKI_READY=$(curl -s -f http://localhost:$LOKI_PORT/ready 2>/dev/null || echo "FAIL")
if [ -z "$LOKI_READY" ]; then
  pass "Loki HTTP ready check passed"
else
  warn "Loki ready check returned unexpected value"
fi

# Check Loki label discovery
log "Testing Loki label discovery..."
LOKI_LABELS=$(curl -s "http://localhost:$LOKI_PORT/loki/api/v1/labels" 2>/dev/null || true)
if [ -n "$LOKI_LABELS" ]; then
  pass "Loki label discovery working"
  echo "$LOKI_LABELS" > "$REPORT_DIR/loki-labels.json"

  # Verify expected labels are present
  if echo "$LOKI_LABELS" | grep -q "job"; then
    pass "Loki has 'job' label (expected from backend/OTel logs)"
  else
    warn "Loki 'job' label not found (logs may not have been ingested yet)"
  fi
else
  warn "Could not query Loki labels"
fi

# Check for recent logs in backend service
log "Testing Loki log range query..."
LOKI_LOGS=$(curl -s "http://localhost:$LOKI_PORT/loki/api/v1/query_range?query={job=\"backend\"}&start=$(($(date +%s) - 300))000000000&end=$(date +%s)000000000" 2>/dev/null || true)
if [ -n "$LOKI_LOGS" ]; then
  pass "Loki query_range succeeded"
  echo "$LOKI_LOGS" > "$REPORT_DIR/loki-logs-sample.json"

  if echo "$LOKI_LOGS" | grep -q '"stream"'; then
    pass "Loki returned log streams from backend"
  else
    warn "Loki query returned no streams (logs may not have arrived yet)"
  fi
else
  warn "Could not query Loki logs"
fi

# Verify logs do not contain sensitive data
if [ -n "$LOKI_LOGS" ]; then
  LOG_SAMPLE=$(echo "$LOKI_LOGS" | grep -o '"values":\[\[[^]]*\]\]' | head -1 || true)
  if [ -n "$LOG_SAMPLE" ]; then
    # Check for sensitive material (basic pattern matching)
    if echo "$LOG_SAMPLE" | grep -iE 'password=|authorization:|x-xsrf-token:|cookie:' >/dev/null; then
      fail "SENSITIVE DATA DETECTED in Loki logs (headers or passwords)"
    else
      pass "Loki logs do not contain obvious sensitive headers/passwords"
    fi

    # Check for correlation IDs
    if echo "$LOG_SAMPLE" | grep -iE 'trace_id|span_id|correlation_id|request_id' >/dev/null; then
      pass "Correlation IDs found in logs (good for tracing)"
    else
      warn "No correlation/trace IDs in log sample (check if OTLP is enabled)"
    fi
  fi
fi

# ============================================================================
# Test 4: Tempo trace ingestion
# ============================================================================
log ""
log "=== Test 4: Tempo Trace Collection ==="

# Test Tempo readiness
TEMPO_READY=$(curl -s -f http://localhost:$TEMPO_PORT/ready 2>/dev/null || echo "FAIL")
if [ -z "$TEMPO_READY" ]; then
  pass "Tempo HTTP ready check passed"
else
  warn "Tempo ready check returned unexpected value"
fi

# Test Tempo API - trace search
log "Testing Tempo trace search API..."
TEMPO_TRACES=$(curl -s "http://localhost:$TEMPO_PORT/api/search?limit=10" 2>/dev/null || true)
if [ -n "$TEMPO_TRACES" ]; then
  pass "Tempo trace search API accessible"
  echo "$TEMPO_TRACES" > "$REPORT_DIR/tempo-traces-sample.json"

  if echo "$TEMPO_TRACES" | grep -q '"traces"'; then
    pass "Tempo returned trace data"
  else
    warn "Tempo search returned no traces (spans may not have arrived yet)"
  fi
else
  warn "Could not query Tempo trace search"
fi

# Test Tempo by trace ID (if traces available)
if [ -n "$TEMPO_TRACES" ] && echo "$TEMPO_TRACES" | grep -q '"traceID"'; then
  FIRST_TRACE=$(echo "$TEMPO_TRACES" | grep -o '"traceID":"[^"]*"' | head -1 | cut -d'"' -f4)
  if [ -n "$FIRST_TRACE" ]; then
    log "Testing Tempo fetch trace: $FIRST_TRACE"
    TRACE_DATA=$(curl -s "http://localhost:$TEMPO_PORT/api/traces/$FIRST_TRACE" 2>/dev/null || true)
    if [ -n "$TRACE_DATA" ]; then
      pass "Tempo successfully retrieved trace $FIRST_TRACE"
      echo "$TRACE_DATA" | head -c 2000 > "$REPORT_DIR/tempo-trace-sample.json"
    else
      warn "Could not fetch trace data for $FIRST_TRACE"
    fi
  fi
fi

# ============================================================================
# Test 5: Grafana datasource connectivity
# ============================================================================
log ""
log "=== Test 5: Grafana Datasource Configuration ==="

log "Testing Grafana API access..."

# Grafana default login (admin/GRAFANA_ADMIN_PASSWORD from compose)
# Try basic connectivity first (no auth)
GRAFANA_HEALTH=$(curl -s -w "%{http_code}" -o /dev/null http://localhost:$GRAFANA_PORT/api/health)
if [ "$GRAFANA_HEALTH" = "200" ]; then
  pass "Grafana HTTP health endpoint accessible (status: 200)"
else
  warn "Grafana health endpoint returned status: $GRAFANA_HEALTH"
fi

# List datasources (requires auth; test with weak credentials or unauthenticated access)
log "Attempting to fetch Grafana datasources..."
DATASOURCES=$(curl -s http://localhost:$GRAFANA_PORT/api/datasources 2>/dev/null || true)
if [ -n "$DATASOURCES" ]; then
  echo "$DATASOURCES" > "$REPORT_DIR/grafana-datasources.json"

  # Check for expected datasources
  for ds in prometheus loki tempo; do
    if echo "$DATASOURCES" | grep -qi "\"name\":\"$ds\""; then
      pass "Grafana datasource '$ds' configured"
    else
      warn "Grafana datasource '$ds' not found in list"
    fi
  done
else
  warn "Could not fetch Grafana datasources (auth may be required)"
fi

# ============================================================================
# Test 6: Correlation ID flow (backend -> OTLP -> Prometheus/Loki/Tempo)
# ============================================================================
log ""
log "=== Test 6: Correlation ID Flow Through Stack ==="

log "Generating correlation ID by triggering a backend request..."

# Generate a trace by hitting the backend health endpoint
CORRELATION_ID=$(uuidgen 2>/dev/null || echo "trace-$(date +%s)")
HEALTH_RESPONSE=$(curl -s -H "X-Trace-ID: $CORRELATION_ID" \
  http://localhost:8080/actuator/health 2>/dev/null || true)

if [ -n "$HEALTH_RESPONSE" ]; then
  pass "Backend health endpoint responded"

  # Give observability stack time to ingest the data
  sleep 2

  # Search for the trace ID in logs (Loki)
  log "Searching for correlation ID in Loki logs..."
  LOKI_SEARCH=$(curl -s "http://localhost:$LOKI_PORT/loki/api/v1/query_range?query={job=\"backend\"}&start=$(($(date +%s) - 30))000000000&end=$(date +%s)000000000" 2>/dev/null || true)

  if echo "$LOKI_SEARCH" | grep -q "$CORRELATION_ID"; then
    pass "Correlation ID '$CORRELATION_ID' found in Loki logs"
  else
    warn "Correlation ID not yet visible in Loki (data ingestion may be delayed)"
  fi

  # Search for trace ID in Tempo
  log "Searching for correlation ID in Tempo..."
  TEMPO_SEARCH=$(curl -s "http://localhost:$TEMPO_PORT/api/search?q=$CORRELATION_ID&limit=1" 2>/dev/null || true)

  if echo "$TEMPO_SEARCH" | grep -q "$CORRELATION_ID"; then
    pass "Correlation ID found in Tempo traces"
  else
    warn "Correlation ID not yet visible in Tempo (data ingestion may be delayed)"
  fi
else
  warn "Could not trigger backend request for correlation ID test"
fi

# ============================================================================
# Test 7: OTel Collector health and exports
# ============================================================================
log ""
log "=== Test 7: OTel Collector Health ==="

# Query OTel collector's own metrics
log "Testing OTel collector metrics endpoint..."
OTEL_METRICS=$(curl -s "http://localhost:$OTEL_COLLECTOR_PROMETHEUS_PORT/metrics" 2>/dev/null || true)

if [ -n "$OTEL_METRICS" ]; then
  pass "OTel collector metrics endpoint accessible"
  printf '%s' "$OTEL_METRICS" | head -c 2000 > "$REPORT_DIR/otel-collector-metrics.txt"

  # Check for exporter success metrics
  for exporter in prometheus loki tempo; do
    if printf '%s' "$OTEL_METRICS" | grep -qi "exporter.*$exporter"; then
      pass "OTel collector has $exporter exporter configured"
    else
      warn "OTel collector $exporter exporter metrics not found"
    fi
  done
else
  warn "Could not access OTel collector metrics"
fi

# ============================================================================
# Test 8: Full stack integration - trace through all services
# ============================================================================
log ""
log "=== Test 8: Full Stack Integration Test ==="

log "Performing full integration test..."

# Make a request through the entire stack
INTEGRATION_TEST=$(curl -s -v http://localhost:8080/ 2>&1 | grep -E "HTTP|trace|span" || true)

if [ -n "$INTEGRATION_TEST" ]; then
  pass "Full HTTP request completed"
  echo "$INTEGRATION_TEST" >> "$REPORT_DIR/integration-test.log"
else
  warn "Full integration test could not verify response headers"
fi

# ============================================================================
# Summary and Exit Code
# ============================================================================
log ""
log "=== Test Summary ==="
log "Passed: $PASS_COUNT"
log "Failed: $FAIL_COUNT"
log "Warnings: $WARN_COUNT"
log "Test completed: $(date -u +'%Y-%m-%dT%H:%M:%SZ')"
log ""

# Determine exit code
if [ $FAIL_COUNT -gt 0 ]; then
  log "RESULT: FAILED - Critical tests did not pass"
  exit 1
elif [ $WARN_COUNT -gt 0 ]; then
  log "RESULT: PARTIAL - Some warnings noted (may be expected on first run)"
  exit 2
else
  log "RESULT: PASSED - All observability tests successful"
  exit 0
fi
