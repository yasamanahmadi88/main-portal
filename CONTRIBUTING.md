# Contributing

## Development principles

- Keep the modular monolith boundaries explicit.
- Prefer Spring, Angular, and repository conventions over custom infrastructure.
- Treat security documentation as part of the deliverable.
- Mark controls as verified only with code and test evidence.

## Setup

Backend:

```bash
cd backend
./mvnw test
```

Frontend:

```bash
cd frontend
npm install
npm test
```

Some tests may require PostgreSQL, Redis, or Docker/Testcontainers once those suites are implemented.

## Branch and commit guidance

- Use focused commits for logical changes.
- Do not commit generated build outputs, local `.env`, secrets, or dependency caches.
- Keep documentation and tests in the same change when behavior changes.

## Code standards

- Backend: Java 25, Spring Boot, Spring Security, Spring Modulith.
- Frontend: Angular 22, Angular Material, runtime i18n.
- Default locale: `fa-IR`; default direction: RTL.
- Authorization: permission checks, not role-name checks.
- Logs: structured, redacted, no secrets.

## Required checks for security-sensitive changes

Before requesting review for authentication, authorization, session, crypto, audit, logging, or deployment changes:

- Add or update integration tests.
- Add RBAC matrix tests for new permissions.
- Update `docs/security/asvs-5-checklist.md` evidence.
- Update `QUALITY-GATES.md` only when evidence exists.
- Review `docs/security/secure-coding-standard.md`.

## Documentation style

- Be concise and specific.
- Distinguish design intent from implemented behavior.
- Link to evidence rather than claiming completion.
- Use Mermaid diagrams where they clarify architecture or flow.
