# Architecture Overview

## Purpose

This portal is designed as a secure enterprise administration platform delivered as a **modular monolith**. The runtime is one Spring Boot backend process, one Angular frontend, PostgreSQL for durable state, Redis for sessions and short-lived security state, and observability integrations for metrics, traces, and logs.

The implementation is still in early foundation phases. This document describes the target architecture accepted by the ADRs and should be updated with implementation evidence as modules are built.

## Architectural style

- **Same-origin BFF**: the browser talks to the backend through the same public origin. The backend owns sessions and security decisions.
- **Modular monolith**: business capabilities are separated into Spring Modulith modules with explicit APIs and dependency rules.
- **Permission-based authorization**: endpoints and services check stable permission codes, not role names.
- **Persian-first UX**: `fa-IR` is the default language; RTL is the default direction until a user preference changes it.
- **Evidence-based security**: design documents distinguish planned controls from verified implementation.

## System context

```mermaid
C4Context
title Enterprise Portal - System Context

Person(admin, "Administrator", "Manages users, roles, settings, and security posture.")
Person(auditor, "Auditor", "Reviews immutable audit evidence and operational events.")
Person(user, "Portal User", "Uses enterprise portal features through a browser.")

System(portal, "Enterprise Portal", "Same-origin web portal with session BFF, RBAC, audit, notifications, and observability.")
System_Ext(mail, "SMTP / Mailpit", "Sends or captures account and notification email.")
System_Ext(otel, "OTLP Collector", "Receives traces, metrics, and structured telemetry.")
System_Ext(browserIdp, "Browser", "Stores only HttpOnly session cookies and non-secret CSRF cookie.")

Rel(admin, portal, "Administers", "HTTPS")
Rel(auditor, portal, "Reviews evidence", "HTTPS")
Rel(user, portal, "Uses", "HTTPS")
Rel(portal, mail, "Sends email", "SMTP")
Rel(portal, otel, "Exports telemetry", "OTLP/HTTP")
Rel(browserIdp, portal, "Same-origin requests", "HTTPS + cookies")
```

## Containers

```mermaid
C4Container
title Enterprise Portal - Containers

Person(person, "User", "Admin, auditor, support, or standard user")

System_Boundary(system, "Enterprise Portal") {
  Container(nginx, "nginx", "Reverse proxy", "Terminates public same-origin route, serves static Angular assets, proxies /api and actuator-safe paths.")
  Container(spa, "Angular SPA", "Angular 22 + Material", "Runtime i18n, RTL/LTR, themes, form UX.")
  Container(api, "Backend", "Spring Boot 4.1 / Java 25", "Session BFF, business modules, RBAC, audit, OpenAPI, observability.")
  ContainerDb(db, "PostgreSQL", "Relational database", "Users, roles, permissions, settings, audit hash chain, notifications.")
  ContainerDb(redis, "Redis", "Key-value store", "Spring Session, CSRF/session metadata, rate-limit counters.")
}

System_Ext(mail, "SMTP / Mailpit", "Email delivery or capture")
System_Ext(otel, "OTLP Collector", "Telemetry backend")

Rel(person, nginx, "Uses", "HTTPS")
Rel(nginx, spa, "Serves static files", "filesystem")
Rel(nginx, api, "Proxies /api", "HTTP")
Rel(api, db, "Reads/writes", "JDBC/Flyway")
Rel(api, redis, "Session and counters", "Lettuce")
Rel(api, mail, "Sends email", "SMTP")
Rel(api, otel, "Exports traces/metrics", "OTLP")
```

## Backend modules

```mermaid
flowchart LR
  bootstrap[bootstrap]
  shared[shared]
  identity[identity]
  access[accesscontrol]
  admin[administration]
  audit[audit]
  securityevent[securityevent]
  notification[notification]
  settings[settings]
  dashboard[dashboard]
  observability[observability]

  bootstrap --> identity
  bootstrap --> access
  identity --> shared
  identity --> audit
  identity --> securityevent
  identity --> notification
  identity --> settings
  access --> shared
  access --> audit
  admin --> identity
  admin --> access
  admin --> audit
  securityevent --> audit
  notification --> shared
  settings --> shared
  dashboard --> identity
  dashboard --> audit
  dashboard --> securityevent
  observability --> shared
  audit --> shared
```

## Key runtime flows

1. Browser loads Angular assets from nginx.
2. Angular calls backend APIs with same-origin credentials.
3. Backend authenticates using a Redis-backed opaque session cookie.
4. Mutating requests require a valid CSRF header matching the CSRF cookie/session token.
5. Authorization is evaluated against session authorities derived from database permissions.
6. Security-relevant state changes write append-only audit events and structured security events.
7. Metrics and traces are exported through Micrometer and OpenTelemetry.

## Main design constraints

- No JWT or bearer token is stored in browser storage.
- CSRF is mandatory for state-changing browser requests.
- Passwords use Argon2id through Spring Security and BouncyCastle.
- TOTP/MFA secrets are encrypted at rest with externally supplied keys.
- Public self-registration is disabled; the first administrator is bootstrapped once through environment variables.
- Administrative and security events must not log passwords, session identifiers, CSRF tokens, reset tokens, or MFA secrets.

## Current implementation evidence

| Area | Evidence | Status |
|------|----------|--------|
| Technology baseline | `docs/architecture/technology-baseline.md`, `backend/pom.xml`, `frontend/package.json` | Drafted |
| Session BFF decision | `docs/decisions/ADR-0002-session-bff-auth.md` | Accepted design |
| RBAC decision | `docs/decisions/ADR-0003-rbac-permissions.md` | Accepted design |
| Audit hash chain decision | `docs/decisions/ADR-0004-audit-hash-chain.md` | Accepted design |
| Backend modules | Spring Modulith dependency present | Implementation pending |
| Docker/nginx deployment | Design documented | Implementation pending |
