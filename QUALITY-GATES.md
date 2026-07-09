# Quality Gates

| Gate | Status | Evidence |
|------|--------|----------|
| Repository structure | PASS | Root layout |
| Java 25 compile + tests | PASS | CI backend job + local unit suite |
| Angular 22 build + unit tests | PASS | CI frontend job (39 Vitest) |
| Architecture / Modulith tests | PASS | `ModulithTest` |
| OpenAPI lint | PASS | Spectral CI |
| Flyway migrations | PASS | V1–V8 (audit append-only + extra roles) |
| Redis indexed sessions | PASS | CI contextLoads + compose stack |
| CSRF / session auth | PASS | `live-api-verify.sh` + MockMvc CSRF tests |
| MFA / RBAC / audit unit evidence | PASS | Dedicated unit tests + final SUPER_ADMIN guards |
| Full e2e / axe live | IN CI | `fullstack-verify` Playwright + Axe (`PORTAL_E2E_REQUIRE_LIVE=1`) |
| Compose config | PASS | CI + local `docker compose config` |
| Full compose stack smoke | IN CI | `fullstack-verify` (Cloud Agent cannot run containers) |
| Bootstrap admin verification | IN CI | `bootstrap-verify.sh` |
| RBAC matrix (API) | IN CI | `rbac-matrix-verify.sh` |
| Audit append-only + hash chain | IN CI | `audit-db-verify.sh` + integrity API |
| Observability / log redaction | IN CI | `observability-verify.sh` |
| Container build + Trivy CRITICAL | PASS | container-build workflow |
| SBOM | PASS | sbom workflow |
| Secret scan | PASS | Gitleaks CI |
| OWASP ZAP baseline | IN CI | `fullstack-verify` ZAP step (artifact; non-blocking baseline) |
| No committed secrets | PASS | placeholders only |
| ASVS evidence | PARTIAL→expanding | `docs/security/asvs-5-checklist.md` |
| Pull Request | OPEN | https://github.com/yasamanahmadi88/main-portal/pull/2 |

## Commands

```bash
# Backend
cd backend && ./mvnw clean verify

# Frontend
cd frontend && npm ci && npm run lint && npm test && npm run build

# Live stack (Docker-capable host or GitHub Actions fullstack-verify)
docker compose --env-file .env config
docker compose --env-file .env build
docker compose --env-file .env up -d
./infrastructure/scripts/bootstrap-verify.sh
./infrastructure/scripts/live-api-verify.sh
./infrastructure/scripts/rbac-matrix-verify.sh
./infrastructure/scripts/audit-db-verify.sh
./infrastructure/scripts/observability-verify.sh
cd frontend && PORTAL_E2E_REQUIRE_LIVE=1 npx playwright test e2e/live-portal.spec.ts
```

## Cloud Agent limitation

This Cursor Cloud environment can run `docker info` but cannot create containers (overlay mount `invalid argument`). Live stack verification is therefore executed in GitHub Actions via `fullstack-verify.yml`, not marked PASS from this agent host.
