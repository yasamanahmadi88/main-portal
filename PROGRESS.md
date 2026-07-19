# Progress

## Current phase

Enterprise final upgrade on branch **`cursor/enterprise-final-upgrade-4bed`** (from consolidated `main`). Gap analysis: `docs/audit/enterprise-gap-analysis.md`.

## Branch policy

- Long-lived target: `main`
- Active upgrade branch: `cursor/enterprise-final-upgrade-4bed` (user-facing name: enterprise-final-upgrade)
- Obsolete remotes (`cursor/complete-enterprise-portal`, `cursor/portal-foundation`) contain no unique valuable commits vs `main` and are scheduled for deletion after upgrade PR registration

## Implemented in this upgrade

| Area | Status |
|------|--------|
| CAPTCHA (self-hosted SVG + Redis TTL, one-time consume) | Done — API + SPA + OpenAPI + unit/IT hooks |
| Monitoring SPA `/monitoring` + `MONITORING_*` permissions (V10) | Done |
| Auth Micrometer counters for Grafana | Done |
| Navbar security + monitoring shortcuts | Done |
| Theme token cleanup (brand surfaces) | Done |
| i18n: password confirm dialog (no English `prompt`) | Done |
| `SECURITY_EVENT_READ` → `SECURITY_READ` alignment | Done |
| Password-reset + MFA rate-limit wiring; auth fail-closed on Redis outage | Done |
| Grafana Prometheus/Loki datasource UIDs | Done |
| Verify scripts + fullstack CI CAPTCHA_REVEAL_ANSWER | Done |

## Verification (this upgrade)

| Item | Result |
|------|--------|
| Frontend Vitest / lint / build | PASS (43 tests; CI green) |
| Backend unit + LiveSecurity IT | PASS (CI Java 25) |
| Docker compose live stack | PASS — `fullstack-verify` https://github.com/yasamanahmadi88/main-portal/actions/runs/29684574811 |
| Container build / Trivy / SBOM / Gitleaks | PASS |
| PR | https://github.com/yasamanahmadi88/main-portal/pull/3 |

## Prior live verification (on `main` tip before upgrade)

| Item | Result |
|------|--------|
| Compose stack / bootstrap / CSRF / RBAC / audit / Playwright / ZAP / rate-limit / SBOM | PASS (see prior `QUALITY-GATES.md` evidence) |

## Known limitations (truthful)

- Dedicated live MFA enroll/challenge/recovery Playwright suite still expanding.
- Full Tempo/Loki/Grafana correlation needs `compose.observability.yaml` exercised in CI.
- `CAPTCHA_REVEAL_ANSWER` is for automated tests only; forbidden with `prod` profile (startup fails).
- Production TLS/WAF/backup immutability remain environment controls.
- External pen-test still required before production.
