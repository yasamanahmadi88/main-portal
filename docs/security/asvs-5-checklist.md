# ASVS 5 Checklist

Evidence tip commit: `41eac10053e749b6dde0a305ee9fc2b1f9900e80`  
Executed live command channel: `.github/workflows/fullstack-verify.yml` run [29050258913](https://github.com/yasamanahmadi88/main-portal/actions/runs/29050258913) (artifact `live-stack-evidence`).

Status values:

- `VERIFIED` — executable test or inspection evidence exists (unit, integration, or live CI).
- `PARTIALLY VERIFIED` — control implemented and partially evidenced; remaining gaps listed.
- `NOT VERIFIED` — no executable evidence yet.
- `NOT APPLICABLE` — out of scope for this application layer (rationale required).

| Requirement ID | Summary | Applicability | Implementation evidence | Test / command evidence | Status | Limitation | Accepted risk |
|----------------|---------|---------------|-------------------------|-------------------------|--------|------------|---------------|
| V2.1 | Server-side session authentication (no browser JWT) | Applicable | `SecurityConfig`, Spring Session Redis, ADR-0002 | `live-api-verify.sh` (PORTAL_SESSION, no JWT in JSON); Playwright storage checks | VERIFIED | Secure cookie flag requires TLS/`prod` | None |
| V2.2 | Passwords stored with Argon2id | Applicable | Argon2 encoder, `BootstrapAdminRunner` | `PasswordHashingTest`; `bootstrap-verify.sh` Argon2 hash check | VERIFIED | Production parameter tuning pending | None |
| V2.3 | Auth errors do not disclose account existence | Applicable | Generic auth failures | `live-api-verify.sh`; Playwright failed-login | VERIFIED | — | None |
| V2.4 | Password reset tokens random, short-lived, hashed, one-time | Applicable | `PasswordService`, reset token entity | Forgot-password generic HTTP 202 in live API | PARTIALLY VERIFIED | Mailpit token replay/expiry assertion not in CI | None |
| V2.5 | MFA secrets protected at rest | Applicable | `SecretEncryptionService`, MFA key ring | Unit crypto; compose requires `MFA_ENCRYPTION_KEY_*` | PARTIALLY VERIFIED | Live TOTP enroll/challenge e2e suite not yet dedicated | None |
| V2.6 | Rate limiting on login/reset/MFA | Applicable | Redis `RateLimiter` + nginx login zone | `rate-limit-verify.sh` → HTTP 429 under burst | VERIFIED | MFA/reset burst not separately asserted | None |
| V3.1 | Session IDs never exposed to JavaScript | Applicable | HttpOnly session cookie | Live Set-Cookie HttpOnly; Playwright cookie check | VERIFIED | Secure flag requires TLS | None |
| V3.2 | Session ID rotates after login | Applicable | Spring Security `migrateSession` | Code path in `SecurityConfig` | PARTIALLY VERIFIED | Explicit before/after cookie compare not in shell | None |
| V3.3 | Session timeout / invalidation | Applicable | Idle/absolute timeouts | Logout invalidation in `live-api-verify.sh` | PARTIALLY VERIFIED | Idle timeout soak not automated | None |
| V3.4 | CSRF protects unsafe browser requests | Applicable | `CookieCsrfTokenRepository` | Live API + Playwright CSRF 403 | VERIFIED | — | None |
| V3.5 | Logout invalidates server-side session | Applicable | Auth logout + Redis session | Post-logout `/me` 401 in live API | VERIFIED | — | None |
| V4.1 | Access control deny-by-default | Applicable | Authenticated anyRequest + method security | `rbac-matrix-verify.sh` anonymous denials | VERIFIED | — | None |
| V4.2 | Privileged functions require permissions | Applicable | `@PreAuthorize` admin/audit/RBAC | Live RBAC matrix + permission matrix GET | VERIFIED | — | None |
| V4.3 | Horizontal access control / IDOR prevention | Applicable | Object checks + authz | Anonymous IDOR baselines in RBAC script | PARTIALLY VERIFIED | Broader cross-user object matrix still expanding | None |
| V4.4 | Privilege escalation paths tested | Applicable | `AuthorizationDecisionService` | Unit + live SUPER_ADMIN demotion/disable blocks | VERIFIED | External pen-test still required | None |
| V4.5 | Final active SUPER_ADMIN protected | Applicable | Role/disable guards | Unit + `rbac-matrix-verify.sh` | VERIFIED | — | None |
| V7.1 | Logs/audit do not contain secrets | Applicable | `LogSanitizer`, audit redaction | bootstrap/audit/observability scripts | PARTIALLY VERIFIED | Continuous log-injection fuzzing not automated | None |
| V7.2 | Security-relevant events audited | Applicable | `AuditService` call sites | Bootstrap/RBAC/login traffic; integrity API | PARTIALLY VERIFIED | Full event catalog matrix still expanding | None |
| V7.3 | Audit integrity protected | Applicable | Hash chain + Flyway append-only grants | `audit-db-verify.sh` + `/audit-events/verify-integrity` | VERIFIED | — | None |
| V7.4 | Logs include correlation identifiers | Applicable | `CorrelationIdFilter`, MDC | `observability-verify.sh` | PARTIALLY VERIFIED | Tempo/Loki full stack needs observability overlay | None |
| V8.1 | Sensitive data classified | Applicable | `docs/security/data-classification.md` | Document review | PARTIALLY VERIFIED | Inventory refresh pending | None |
| V8.2 | Sensitive data encrypted where required | Applicable | MFA secret encryption | Crypto unit tests | PARTIALLY VERIFIED | KMS/HSM is deployment concern | None |
| V8.3 | Secrets not committed | Applicable | `.env.example`, Gitleaks | Gitleaks CI green; Actions `::add-mask::` | VERIFIED | — | None |
| V8.4 | Personal data retention defined | Applicable | Retention notes | Policy docs | PARTIALLY VERIFIED | Organizational approval pending | None |
| V8.5 | Backups protect audit data | Applicable | Operations runbook | Environment-specific | NOT APPLICABLE | Outside app repo; see pen-test plan | None |
| V14 | TLS / WAF / network controls | Applicable (deploy) | nginx headers; TLS at ingress | Security headers in live API; ZAP baseline | PARTIALLY VERIFIED | Production TLS/WAF require environment pen-test | Candidate in `accepted-risks.md` |

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
| Live stack (compose, Playwright, Axe, ZAP, RBAC, audit) | `.github/workflows/fullstack-verify.yml` → `live-stack-evidence`, `zap-baseline-report` |
