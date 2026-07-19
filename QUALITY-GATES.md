# Quality Gates

Upgrade branch: `cursor/enterprise-final-upgrade-4bed`
Gap analysis: `docs/audit/enterprise-gap-analysis.md`
Prior green fullstack (pre-upgrade `main`): https://github.com/yasamanahmadi88/main-portal/actions/runs/29050258913

| Gate | Status | Evidence |
|------|--------|----------|
| Repository structure | PASS | Root layout |
| Gap analysis (Phase 1) | PASS | `docs/audit/enterprise-gap-analysis.md` |
| Java 25 compile + unit tests | PASS (local subset) | CaptchaService + Modulith + ArchUnit + crypto/password (JDK 25) |
| Angular 22 build + unit tests | PASS | Vitest **43** + lint 0 errors + production build |
| CAPTCHA unit coverage | PASS | Valid / invalid / expired / missing + prod reveal guard |
| OpenAPI captcha + monitoring paths | PASS | `contracts/openapi/portal-v1.yaml` |
| Flyway V10 monitoring permissions | PASS | Migration present; SUPER_ADMIN/SECURITY_ADMIN/ADMIN/SUPPORT grants |
| Theme LIGHT/DARK/SYSTEM | PASS | Existing ThemeService + FOUC bootstrap; brand tokens |
| i18n fa-IR / en-US + RTL/LTR | PASS | Parity suite + password-confirm dialog keys |
| Monitoring SPA route | PASS | `/monitoring` + `MONITORING_READ` guard |
| CSRF / session auth design | PASS | Unchanged; CAPTCHA precedes credentials |
| Compose config | PENDING CI | `fullstack-verify` on upgrade branch |
| Full compose stack smoke | PENDING CI | Docker unavailable on this agent host |
| Container build + Trivy / SBOM / Gitleaks | PENDING CI | Existing workflows |
| OWASP ZAP baseline | PRIOR PASS | 0 High; 1 Medium CSP (documented) |
| ASVS evidence | PARTIAL | `docs/security/asvs-5-checklist.md` |
| Pull Request | OPEN | Upgrade PR for this branch |

## Commands

```bash
# Backend
cd backend && ./mvnw clean verify

# Frontend
cd frontend && npm ci && npm run lint && npm test && npm run build

# Live stack (Docker-capable host or GitHub Actions fullstack-verify)
cp .env.example .env   # set secrets; for CI/e2e set CAPTCHA_REVEAL_ANSWER=true
docker compose --env-file .env config
docker compose --env-file .env up -d --build
./infrastructure/scripts/bootstrap-verify.sh
./infrastructure/scripts/live-api-verify.sh
./infrastructure/scripts/rbac-matrix-verify.sh
./infrastructure/scripts/audit-db-verify.sh
./infrastructure/scripts/observability-verify.sh
cd frontend && PORTAL_E2E_REQUIRE_LIVE=1 npx playwright test e2e/live-portal.spec.ts
./infrastructure/scripts/rate-limit-verify.sh
```

## Cloud Agent limitation

This Cursor Cloud environment can run unit/lint/build but cannot create Docker containers. Live stack verification is executed in GitHub Actions via `fullstack-verify.yml`.
