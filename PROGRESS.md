# Progress

## Current phase

Phase 3 (backend feature modules) — identity, authentication, MFA, RBAC,
audit, security events, notification, dashboard, administration modules are
implemented on a passing test suite.

## Environment

| Tool | Version / status |
|------|------------------|
| Git remote | `yasamanahmadi88/main-portal` |
| Branch | `cursor/complete-enterprise-portal` |
| Java | Temurin 25.0.3+9 |
| Maven | 3.9.10 |
| Node | 24.15.0 |
| npm | 11.12.1 |
| Docker | 29.1.3 (daemon started) |
| Angular scaffold | 22.0.5 created |
| Spring Boot | 4.1.0 (Java 25, Spring Security 7, Spring Modulith) |

## Completed

- [x] Inspect repository and remotes
- [x] Create feature branch `cursor/complete-enterprise-portal`
- [x] Install Java 25, Maven, Node 24
- [x] Scaffold backend (Spring Boot 4.1.0) and frontend (Angular 22)
- [x] Root `.gitignore`, `.editorconfig`, `.gitattributes`, `.env.example`, `AGENTS.md`
- [x] Technology baseline draft
- [x] Backend platform: `SecurityConfig`, `PortalProperties`, `PortalPermission`,
      `PortalRoles`, `ProblemDetailExceptionHandler`, `SecretEncryptionService`,
      `BootstrapAdminRunner`, Flyway V1–V5, `AbstractIntegrationTest`
- [x] **Identity module** — `UserEntity`, `UserPreferencesEntity`,
      `LoginAttemptEntity`, `PasswordResetTokenEntity`, `MfaCredentialEntity`,
      `MfaRecoveryCodeEntity`, `UserSessionMetadataEntity`, JPA repositories,
      `AuthenticationService` (session rotation, Redis rate limiting,
      progressive lockout, MFA challenge, security events), `MeService`,
      `CurrentUserService`, `PasswordService` (Argon2, hashed reset tokens),
      `SessionService`, `MfaService` (RFC 6238 TOTP, AES-GCM encrypted secret,
      hashed single-use recovery codes, clock skew tolerance),
      `PreferenceService`, `UserAdminService`, `AuthController`, `MeController`
- [x] **Access-control module** — `RoleEntity`, `PermissionEntity`,
      `UserRoleEntity`, `RolePermissionEntity`, `RoleService`,
      `PermissionService`, `RbacService` (Redis-cached effective permissions),
      `AuthorizationDecisionService` (SUPER_ADMIN protection, cannot
      self-escalate, cannot grant permissions caller doesn't hold, reserved
      roles), `RoleController`, `PermissionController`, method security
      wired via `@EnableMethodSecurity` + `hasAuthority`
- [x] **Audit module** — `AuditEventEntity` with hash-chain columns,
      `AuditEventChainEntity` with pessimistic locking, `AuditService.append`
      computing SHA-256 previous/current hashes over canonical fields, no
      update/delete after insert, `AuditIntegrityVerifier`, `AuditController`
      (list, get, export CSV/JSON, verify-integrity) gated by
      `audit:read` / `audit:export`
- [x] **Security-event module** — `SecurityEventEntity`, publisher API used by
      auth/RBAC flows, `SecurityEventController` for list + acknowledge
- [x] **Notification module** — `OutboxEventEntity`, `OutboxPublisher`
      (`@Scheduled`) with `JavaMailSender`, classpath email templates
      (`password-reset`, `password-changed`, `mfa-enabled`, `mfa-disabled`,
      `suspicious-login`, `invitation`) with EN/FA variants — mail dispatch is
      never done inside a DB transaction (outbox is written first, published
      by the scheduler)
- [x] **Administration module** — `UserAdminController` (search, create,
      status changes, unlock, MFA admin reset, role assign — no physical
      delete), `SettingsController`, `SessionAdminController`
- [x] **Settings module** — `SystemSettingEntity`, `SettingsService`
      (typed getters, patch)
- [x] **Dashboard module** — `DashboardQueryService` returning live counts,
      `DashboardController`
- [x] **SecurityConfig** — permit `/api/v1/auth/**` + `/api/v1/csrf`,
      authenticated for `/api/v1/**`, CSRF via `CookieCsrfTokenRepository`,
      method security, RFC 9457 Problem Details on 401/403 via
      `ProblemDetailAuthenticationEntryPoint` and
      `ProblemDetailAccessDeniedHandler`
- [x] **Modulith modules** — `@ApplicationModule` on each module,
      cross-module usage via `api` and `application` named interfaces,
      `CurrentUserAccessor` promoted to `shared.security` to avoid
      identity ⇄ accesscontrol / securityevent cycles
- [x] **Tests** (`./mvnw test` — 36 tests, 0 failures, 0 errors,
      1 skipped for Docker-only integration):
  - `PasswordHashingTest`, `PasswordServicePolicyTest`
  - `TotpCodeGeneratorTest` (RFC 6238 reference vectors), `Base32Test`
  - `AuditHashChainTest` (chain continuity + tamper detection)
  - `AuthorizationDecisionServiceTest` (RBAC escalation matrix)
  - `SecretEncryptionServiceTest` (AES-GCM roundtrip + non-determinism)
  - `ProblemDetailAuthErrorsTest` (401/403 CSRF flows)
  - `ModulithTest` (module boundaries verify green)
  - `ModuleArchitectureTest` (ArchUnit rules: controllers ↛ repositories,
    domain purity, DTO-only in web layer)

## Incomplete

- Architecture/security ADRs and diagrams
- Full frontend features
- Docker compose stack for the full application
- CI workflows
- Live end-to-end verification against Postgres + Redis containers

## Exact next action

Move to Phase 4 — frontend feature modules (auth, users, roles, dashboard),
then Docker Compose + CI wiring.
