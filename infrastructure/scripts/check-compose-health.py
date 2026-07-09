#!/usr/bin/env python3
"""Parse `docker compose ps --format json` and exit 0 only when required services are healthy."""
from __future__ import annotations

import json
import sys
from pathlib import Path

WANTED = {"postgres", "redis", "mailpit", "backend", "frontend"}


def load_rows(path: Path) -> list[dict]:
    raw = path.read_text().strip()
    if not raw:
        return []
    if raw.startswith("["):
        data = json.loads(raw)
        return data if isinstance(data, list) else [data]
    rows: list[dict] = []
    for line in raw.splitlines():
        line = line.strip()
        if line:
            rows.append(json.loads(line))
    return rows


def main() -> int:
    path = Path(sys.argv[1] if len(sys.argv) > 1 else "ps.json")
    rows = load_rows(path)
    have = {r.get("Service") for r in rows}
    print("services", sorted(x for x in have if x))
    if not WANTED.issubset(have):
        print("missing", sorted(WANTED - have))
        return 1
    unhealthy = [
        r.get("Service")
        for r in rows
        if r.get("Service") in WANTED and r.get("Health") not in (None, "", "healthy")
    ]
    if unhealthy:
        print("unhealthy", unhealthy)
        return 2
    need_health = [r for r in rows if r.get("Service") in WANTED and r.get("Health")]
    if need_health and any(r.get("Health") != "healthy" for r in need_health):
        print("not all healthy yet")
        return 3
    print("compose health ok")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
