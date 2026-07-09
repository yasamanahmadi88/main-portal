#!/usr/bin/env bash
# ----------------------------------------------------------------------
#  wait-for-healthy.sh — block until every named docker compose service is
#  reporting `healthy` (or until the timeout expires).
#
#  Usage:
#      infrastructure/scripts/wait-for-healthy.sh [-f compose.yaml] [-t 300] service [service …]
#
#  Options:
#      -f FILE     compose file to consult (repeatable; default: compose.yaml)
#      -t SECONDS  overall timeout in seconds (default: 300)
#      -i SECONDS  poll interval (default: 5)
#      -v          verbose
#
#  Exit codes:
#      0  — all services healthy
#      1  — one or more services are `unhealthy`
#      2  — usage error
#      124 — timed out waiting
# ----------------------------------------------------------------------

set -euo pipefail

timeout=300
interval=5
verbose=0
compose_args=()

while getopts ':f:t:i:vh' opt; do
    case "$opt" in
        f) compose_args+=(-f "$OPTARG") ;;
        t) timeout="$OPTARG" ;;
        i) interval="$OPTARG" ;;
        v) verbose=1 ;;
        h)
            grep -E '^#( |$)' "$0" | sed 's/^# \{0,1\}//' >&2
            exit 0
            ;;
        *) echo "usage error" >&2; exit 2 ;;
    esac
done
shift $((OPTIND - 1))

if [[ $# -lt 1 ]]; then
    echo "usage: $0 [-f compose.yaml] [-t seconds] service [service …]" >&2
    exit 2
fi

[[ ${#compose_args[@]} -eq 0 ]] && compose_args=(-f compose.yaml)

deadline=$(( $(date +%s) + timeout ))

state_of() {
    local svc=$1
    local cid
    cid=$(docker compose "${compose_args[@]}" ps -q "$svc" 2>/dev/null | head -n1)
    if [[ -z "$cid" ]]; then
        echo "missing"
        return
    fi
    docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$cid" 2>/dev/null || echo "unknown"
}

while :; do
    all_ok=1
    unhealthy=""
    for svc in "$@"; do
        s=$(state_of "$svc")
        [[ $verbose -eq 1 ]] && printf '  %-16s %s\n' "$svc" "$s"
        case "$s" in
            healthy|running) : ;;
            unhealthy)       unhealthy="$unhealthy $svc"; all_ok=0 ;;
            *)               all_ok=0 ;;
        esac
    done

    if [[ -n "$unhealthy" ]]; then
        echo "wait-for-healthy: FAILED — unhealthy:$unhealthy" >&2
        exit 1
    fi

    if [[ $all_ok -eq 1 ]]; then
        [[ $verbose -eq 1 ]] && echo "wait-for-healthy: all services healthy"
        exit 0
    fi

    if (( $(date +%s) >= deadline )); then
        echo "wait-for-healthy: TIMEOUT after ${timeout}s" >&2
        docker compose "${compose_args[@]}" ps >&2 || true
        exit 124
    fi

    sleep "$interval"
done
