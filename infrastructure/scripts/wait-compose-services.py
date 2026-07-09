#!/usr/bin/env python3
"""Wait helper: exit 0 when all named compose services report healthy."""
from __future__ import annotations

import json
import sys
from pathlib import Path


def load_rows(path: Path) -> list[dict]:
    raw = path.read_text().strip()
    if not raw:
        return []
    if raw.startswith("["):
        data = json.loads(raw)
        return data if isinstance(data, list) else [data]
    return [json.loads(line) for line in raw.splitlines() if line.strip()]


def main() -> int:
    if len(sys.argv) < 3:
        print("usage: wait-compose-services.py <ps.json> <service>...", file=sys.stderr)
        return 2
    path = Path(sys.argv[1])
    wanted = set(sys.argv[2:])
    rows = load_rows(path)
    have = {r.get("Service") for r in rows}
    print("services", sorted(x for x in have if x))
    if not wanted.issubset(have):
        print("missing", sorted(wanted - have))
        return 1
    unhealthy = [
        r.get("Service")
        for r in rows
        if r.get("Service") in wanted and r.get("Health") not in (None, "", "healthy")
    ]
    if unhealthy:
        print("unhealthy", unhealthy)
        return 2
    need = [r for r in rows if r.get("Service") in wanted and r.get("Health")]
    if need and any(r.get("Health") != "healthy" for r in need):
        print("not all healthy yet")
        return 3
    print("ok")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
