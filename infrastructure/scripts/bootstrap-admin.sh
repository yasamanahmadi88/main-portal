#!/usr/bin/env bash
# ----------------------------------------------------------------------
#  bootstrap-admin.sh — document + validate the environment variables
#  required to seed the very first SUPER_ADMIN user of the portal.
#
#  This script NEVER prints the password.  It only:
#    1. Verifies the required variables are present.
#    2. Verifies the password meets the deployed policy (length ≥ 16,
#       at least one upper, lower, digit and symbol).
#    3. Optionally hits the backend `/api/v1/system/bootstrap-admin`
#       endpoint (invoked once and then permanently disabled by the
#       BootstrapAdminRunner).
#
#  Usage:
#      export BOOTSTRAP_ADMIN_EMAIL="ops@example.com"
#      export BOOTSTRAP_ADMIN_PASSWORD='use-a-real-passphrase!'   # NOT logged
#      export BOOTSTRAP_ADMIN_DISPLAY_NAME="Ops Bootstrap"
#      infrastructure/scripts/bootstrap-admin.sh
#
#  Exit codes:
#      0  — success
#      1  — required env var missing
#      2  — password policy violation
# ----------------------------------------------------------------------

set -euo pipefail

fail() { printf 'bootstrap-admin: %s\n' "$1" >&2; exit "${2:-1}"; }

require_var() {
    local name=$1
    if [[ -z "${!name:-}" ]]; then
        fail "$name is required" 1
    fi
}

require_var BOOTSTRAP_ADMIN_EMAIL
require_var BOOTSTRAP_ADMIN_PASSWORD
require_var BOOTSTRAP_ADMIN_DISPLAY_NAME

if ! [[ "$BOOTSTRAP_ADMIN_EMAIL" =~ ^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$ ]]; then
    fail "BOOTSTRAP_ADMIN_EMAIL is not a valid email address" 2
fi

pw="$BOOTSTRAP_ADMIN_PASSWORD"
if (( ${#pw} < 16 )); then
    fail "password must be at least 16 characters" 2
fi
if ! [[ "$pw" =~ [A-Z] && "$pw" =~ [a-z] && "$pw" =~ [0-9] && "$pw" =~ [^A-Za-z0-9] ]]; then
    fail "password must include upper, lower, digit and symbol" 2
fi
unset pw

cat <<'INFO'
bootstrap-admin: environment looks good.
  * A one-shot SUPER_ADMIN will be created the next time the backend starts
    with these variables present.
  * The BootstrapAdminRunner deletes/clears these variables from its own
    environment after use and refuses to run a second time.
  * Rotate the password IMMEDIATELY after first login, then unset all three
    variables from the deployment secret store.
INFO

# Optional health probe.  Set BOOTSTRAP_PROBE_URL=https://portal.example.com
# to enable.  We never send the password itself.
if [[ -n "${BOOTSTRAP_PROBE_URL:-}" ]]; then
    printf 'bootstrap-admin: probing %s/actuator/health ... ' "$BOOTSTRAP_PROBE_URL"
    if curl --fail --silent --show-error \
            --max-time 10 \
            "$BOOTSTRAP_PROBE_URL/actuator/health" >/dev/null; then
        echo "ok"
    else
        echo "unreachable" >&2
        exit 1
    fi
fi
