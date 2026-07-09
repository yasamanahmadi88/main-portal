# Quality Gates

Status legend: `PASS` | `FAIL` | `NOT VERIFIED` | `IN PROGRESS`

| Gate | Status | Evidence |
|------|--------|----------|
| Correct repository structure | PASS | `com.company.portal.{identity,accesscontrol,administration,audit,securityevent,notification,dashboard,settings,observability,shared,bootstrap}` |
| Java 25 backend compilation | PASS | `./mvnw -o -DskipTests compile` → BUILD SUCCESS; 149 source files |
| Angular 22 frontend compilation | NOT VERIFIED | — |
| Production builds | NOT VERIFIED | — |
| Unit / integration / architecture tests | PASS | `./mvnw -o test` → 36 tests, 0 failures, 0 errors, 1 skipped (Docker-only IT) |
| E2E / accessibility tests | NOT VERIFIED | — |
| Flyway on PostgreSQL | IN PROGRESS | V1–V5 migrations present; entities mapped to `validate` |
| Redis sessions / CSRF / login / logout | IN PROGRESS | `SecurityConfig` wires `CookieCsrfTokenRepository`, session-fixation `changeSessionId`; unit-tested via `ProblemDetailAuthErrorsTest`. Full IT depends on Docker. |
| MFA / recovery codes | PASS | RFC 6238 TOTP verified against reference vectors; AES-GCM encrypted secret; hashed single-use recovery codes |
| Complete RBAC + escalation protections | PASS | `AuthorizationDecisionService` blocks SUPER_ADMIN structural changes, self-escalation, granting non-held permissions; `AuthorizationDecisionServiceTest` covers matrix |
| Audit append-only + hash chain | PASS | `DefaultAuditService.append` computes prev/current SHA-256 over canonical payload; `AuditIntegrityVerifier` detects tampering; repository never exposes `update`/`delete`; `AuditHashChainTest` |
| Bilingual RTL/LTR + themes | NOT VERIFIED | Frontend still pending |
| Docker configuration | NOT VERIFIED | — |
| Mandatory CI green | NOT VERIFIED | — |
| No committed secrets | PASS | `.env.example` only; `SecretEncryptionService` reads key from configuration; no default `admin/admin` |
| ASVS evidence | IN PROGRESS | Password Argon2 (V2.1), session rotation (V3.2), CSRF cookie (V4.2), rate limiting (V11.5), audit chain (V10.7), problem details (V13) |
| Pull Request | NOT VERIFIED | — |

Gates are marked PASS only with command/CI evidence recorded in this file or linked artifacts.
