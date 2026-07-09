#!/usr/bin/env bash
# Verify the Angular SPA can boot over the local HTTP compose stack.
# Fails if CSP upgrade-insecure-requests is present (breaks HTTP script loads)
# or if theme-bootstrap / Angular bundles are missing.
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
REPORT_DIR="${REPORT_DIR:-/tmp/portal-live-evidence}"
mkdir -p "$REPORT_DIR"

EVIDENCE="$REPORT_DIR/spa-boot.txt"
: > "$EVIDENCE"
log() { echo "$@" | tee -a "$EVIDENCE"; }
fail() { log "FAIL: $*"; exit 1; }
pass() { log "PASS: $*"; }

curl -sD "$REPORT_DIR/spa-headers.txt" -o "$REPORT_DIR/spa-index.html" "$BASE_URL/index.html" \
  || fail "GET /index.html"

grep -qi 'Content-Security-Policy:' "$REPORT_DIR/spa-headers.txt" \
  || fail "CSP header missing on /index.html"

if grep -qi 'upgrade-insecure-requests' "$REPORT_DIR/spa-headers.txt"; then
  cat "$REPORT_DIR/spa-headers.txt" >>"$EVIDENCE"
  fail "CSP upgrade-insecure-requests present on HTTP stack (SPA cannot load scripts)"
fi
pass "CSP has no upgrade-insecure-requests on HTTP"

grep -q 'theme-bootstrap.js' "$REPORT_DIR/spa-index.html" \
  || fail "theme-bootstrap.js not referenced in index.html"
# Event-handler attributes (onload=) are blocked by script-src 'self'.
if grep -qiE 'onload=|onerror=' "$REPORT_DIR/spa-index.html"; then
  fail "index.html contains CSP-blocked event-handler attributes"
fi
# Inline <script>...</script> bodies are also blocked.
if grep -qiE '<script[^>]*>[^<]+</script>' "$REPORT_DIR/spa-index.html"; then
  fail "index.html contains inline script bodies blocked by CSP"
fi
curl -fsS "$BASE_URL/theme-bootstrap.js" -o /dev/null \
  || fail "theme-bootstrap.js not served"
pass "theme-bootstrap.js served; index.html has no CSP-blocked inline scripts"

# i18n JSON must be reachable from deep routes (absolute /assets/... paths).
for path in \
  /assets/i18n/common/fa-IR.json \
  /assets/i18n/authentication/fa-IR.json \
  /assets/i18n/common/en-US.json
do
  code="$(curl -s -o /tmp/i18n-sample.json -w '%{http_code}' "$BASE_URL$path")"
  ctype="$(file -b --mime-type /tmp/i18n-sample.json 2>/dev/null || true)"
  [[ "$code" == "200" ]] || fail "i18n asset $path HTTP $code"
  python3 -c 'import json,sys; json.load(open("/tmp/i18n-sample.json"))' \
    || fail "i18n asset $path is not JSON (got HTML SPA fallback?)"
  log "i18n_ok path=$path http=$code mime=${ctype:-unknown}"
done
pass "i18n JSON assets served as JSON"

python3 - <<'PY' "$BASE_URL" "$REPORT_DIR" "$EVIDENCE"
import re, subprocess, sys, pathlib

base, report, evidence = sys.argv[1], pathlib.Path(sys.argv[2]), pathlib.Path(sys.argv[3])
html = (report / "spa-index.html").read_text(encoding="utf-8", errors="replace")

scripts = []
scripts += re.findall(r'type="module"[^>]*src="([^"]+)"', html)
scripts += re.findall(r'src="([^"]+)"[^>]*type="module"', html)
scripts += re.findall(r'src="([^"]+\.js)"', html)
scripts += re.findall(r'href="([^"]+\.js)"', html)
# de-dupe preserving order
seen = set()
ordered = []
for s in scripts:
    if s not in seen:
        seen.add(s)
        ordered.append(s)

if not ordered:
    print("FAIL: no script references in index.html", file=sys.stderr)
    sys.exit(1)

main = next((s for s in ordered if "theme-bootstrap" not in s), ordered[0])
url = base.rstrip("/") + "/" + main.lstrip("/")
proc = subprocess.run(["curl", "-s", "-o", "/tmp/spa-main.js", "-w", "%{http_code}", url], capture_output=True, text=True)
code = proc.stdout.strip()
with evidence.open("a") as f:
    f.write(f"main_bundle={main}\nmain_bundle_http={code}\n")
print(f"main_bundle={main} http={code}")
if code != "200":
    print("FAIL: main bundle not HTTP 200", file=sys.stderr)
    sys.exit(1)

found_in = None
for s in ordered:
    u = base.rstrip("/") + "/" + s.lstrip("/")
    try:
        body = subprocess.check_output(["curl", "-fsS", u], text=True, errors="replace")
    except subprocess.CalledProcessError:
        continue
    if "language-switcher" in body:
        found_in = s
        break

if not found_in:
    # Scan sibling chunks that may be imported (list directory via common hashed names in index)
    print("FAIL: language-switcher marker not found in any index-referenced JS", file=sys.stderr)
    sys.exit(1)

with evidence.open("a") as f:
    f.write(f"language_switcher_in={found_in}\n")
print(f"language-switcher found in {found_in}")
print("PASS: SPA assets and CSP are bootable over HTTP")
with evidence.open("a") as f:
    f.write("PASS: SPA assets and CSP are bootable over HTTP\n")
PY

pass "SPA boot smoke complete"
