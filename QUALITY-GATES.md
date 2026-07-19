# Quality Gates

Evidence tip: `41eac10053e749b6dde0a305ee9fc2b1f9900e80`  
Green fullstack run: https://github.com/yasamanahmadi88/main-portal/actions/runs/29050258913  
PR fullstack run: https://github.com/yasamanahmadi88/main-portal/actions/runs/29050262164  
Artifact: `live-stack-evidence` (+ `zap-baseline-report`)

| Gate | Status | Evidence |
|------|--------|----------|
| Repository structure | PASS | Root layout |
| Java 25 compile + tests | PASS | CI backend job + local unit suite |
| Angular 22 build + unit tests | PASS | CI frontend job (40 Vitest) |
| Architecture / Modulith tests | PASS | `ModulithTest` |
| OpenAPI lint | PASS | Spectral CI |
| Flyway migrations | PASS | V1–V9 (audit append-only + roles + chain cursor) |
| Redis indexed sessions | PASS | Live stack + `live-api-verify.sh` session cookie / listing |
| CSRF / session auth | PASS | Live API + Playwright CSRF 403 + logout invalidation |
| MFA / RBAC / audit unit evidence | PASS | Dedicated unit tests + final SUPER_ADMIN guards |
| Full e2e / axe live | PASS | `fullstack-verify` Playwright **12 passed** (`PORTAL_E2E_REQUIRE_LIVE=1`) |
| Compose config | PASS | CI `docker compose config` |
| Full compose stack smoke | PASS | `fullstack-verify` healthy postgres/redis/backend/frontend |
| Bootstrap admin verification | PASS | `bootstrap-verify.sh` + second-start no-op + post-clear auth |
| RBAC matrix (API) | PASS | `rbac-matrix-verify.sh` (all initial roles + SUPER_ADMIN guards) |
| Audit append-only + hash chain | PASS | `audit-db-verify.sh` + integrity API |
| Observability / log redaction | PASS | `observability-verify.sh` (no secret headers in logs) |
| Login rate limiting | PASS | `rate-limit-verify.sh` observed HTTP 429 under burst |
| Container build + Trivy CRITICAL | PASS | container-build workflow |
| SBOM | PASS | sbom workflow |
| Secret scan | PASS | Gitleaks CI |
| OWASP ZAP baseline | PASS (reviewed) | Artifact uploaded; **0 High**; 1 Medium (`style-src 'unsafe-inline'` for Angular Material — documented) |
| No committed secrets | PASS | placeholders only; CI masks bootstrap secrets |
| ASVS evidence | PARTIAL (truthful) | `docs/security/asvs-5-checklist.md` — many VERIFIED via live CI; residual gaps listed |
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
./infrastructure/scripts/spa-boot-verify.sh
cd frontend && PORTAL_E2E_REQUIRE_LIVE=1 npx playwright test e2e/live-portal.spec.ts
./infrastructure/scripts/rate-limit-verify.sh
```

## Cloud Agent limitation

This Cursor Cloud environment can run `docker info` but cannot create containers (overlay mount `invalid argument`). Live stack verification is executed in GitHub Actions via `fullstack-verify.yml`. Gates above marked PASS are backed by green Actions runs, not by containers on this agent host.
