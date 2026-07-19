# Quality Gates

Upgrade branch: `cursor/enterprise-final-upgrade-4bed`
Gap analysis: `docs/audit/enterprise-gap-analysis.md`
Green fullstack (upgrade): https://github.com/yasamanahmadi88/main-portal/actions/runs/29684574811
Green CI backend/frontend: https://github.com/yasamanahmadi88/main-portal/actions/runs/29684574819
PR: https://github.com/yasamanahmadi88/main-portal/pull/3

| Gate | Status | Evidence |
|------|--------|----------|
| Repository structure | PASS | Root layout |
| Gap analysis (Phase 1) | PASS | `docs/audit/enterprise-gap-analysis.md` |
| Java 25 compile + tests | PASS | CI backend job (Captcha + LiveSecurity IT + Modulith) |
| Angular 22 build + unit tests | PASS | CI frontend + Vitest 43 |
| CAPTCHA unit / IT | PASS | CaptchaServiceTest + LiveSecurityIntegrationTest |
| OpenAPI captcha + monitoring | PASS | Spectral CI |
| Flyway V10 monitoring permissions | PASS | Migration + live stack bootstrap |
| Theme LIGHT/DARK/SYSTEM | PASS | ThemeService + live Playwright |
| i18n fa-IR / en-US + RTL/LTR | PASS | Parity + live Playwright |
| Monitoring SPA `/monitoring` | PASS | Feature + RBAC permissions |
| CSRF / session / CAPTCHA login | PASS | `fullstack-verify` live-api + Playwright |
| Compose config | PASS | container-build + fullstack |
| Full compose stack smoke | PASS | `fullstack-verify` |
| Container build + Trivy | PASS | container-build workflow |
| SBOM | PASS | sbom workflow |
| Secret scan | PASS | Gitleaks CI |
| Login rate limiting | PASS | rate-limit-verify in fullstack |
| Pull Request | OPEN | https://github.com/yasamanahmadi88/main-portal/pull/3 |

## Commands

```bash
cd backend && ./mvnw clean verify
cd frontend && npm ci && npm run lint && npm test && npm run build
docker compose --env-file .env -f compose.yaml up -d --build
./infrastructure/scripts/bootstrap-verify.sh
./infrastructure/scripts/live-api-verify.sh
./infrastructure/scripts/rbac-matrix-verify.sh
cd frontend && PORTAL_E2E_REQUIRE_LIVE=1 npx playwright test e2e/live-portal.spec.ts
```
