# Quality Gates

| Gate | Status | Evidence |
|------|--------|----------|
| Repository structure | PASS | Root layout present |
| Java 25 compile | PASS | `./mvnw -DskipTests compile` |
| Backend unit/arch/modulith tests | PASS | 36 run / 0 fail / 1 skipped |
| Backend Testcontainers IT | NOT VERIFIED locally | Overlay mount failure; CI has Docker |
| Angular 22 build | PASS | `npm run build` |
| Frontend unit tests | PASS | 39 passed |
| Frontend lint | PASS (CI job) | warnings only locally acceptable |
| E2E / axe | NOT VERIFIED | Playwright skips without stack |
| Flyway migrations | PASS (files + CI path) | V1–V6 committed |
| Redis sessions / CSRF / login | PARTIAL | Implemented; full IT pending CI |
| MFA / recovery codes | PARTIAL | Unit TOTP/crypto tests PASS |
| RBAC escalation protections | PARTIAL | AuthorizationDecisionServiceTest PASS |
| Audit hash chain | PASS (unit) | AuditHashChainTest PASS |
| Bilingual + themes | PASS (unit) | i18n/theme specs PASS |
| Compose config | PASS | `docker compose config -q` |
| Container build/scan | NOT VERIFIED locally | CI container workflow |
| Mandatory CI green | IN PROGRESS | Fixing Spectral/Trivy/SBOM/event_publication |
| No committed secrets | PASS | `.env.example` placeholders only |
| ASVS evidence | PARTIAL | Checklist present; many NOT VERIFIED |
| Pull Request | IN PROGRESS | Creating/updating |
