# Progress

## Current phase

Phases 0–15 delivered. Remaining live-verification gaps closed via green GitHub Actions `fullstack-verify` (compose stack, bootstrap, API security, RBAC matrix, audit DB, Playwright+Axe, ZAP, observability, rate-limit).

## Branch / PR

- Branch: `cursor/complete-enterprise-portal`
- Tip: `41eac10053e749b6dde0a305ee9fc2b1f9900e80`
- PR: https://github.com/yasamanahmadi88/main-portal/pull/2
- Green fullstack (push): https://github.com/yasamanahmadi88/main-portal/actions/runs/29050258913
- Green fullstack (PR): https://github.com/yasamanahmadi88/main-portal/actions/runs/29050262164

## Live verification results (executed)

| Item | Result |
|------|--------|
| Compose stack healthy | PASS |
| Bootstrap admin (Argon2id, SUPER_ADMIN, audit, no plaintext in logs) | PASS |
| Second bootstrap no-op after users exist | PASS |
| Auth after clearing bootstrap env | PASS |
| Live API (CSRF, session cookie, `/me`, logout, headers) | PASS |
| RBAC matrix (8 initial roles + SUPER_ADMIN protection) | PASS |
| Audit DB append-only + hash chain API | PASS |
| SPA boot smoke (CSP/i18n/assets) | PASS |
| Playwright + Axe live | PASS — **12 passed** |
| OWASP ZAP baseline | PASS artifact — 0 High, 1 Medium (CSP style-src unsafe-inline) |
| Login rate-limit burst | PASS — HTTP 429 observed |
| CI / container-build / sbom | PASS |

## SPA boot remediation (required for live e2e)

1. `upgrade-insecure-requests` only when HTTPS (`$forwarded_scheme`)
2. External `theme-bootstrap.js`; production `inlineCritical: false`
3. Absolute `/assets/i18n/...` prefixes
4. Preference `effect()` via `runInInjectionContext` before APP_INITIALIZER await
5. Muted text contrast raised for WCAG AA; snackbar action removed (aria-hidden-focus)

## Known limitations (truthful)

- Cursor Cloud cannot run Docker containers; live evidence is from GitHub Actions.
- Live TOTP enroll/challenge/recovery Playwright scenarios are not yet a dedicated suite (unit + encryption wiring verified).
- Full Tempo/Loki/Grafana correlation requires `compose.observability.yaml` profile (not started in default fullstack job).
- Production TLS/WAF/backup immutability remain environment/process controls.
- ZAP Medium: Angular Material requires `style-src 'unsafe-inline'` — accepted candidate, not falsely marked fixed.
- Session ID rotation before/after compare and idle-timeout soak are not yet automated shell assertions.
