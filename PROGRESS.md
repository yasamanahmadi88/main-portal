# Progress

## Current phase

Phase 4 (frontend) — Angular 22 portal SPA is implemented end-to-end:
core services (auth, csrf, i18n, theme, error handling, http interceptors),
guards, shared UI kit, feature modules (auth flows, dashboard, profile,
users, roles, permissions, audit, security events, settings), design
tokens with self-hosted Vazirmatn and CSS-variable theming, Vitest unit
suite (39 passing), Playwright + axe smoke suite, and ESLint 9 flat
config all in place. `npm run build`, `npm test`, and `npm run lint` are
green.

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
- Docker compose stack for the full application
- CI workflows
- Live end-to-end verification against Postgres + Redis containers

## Frontend (Phase 4)

- [x] Angular 22.0.5 scaffold wired for cookie-session auth + `X-XSRF-TOKEN`
      CSRF (double-submit)
- [x] `core/` layout — `authentication/`, `authorization/`,
      `configuration/`, `error-handling/`, `http/` (interceptors +
      per-feature API clients), `i18n/`, `observability/`, `routing/`,
      `session/`, `shell/`
- [x] `AuthService` with signals; no tokens in storage; only
      language/theme persisted for anonymous users
- [x] Guards — `authGuard`, `guestGuard`, `permissionGuard`,
      `mfaChallengeGuard`
- [x] Error interceptor handling 401/403/409/422/429/5xx with RFC 9457
      Problem-Details normalisation and toasts
- [x] `I18nService` (default `fa-IR`, runtime switch `en-US`, sets
      `<html lang>` + `<html dir>`, CDK Directionality change emitter)
- [x] `ThemeService` (LIGHT/DARK/SYSTEM, no-flash inline bootstrap in
      `index.html`, `prefers-color-scheme` listener)
- [x] Preference sync API when authenticated (`PreferencesSyncService`)
- [x] App shell with sidenav, topbar, language/theme/user menus and
      notification-panel placeholder
- [x] Design-token SCSS (`_tokens.scss`, `_fonts.scss`,
      `_utilities.scss`) with logical CSS properties
- [x] Self-hosted **Vazirmatn** OFL (5 weights) with a curated fallback
      stack — **no remote CDN at runtime**
- [x] Auth UI — login (two-column desktop, form-first mobile), forgot
      password, reset password, MFA challenge, access denied, session
      expired, maintenance, 404, all with language + theme controls
- [x] Portal features (functional, wired to API clients):
      dashboard, profile (edit / password / MFA / sessions), users
      admin (list / detail / create / edit / role assign / actions),
      roles (list / detail / create / edit) + RTL-aware permission
      matrix, permissions list, audit list/detail with integrity
      verification, security-events list with acknowledge, settings
- [x] Shared UI: button, input, password-input (show/hide + CapsLock),
      select, table, pagination, dialog/confirm, snackbar wrapper,
      skeleton, empty-state, error-state, page-header, breadcrumb,
      status-badge, permission-matrix
- [x] i18n JSON bundles for `common`, `validation`, `navigation`,
      `authentication`, `dashboard`, `profile`, `users`, `roles`,
      `permissions`, `audit`, `security-events`, `settings` in `fa-IR`
      and `en-US` — parity checked by a Vitest test
- [x] Unit tests (Vitest, 39 passing):
      `CsrfService` + `csrfInterceptor`, `AuthService`, `guards`,
      `I18nService` (lang/dir), `ThemeService`, and translation parity
- [x] Playwright + `@axe-core/playwright` smoke suite:
      login renders, language toggle, theme toggle, axe scan; suite
      self-skips when the dev server isn't running
- [x] ESLint 9 flat config (`eslint.config.mjs`) with
      `angular-eslint` + `typescript-eslint`; `npm run lint` passes
      (warnings only)
- [x] `proxy.conf.json` — `ng serve` proxies `/api` to
      `http://localhost:8080` with cookie domain/path rewrites
- [x] `package.json` scripts: `start`, `build`, `build:dev`, `watch`,
      `test`, `test:watch`, `lint`, `lint:fix`, `format`, `e2e`,
      `e2e:install`
- [x] `npm run build` → success (main 680 kB, ~156 kB gzip)
- [x] `npm test` → 39/39 passing
- [x] `npm run lint` → 0 errors, 6 non-blocking warnings

## Exact next action

Move to Phase 5 — Docker Compose (backend + Postgres + Redis + frontend
static server) and CI workflows for lint/test/build.
