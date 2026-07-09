# Quality Gates

Status legend: `PASS` | `FAIL` | `NOT VERIFIED` | `IN PROGRESS`

| Gate | Status | Evidence |
|------|--------|----------|
| Correct repository structure | IN PROGRESS | Directories created |
| Java 25 backend compilation | NOT VERIFIED | — |
| Angular 22 frontend compilation | NOT VERIFIED | — |
| Production builds | NOT VERIFIED | — |
| Unit / integration / architecture tests | NOT VERIFIED | — |
| E2E / accessibility tests | NOT VERIFIED | — |
| Flyway on PostgreSQL | NOT VERIFIED | — |
| Redis sessions / CSRF / login / logout | NOT VERIFIED | — |
| MFA / recovery codes | NOT VERIFIED | — |
| Complete RBAC + escalation protections | NOT VERIFIED | — |
| Audit append-only + hash chain | NOT VERIFIED | — |
| Bilingual RTL/LTR + themes | NOT VERIFIED | — |
| Docker configuration | NOT VERIFIED | — |
| Mandatory CI green | NOT VERIFIED | — |
| No committed secrets | IN PROGRESS | `.env.example` only |
| ASVS evidence | NOT VERIFIED | — |
| Pull Request | NOT VERIFIED | — |

Gates are marked PASS only with command/CI evidence recorded in this file or linked artifacts.
