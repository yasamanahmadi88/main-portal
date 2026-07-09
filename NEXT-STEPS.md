# Next Steps

Ordered delivery phases after `cursor/portal-foundation`. Each phase gets its own branch (`cursor/<phase-name>`), commit(s), and PR targeting `main`.

## Phase 1 — API contracts & shared types

- OpenAPI document for `/api/**`
- Shared TypeScript client generation or hand-maintained DTO package
- Consistent error envelope

## Phase 2 — Persistence

- PostgreSQL schema + Flyway/Liquibase migrations
- Spring Data repositories for core portal entities
- Local compose file once Docker is available in the environment

## Phase 3 — Authentication & sessions

- Login/logout, session or JWT strategy
- Password hashing, CSRF/CORS hardening
- `.env.example` extended with auth placeholders only

## Phase 4 — Portal shell UX

- Authenticated layout, navigation, protected routes
- Design tokens aligned with brand rules
- Accessibility baseline (focus, landmarks, contrast)

## Phase 5 — Domain modules

- User/profile management
- Content or dashboard modules as product requirements land
- Role-based access control

## Phase 6 — Observability & CI

- Structured logging, metrics, tracing hooks
- GitHub Actions: build, test, lint, basic security scans
- Dockerfiles + compose when the cloud image includes Docker

## Phase 7 — Hardening

- Rate limiting, input validation audit
- Dependency scanning (npm audit, OWASP Dependency-Check)
- Backup/restore runbooks

## Explicit non-goals for foundation

- Full product feature set in one commit
- Real secrets or production infrastructure
- Claiming Java 25 / Docker support without installing them
