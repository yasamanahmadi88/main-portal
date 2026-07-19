# Enterprise Portal

Secure enterprise portal built as a modular monolith: Spring Boot 4 / Java 25 API and Angular 22 SPA behind Nginx (same-origin `/` + `/api/`).

Session authentication (Redis), CSRF, Argon2id passwords, TOTP MFA, permission RBAC, tamper-evident audit, bilingual Persian-first UI, and Docker Compose are implemented and verified through GitHub Actions `fullstack-verify`. Status and residual gaps: `PROGRESS.md`, `QUALITY-GATES.md`, and `docs/security/asvs-5-checklist.md`.

Compared with [secure-portal](https://github.com/yasamanahmadi88/secure-portal): this repository is the advanced consolidation — see `docs/comparison/secure-portal-vs-main-portal.md`.

## Technology baseline

| Area | Stack |
|------|-------|
| Backend | Java 25, Spring Boot 4.1, Spring Security, Spring Modulith |
| Frontend | Angular 22, Angular Material 22, ngx-translate |
| Data | PostgreSQL, Flyway |
| Sessions/cache | Redis, Spring Session |
| Observability | Spring Boot Actuator, Micrometer Prometheus, OpenTelemetry |
| Security | Same-origin session BFF, CSRF, Argon2id, permission RBAC, audit hash chain |

Persian (`fa-IR`) is the default language and RTL is the default direction for first-time users.

## Repository layout

```text
backend/      Spring Boot backend scaffold
frontend/     Angular frontend scaffold
docs/         Architecture, security, testing, operations, and ADRs
```

## Local startup (Docker-first)

**Recommended path — Git + Docker + Docker Compose only.** No host Java, Node, npm, PostgreSQL, or Redis install is required to run the portal.

```bash
cp .env.example .env
# edit .env — set MFA_ENCRYPTION_KEY_BASE64 (openssl rand -base64 32),
# database passwords, GRAFANA_ADMIN_PASSWORD and Redis password.
# For Playwright / verify scripts against a local stack, set CAPTCHA_REVEAL_ANSWER=true
# (never in production — the prod profile refuses to start if reveal is enabled).

docker compose -f compose.yaml config -q       # validate
docker compose -f compose.yaml up -d --build   # build + start core stack

# Optional — observability overlay (Grafana, Prometheus, Loki, Tempo, OTel):
docker compose -f compose.yaml -f compose.observability.yaml up -d --build

# Poll health across the stack
infrastructure/scripts/wait-for-healthy.sh -v postgres redis backend frontend mailpit
```

The portal is then reachable at `http://localhost:8080` (Mailpit UI at
`:8025`, Grafana at `:3000` when the observability overlay is up).
Monitoring inside the SPA: `/monitoring` (requires `MONITORING_READ`).

Optional host-port overlays for Postgres/Redis/Mailpit:

```bash
docker compose -f compose.yaml -f compose.dev.yaml --env-file .env up -d --build
```

See `docs/operations/deployment.md` for production requirements
(TLS termination, secret management, image provenance, backups) and
`docs/operations/runbook.md` for day-to-day operational notes.

### Optional: native toolchain (contributors)

For IDE/unit-test iteration without containers:

- Java 25 + Maven wrapper (`backend/`)
- Node.js compatible with Angular 22 + npm (`frontend/`)
- PostgreSQL and Redis (or compose services with published ports)

## Architecture summary

The target deployment is a same-origin BFF:

1. nginx serves Angular static assets.
2. nginx proxies `/api/**` to Spring Boot.
3. Spring Security authenticates opaque Redis-backed sessions.
4. Angular sends CSRF headers for unsafe requests.
5. Backend modules enforce permission-based authorization.
6. Privileged actions write tamper-evident audit events.

Key documents:

- `docs/architecture/overview.md`
- `docs/architecture/modules.md`
- `docs/architecture/data-model.md`
- `docs/architecture/deployment.md`
- `docs/architecture/sequences.md`

## Security documentation

- `docs/security/threat-model.md`
- `docs/security/security-architecture.md`
- `docs/security/authentication-design.md`
- `docs/security/session-design.md`
- `docs/security/authorization-design.md`
- `docs/security/rbac-design.md`
- `docs/security/authorization-matrix.md`
- `docs/security/asvs-5-checklist.md`
- `SECURITY.md`

## Verification status

Do not infer production readiness from design documents alone. Quality gates and residual gaps are tracked in:

- `QUALITY-GATES.md`
- `PROGRESS.md`
- `docs/audit/enterprise-gap-analysis.md`
- `docs/security/asvs-5-checklist.md`
- `docs/risk-register.md`

Core session/CSRF/RBAC/audit/CAPTCHA/i18n/theme controls are implemented; live compose evidence is produced by GitHub Actions `fullstack-verify`. External pen-test and production TLS/WAF remain required before production.

## Contributing

Read `CONTRIBUTING.md` and `AGENTS.md` before making changes. Security-sensitive changes must update the relevant documentation and tests.
