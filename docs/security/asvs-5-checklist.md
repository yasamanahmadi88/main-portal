# ASVS 5 Checklist

Evidence tracker for OWASP ASVS 5 Level 2 themes relevant to this portal. Status values:

- `VERIFIED` — executable test or inspection evidence exists (unit, integration, or live CI).
- `PARTIALLY VERIFIED` — control implemented and partially evidenced; remaining gaps listed.
- `NOT VERIFIED` — no executable evidence yet.
- `NOT APPLICABLE` — out of scope for this application layer (rationale required).

| Requirement ID | Summary | Applicability | Implementation evidence | Test / command evidence | Status | Limitation | Accepted risk |
|----------------|---------|---------------|-------------------------|-------------------------|--------|------------|---------------|
| V2.1 | Server-side session authentication (no browser JWT) | Applicable | `SecurityConfig`, Spring Session Redis, ADR-0002 | `LiveSecurityIntegrationTest`, `live-api-verify.sh`, Playwright storage checks | PARTIALLY VERIFIED | Live compose evidence via `fullstack-verify` workflow | None |
| V2.2 | Passwords stored with Argon2id | Applicable | `SecurityConfig` Argon2 encoder, `BootstrapAdminRunner` | `PasswordHashingTest`, `bootstrap-verify.sh` Argon2 check | VERIFIED | Parameter tuning for production hardware pending | None |
| V2.3 | Auth errors do not disclose account existence | Applicable | `AuthenticationService` generic failures | `live-api-verify.sh`, Playwright failed-login, `LiveSecurityIntegrationTest` | VERIFIED | — | None |
| V2.4 | Password reset tokens random, short-lived, hashed, one-time | Applicable | `PasswordService`, `PasswordResetTokenEntity` | Unit coverage + forgot-password generic HTTP in `live-api-verify.sh` | PARTIALLY VERIFIED | Full token replay/expiry live path needs Mailpit assertion in CI | None |
| V2.5 | MFA secrets protected at rest | Applicable | `SecretEncryptionService`, MFA key ring in `application.yml` | Unit crypto tests; compose requires `MFA_ENCRYPTION_KEY_*` | PARTIALLY VERIFIED | Live TOTP enroll/challenge e2e not yet green in fullstack job | None |
| V2.6 | Rate limiting on login/reset/MFA | Applicable | `RateLimiter` Redis fixed window | Code + unit path; live burst test pending in fullstack | PARTIALLY VERIFIED | Explicit burst assertion not yet in shell scripts | None |
| V3.1 | Session IDs never exposed to JavaScript | Applicable | HttpOnly session cookie | `live-api-verify.sh` Set-Cookie HttpOnly, Playwright cookie check | VERIFIED | Secure flag requires TLS/`prod` profile | None |
| V3.2 | Session ID rotates after login | Applicable | Spring Security `migrateSession` | `SecurityConfig.configureSessions` | PARTIALLY VERIFIED | Explicit before/after session-id compare in live script pending | None |
| V3.3 | Session timeout / invalidation | Applicable | idle/absolute timeouts in `PortalProperties` | Logout invalidation in `live-api-verify.sh` | PARTIALLY VERIFIED | Idle timeout soak not automated | None |
| V3.4 | CSRF protects unsafe browser requests | Applicable | `CookieCsrfTokenRepository` | CSRF 403 checks in live API + Playwright + MockMvc | VERIFIED | — | None |
| V3.5 | Logout invalidates server-side session | Applicable | Auth logout + Redis session | `live-api-verify.sh` post-logout `/me` | VERIFIED | — | None |
| V4.1 | Access control deny-by-default | Applicable | `SecurityConfig.anyRequest().authenticated()` + method security | `rbac-matrix-verify.sh` anonymous denials | VERIFIED | — | None |
| V4.2 | Privileged functions require permissions | Applicable | `@PreAuthorize` on admin/audit/RBAC controllers | `rbac-matrix-verify.sh`, permission matrix GET | VERIFIED | — | None |
| V4.3 | Horizontal access control / IDOR prevention | Applicable | Object checks in services + authz | Anonymous/IDOR baselines in RBAC script | PARTIALLY VERIFIED | Broader cross-user object matrix still expanding | None |
| V4.4 | Privilege escalation paths tested | Applicable | `AuthorizationDecisionService` | Unit tests + final SUPER_ADMIN demotion/disable blocks in live RBAC | VERIFIED | External pen-test still required for residual paths | None |
| V4.5 | Final active SUPER_ADMIN protected | Applicable | `checkCanAssignRoles` / `checkCanDisableOrDeleteUser` | Unit + `rbac-matrix-verify.sh` | VERIFIED | — | None |
| V7.1 | Logs/audit do not contain secrets | Applicable | `LogSanitizer`, audit payload redaction | `bootstrap-verify.sh`, `audit-db-verify.sh`, `observability-verify.sh` | PARTIALLY VERIFIED | Continuous log-injection fuzzing not automated | None |
| V7.2 | Security-relevant events audited | Applicable | `AuditService` call sites + bootstrap audit | Login/RBAC/bootstrap events; integrity verify API | PARTIALLY VERIFIED | Full event catalog coverage matrix still expanding | None |
| V7.3 | Audit integrity protected | Applicable | Hash chain + Flyway V7 append-only grants | `audit-db-verify.sh`, `/audit-events/verify-integrity` | VERIFIED | Tamper detection of broken chain links covered by verifier unit + API | None |
| V7.4 | Logs include correlation identifiers | Applicable | `CorrelationIdFilter`, MDC | `observability-verify.sh` | PARTIALLY VERIFIED | Trace/span correlation with Tempo needs observability compose profile | None |
| V8.1 | Sensitive data classified | Applicable | `docs/security/data-classification.md` | Document review | PARTIALLY VERIFIED | Inventory refresh with new entities pending | None |
| V8.2 | Sensitive data encrypted where required | Applicable | MFA secret encryption | Crypto unit tests | PARTIALLY VERIFIED | KMS/HSM integration is deployment concern | None |
| V8.3 | Secrets not committed | Applicable | `.env.example`, `.gitignore`, Gitleaks CI | Gitleaks job green on PR | VERIFIED | — | None |
| V8.4 | Personal data retention defined | Applicable | Data model retention notes | Policy docs | PARTIALLY VERIFIED | Organizational approval pending | None |
| V8.5 | Backups protect audit data | Applicable | Operations runbook | Environment-specific | NOT APPLICABLE | Infrastructure/process control outside app repo; see pen-test plan | None |
| V14 | TLS / WAF / network controls | Applicable (deploy) | nginx headers locally; TLS at ingress | Security headers in `live-api-verify.sh` | PARTIALLY VERIFIED | Production TLS/WAF require environment pen-test | See `accepted-risks.md` candidates |

## Evidence rules

- Mark `VERIFIED` only with linked tests, code references, CI artifacts, or inspected live outputs.
- Do not mark `VERIFIED` from code presence alone.
- Use `NOT APPLICABLE` only with a short rationale.
- Accepted risks require owner, expiry, compensating controls, and an entry in `accepted-risks.md`.

## CI evidence channels

| Channel | Workflow / artifact |
|---------|---------------------|
| Unit/integration | `.github/workflows/ci.yml` |
| Containers + Trivy | `.github/workflows/container.yml` |
| SBOM | `.github/workflows/sbom.yml` |
| Live stack (compose, Playwright, Axe, ZAP, RBAC, audit) | `.github/workflows/fullstack-verify.yml` → artifact `live-stack-evidence` |
