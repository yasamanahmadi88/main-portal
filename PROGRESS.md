# Progress

## Current phase

Enterprise portal consolidated onto **`main`** as the sole long-lived branch. Live verification gaps closed via green GitHub Actions `fullstack-verify`. Compared with `secure-portal` and retained as the advanced codebase (see `docs/comparison/secure-portal-vs-main-portal.md`).

## Branch policy

- **Only `main`** is kept on `yasamanahmadi88/main-portal`.
- Feature / agent branches were deleted after consolidation.
- Tip after consolidation includes login hardening (generic lockout responses) and ops docs ported from `secure-portal`.

## Live verification (executed)

| Item | Result |
|------|--------|
| Compose stack healthy | PASS |
| Bootstrap admin (Argon2id, SUPER_ADMIN) | PASS |
| Live API CSRF / session / logout / headers | PASS |
| RBAC matrix (8 roles + SUPER_ADMIN guards) | PASS |
| Audit append-only + hash chain | PASS |
| Playwright + Axe | PASS (12 tests) |
| OWASP ZAP baseline | 0 High; 1 Medium CSP style-src (documented) |
| Login rate-limit burst | PASS (HTTP 429) |
| CI / container-build / sbom | PASS |

## Ported from secure-portal (docs/DX only)

- `docs/operations/key-rotation.md`
- `docs/operations/incident-response.md`
- `docs/operations/backup-restore.md`
- `compose.dev.yaml`

## Login / security polish (post-verify)

- Locked accounts return the same generic auth failure as bad credentials (no lock-state enumeration).
- Removed unimplemented “trust this device” checkbox from the login UI.

## Known limitations (truthful)

- Dedicated live MFA enroll/challenge/recovery Playwright suite still expanding.
- Full Tempo/Loki/Grafana correlation needs `compose.observability.yaml`.
- Production TLS/WAF/backup immutability remain environment controls.
- External pen-test still required before production.
