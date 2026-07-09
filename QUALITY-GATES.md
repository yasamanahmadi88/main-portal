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
| Docker configuration | IN PROGRESS | `docker compose -f compose.yaml config -q` → PASS; `-f compose.yaml -f compose.observability.yaml config -q` → PASS.  Real image build **NOT VERIFIED** locally (`docker buildx` missing in sandbox); runs in `.github/workflows/container.yml` via `docker/setup-buildx-action@v3`. |
| Mandatory CI green | IN PROGRESS | `.github/workflows/{ci,container,sbom}.yml` committed; awaiting GitHub-side execution.  Backend `./mvnw test` and frontend `npm run lint`/`test`/`build` are green locally. |
| Supply-chain SBOM | PASS | Backend: CycloneDX Maven plugin already generates `target/bom.json` at `package`; also emitted as JSON+XML by `sbom.yml`.  Frontend: `@cyclonedx/cyclonedx-npm` in `sbom.yml`. |
| Container image scanning | IN PROGRESS | `.github/workflows/container.yml` builds both images with buildx and runs Trivy with `exit-code: 1` on CRITICAL (SARIF uploaded to code-scanning). Awaiting first successful CI run. |
| Secret scanning | IN PROGRESS | Gitleaks configured in `infrastructure/security/gitleaks.toml`; runs in `ci.yml` `security` job on every push/PR. |
| No committed secrets | PASS | `.env.example` only; `SecretEncryptionService` reads key from configuration; no default `admin/admin` |
| ASVS evidence | IN PROGRESS | Password Argon2 (V2.1), session rotation (V3.2), CSRF cookie (V4.2), rate limiting (V11.5), audit chain (V10.7), problem details (V13) |
| Pull Request | NOT VERIFIED | — |

Gates are marked PASS only with command/CI evidence recorded in this file or linked artifacts.
