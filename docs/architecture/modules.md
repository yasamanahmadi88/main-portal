# Backend Module Boundaries

## Boundary principles

- Modules expose behavior through package-level APIs or application services, not through direct repository access.
- Authorization is enforced at entry points and sensitive application services.
- Database tables are owned by one module; cross-module reads go through APIs or documented projections.
- Shared code contains primitives and cross-cutting utilities only; it must not become a domain dumping ground.
- Module boundaries should be verified with Spring Modulith and ArchUnit once implementations exist.

## Module catalog

| Module | Responsibility | Owns data | Public capabilities | May depend on |
|--------|----------------|-----------|---------------------|---------------|
| `identity` | Users, credentials, login, logout, password reset, MFA enrollment, profile preferences | users, credentials, sessions metadata, password reset tokens, MFA secrets/recovery codes | Authenticate principal, manage own profile, expose user identity summary | `shared`, `settings`, `audit`, `securityevent`, `notification` |
| `accesscontrol` | Roles, permissions, grants, authorization policies, privilege refresh | roles, permissions, user-role links, role-permission links | Resolve authorities, manage RBAC, protect final `SUPER_ADMIN` | `shared`, `audit` |
| `administration` | Admin workflows across identity and RBAC | workflow records if needed | User lifecycle, role assignment, lock/unlock, support actions | `identity`, `accesscontrol`, `audit`, `notification`, `shared` |
| `audit` | Append-only audit events and integrity verification | audit_events, audit_integrity_checkpoints | Record events, verify hash chain, export evidence | `shared` |
| `securityevent` | Security detections and account risk events | security_events, rate_limit_events | Record failed login, lockout, MFA failures, suspicious activity | `audit`, `shared`, `notification` |
| `notification` | Email and in-app notifications | notification_templates, notification_outbox, notification_deliveries | Send password reset, MFA, account, and operational messages | `shared` |
| `settings` | Runtime configuration, localization, theme defaults, user preferences | settings, user_preferences | Read effective setting, manage allowed mutable settings | `shared`, `audit` |
| `dashboard` | Aggregated portal landing data | read models/cache only | Display summaries and metrics from other modules | `identity`, `audit`, `securityevent`, `shared` |
| `observability` | Metrics, traces, health contributors, structured logging helpers | no business tables | Expose meters, span attributes, health checks | `shared` |
| `shared` | Value objects, error model, clock, identifiers, validation helpers | reference data only if unavoidable | Common primitives and contracts | none |
| `bootstrap` | One-time initialization and seed data | bootstrap run records | Create first admin, seed system roles/permissions | `identity`, `accesscontrol`, `settings`, `audit` |

## Module rules

### `identity`

- Must not modify roles or permissions directly.
- Must publish or call an access-control refresh when account authorities change.
- Must rotate the session after login and after material credential changes.
- Must not expose password hash, reset token, TOTP secret, recovery code hash, or session ID in APIs or logs.

### `accesscontrol`

- Owns the permission code namespace and role-permission assignments.
- Enforces deny-by-default authorization.
- Protects reserved roles from deletion or unsafe mutation.
- Prevents removing, disabling, or demoting the final active `SUPER_ADMIN`.

### `administration`

- Coordinates administrative use cases but does not own credentials or RBAC schema.
- Must call `identity` for user state changes and `accesscontrol` for grants.
- Must create audit events for all privileged operations.

### `audit`

- Audit events are append-only.
- Runtime database roles must be denied `UPDATE` and `DELETE` on `audit_events`.
- Critical security and administrative operations fail closed if audit persistence fails.
- Integrity verification reports the first broken link, affected range, and verification timestamp.

### `securityevent`

- Captures security signals separate from general application logs.
- May trigger notification or account lockout workflows.
- Must sanitize request metadata and never persist secrets.

### `notification`

- Uses an outbox pattern for reliable delivery.
- Templates support `fa-IR` as the default locale and may include English alternatives.
- Messages must avoid sensitive tokens in logs; reset URLs are redacted.

### `settings`

- Distinguishes system defaults, tenant/global settings, and user preferences.
- Persian (`fa-IR`) and RTL are the initial defaults.
- Security-critical settings require audit events and elevated permissions.

### `dashboard`

- Provides read-only aggregation.
- Must not bypass authorization by aggregating data the caller cannot otherwise access.

### `observability`

- Provides standardized meter names, trace attributes, and log enrichment.
- Must not depend on business modules in ways that create cycles.

### `shared`

- Allowed content: value objects, exception types, problem details, pagination, clock abstraction, validation annotations, module event contracts.
- Disallowed content: user management, RBAC decisions, audit persistence, notification delivery, feature-specific repositories.

### `bootstrap`

- Runs idempotently and records completion.
- Consumes `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD`, and `BOOTSTRAP_ADMIN_DISPLAY_NAME` once.
- Clears or ignores bootstrap credentials after successful initialization.

## Dependency matrix

| From \ To | shared | identity | accesscontrol | administration | audit | securityevent | notification | settings | dashboard | observability | bootstrap |
|-----------|--------|----------|---------------|----------------|-------|---------------|--------------|----------|-----------|---------------|-----------|
| identity | yes | - | via API only | no | yes | yes | yes | yes | no | no | no |
| accesscontrol | yes | no | - | no | yes | no | no | no | no | no | no |
| administration | yes | yes | yes | - | yes | no | yes | no | no | no | no |
| audit | yes | no | no | no | - | no | no | no | no | no | no |
| securityevent | yes | no | no | no | yes | - | yes | no | no | no | no |
| notification | yes | no | no | no | no | no | - | no | no | no | no |
| settings | yes | no | no | no | yes | no | no | - | no | no | no |
| dashboard | yes | read API | read API | no | read API | read API | no | no | - | no | no |
| observability | yes | no | no | no | no | no | no | no | no | - | no |
| bootstrap | yes | yes | yes | no | yes | no | no | yes | no | no | - |

## Boundary verification to add

- Spring Modulith application module tests.
- ArchUnit rules for forbidden package dependencies.
- Repository visibility checks: no module reads another module's JPA repository directly.
- Public API tests for RBAC refresh, audit append, and bootstrap idempotency.
