# Penetration Test Plan

## Objective

Validate the portal's authentication, authorization, audit, logging, and deployment controls before production release. This plan is scoped for authorized testing only.

## Preconditions

- Test environment mirrors production security configuration.
- Seeded users exist for all baseline roles (`SUPER_ADMIN`, `ADMIN`, `SECURITY_ADMIN`, `USER_MANAGER`, `ROLE_MANAGER`, `AUDITOR`, `SUPPORT`, `USER` via Flyway V2+V8).
- Test data is synthetic.
- Logging and audit are enabled.
- Rate-limit thresholds are documented for testers.
- CI `fullstack-verify` has produced a recent **green** live-stack evidence pack (example: Actions run `29050258913` on tip `41eac10`, artifact `live-stack-evidence` + `zap-baseline-report`).

## Test accounts

| Account | Role |
|---------|------|
| `superadmin.test` | `SUPER_ADMIN` |
| `admin.test` | `ADMIN` |
| `securityadmin.test` | `SECURITY_ADMIN` |
| `usermanager.test` | `USER_MANAGER` |
| `rolemanager.test` | `ROLE_MANAGER` |
| `auditor.test` | `AUDITOR` |
| `support.test` | `SUPPORT` |
| `user.test` | `USER` |

## Test areas

| Area | Scenarios |
|------|-----------|
| Authentication | Credential stuffing, generic errors, session rotation, logout invalidation, password reset token replay, MFA enrollment/challenge/recovery |
| Session/CSRF | Missing/invalid CSRF, SameSite behavior, cookie flags, fixation attempts, stale-session authority |
| Authorization | Role matrix allow/deny, self-escalation, final `SUPER_ADMIN`, horizontal access control, hidden resource probing |
| Input validation | XSS, SQL injection, mass assignment, invalid locale/theme/settings keys, oversized payloads |
| Audit | Required event coverage, hash-chain verification, audit failure behavior, export permissions |
| Logging | Secret redaction, log forging, sensitive trace attributes, high-cardinality metrics |
| Deployment | TLS, security headers, actuator exposure, OpenAPI exposure, nginx request limits |
| Data protection | MFA secret encryption, token hashing, backup handling, export controls |

## Deliverables

- Findings with severity, reproduction steps, affected component, evidence, and remediation guidance.
- Updated risk register entries for accepted or deferred findings.
- Updated ASVS checklist evidence.
- Retest results after remediation.

## Out of scope unless separately authorized

- Social engineering.
- Physical attacks.
- Denial-of-service beyond agreed rate limits.
- Testing against production without a written change window.
- Accessing real customer or employee personal data.

## Entry criteria

- Core auth/RBAC/audit flows implemented.
- CI test suite passing.
- Test environment URL and accounts available.
- Monitoring contact and rollback plan defined.

## Exit criteria

- Critical and high findings remediated or formally accepted with expiry.
- Medium findings triaged with owners.
- No known secret leakage in logs or telemetry.
- ASVS evidence updated.
