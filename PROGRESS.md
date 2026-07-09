# Progress

## Current phase

Phases 0–15 delivered. Closing remaining live-verification gaps via GitHub Actions `fullstack-verify` (compose stack, bootstrap, API security, RBAC matrix, audit DB, Playwright+Axe, ZAP, observability).

## Branch / PR

- Branch: `cursor/complete-enterprise-portal`
- PR: https://github.com/yasamanahmadi88/main-portal/pull/2

## Recent verification work

| Item | Result |
|------|--------|
| Final SUPER_ADMIN demotion/disable protection | Implemented + unit tested |
| Flyway V7 append-only audit grants | Present |
| Flyway V8 USER_MANAGER / ROLE_MANAGER / SUPPORT | Present |
| Live verification scripts | `bootstrap-verify`, `live-api-verify`, `rbac-matrix-verify`, `audit-db-verify`, `observability-verify` |
| Playwright + Axe live suite | `frontend/e2e/live-portal.spec.ts` (hard-fail when `PORTAL_E2E_REQUIRE_LIVE=1`) |
| Fullstack CI workflow | `.github/workflows/fullstack-verify.yml` |
| Local Docker in Cloud Agent | NOT AVAILABLE (overlay mount failure) — do not mark live e2e PASS from this host |
| Backend focused unit tests | PASS (AuthorizationDecision, PasswordHashing, Modulith, AuditHashChain) |
| Frontend Vitest | PASS (39) |

## SPA boot fix (2026-07-09)

Playwright failed because the Angular SPA never mounted under the Docker nginx CSP / deep-route asset loading:

1. `upgrade-insecure-requests` on HTTP localhost upgraded script fetches to HTTPS → bundles never loaded.
2. Inline theme bootstrap + Angular critical-CSS `onload=` handlers violated `script-src 'self'`.
3. Relative `assets/i18n/...` prefixes resolved under `/auth/login` as `/auth/assets/...`, so ngx-translate never completed and `APP_INITIALIZER` hung on the static "Loading…" placeholder.
4. `PreferencesSyncService.install()` called `effect()` after `await` in `APP_INITIALIZER` → Angular `NG0203` aborted bootstrap (confirmed via Playwright trace).

Remediation:

- nginx maps `upgrade-insecure-requests` only when `$forwarded_scheme` is `https`
- theme bootstrap moved to `public/theme-bootstrap.js`
- production build sets `inlineCritical: false`
- i18n loader prefixes are absolute (`/assets/i18n/...`) with bootstrap timeouts
- preferences effects installed via `runInInjectionContext` before any await
- `spa-boot-verify.sh` + Playwright artifact copy-on-failure added to `fullstack-verify`

## Known limitations (truthful)

- Live compose / Playwright / ZAP results depend on green `fullstack-verify` runs on GitHub-hosted runners.
- Some ASVS rows remain PARTIALLY VERIFIED (MFA live enroll, Tempo/Loki full stack, production TLS/WAF).
- ZAP baseline is uploaded as an artifact and does not fail the job by default (`fail_action: false`); high findings must still be reviewed.
- Rate-limit burst is verified by `rate-limit-verify.sh` as the final fullstack step (after other logins).
