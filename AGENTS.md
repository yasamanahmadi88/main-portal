# Enterprise Portal — Agent Instructions

This repository is a production-grade modular-monolith enterprise portal.

## Stack (do not invent versions)

See `docs/architecture/technology-baseline.md`.

- Backend: Java 25, Spring Boot 4.1.0, Maven Wrapper
- Frontend: Angular 22.0.x, TypeScript ~6, Node ^22.22.3 || ^24.15.0 || >=26
- Data: PostgreSQL + Flyway, Redis (Spring Session)
- Auth: Server-side sessions + CSRF (never JWT in browser storage)
- i18n: `@ngx-translate` runtime switching (`fa-IR` default, `en-US`)
- Themes: LIGHT / DARK / SYSTEM

## Layout

- `backend/` — Spring Boot modular monolith (`com.company.portal.*`)
- `frontend/` — Angular 22 standalone app
- `contracts/openapi/` — API source of truth
- `infrastructure/` — Docker, nginx, observability, scripts
- `docs/` — architecture, security, operations, ADRs

## Non-negotiables

1. Never disable CSRF. Never store auth tokens in localStorage/sessionStorage.
2. Never return JPA entities from controllers. Never commit secrets.
3. Deny-by-default RBAC with permission-based `hasAuthority(...)` checks.
4. Audit trail is append-only and hash-chained; runtime DB role cannot mutate it.
5. Controllers have no business logic; modules interact via APIs/events only.
6. Keep the repo buildable after each coherent change.
7. Update `PROGRESS.md`, `NEXT-STEPS.md`, and `QUALITY-GATES.md` continuously.

## Commands

```bash
# Backend
cd backend && ./mvnw clean verify

# Frontend
cd frontend && npm ci && npm run lint && npm run test && npm run build

# Stack
docker compose config && docker compose up -d
```

## Branch / PR

Work on `cursor/complete-enterprise-portal`. Target PR base: `main`.
Use Conventional Commits. Do not force-push. Do not push incomplete work to `main`.
