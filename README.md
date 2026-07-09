# Enterprise Portal

Secure enterprise portal foundation built as a modular monolith with a Spring Boot backend and Angular frontend.

The repository is in an early foundation phase. Architecture and security decisions are documented, while many runtime controls and domain modules are still pending implementation and verification. Status should be read from `PROGRESS.md` and `QUALITY-GATES.md`.

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

## Local startup

Prerequisites:

- Java 25
- Maven wrapper from `backend/`
- Node.js compatible with Angular 22
- npm
- PostgreSQL and Redis configured according to `.env.example`

Backend:

```bash
cd backend
./mvnw spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm start
```

Docker Compose and nginx deployment files are not yet committed. See `docs/operations/runbook.md` for operational notes.

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

Do not infer production readiness from design documents alone. Quality gates are tracked in:

- `QUALITY-GATES.md`
- `docs/security/asvs-5-checklist.md`
- `docs/risk-register.md`

Most security controls are currently documented as design targets and remain **NOT VERIFIED** until implementation and tests are added.

## Contributing

Read `CONTRIBUTING.md` and `AGENTS.md` before making changes. Security-sensitive changes must update the relevant documentation and tests.
