# ASVS 5 Checklist

This evidence tracker covers key OWASP ASVS V2, V3, V4, V7, and V8 themes relevant to the portal. Requirement IDs should be reconciled with the exact ASVS 5 release text before formal assessment. Status is intentionally conservative until implementation and tests exist.

| Requirement ID | Summary | Applicability | Implementation evidence | Test evidence | Status | Limitation | Accepted risk |
|----------------|---------|---------------|-------------------------|---------------|--------|------------|---------------|
| V2 | Authentication architecture uses secure server-side sessions | Applicable | ADR-0002, authentication design | Pending | NOT VERIFIED | Code pending | None |
| V2 | Passwords are stored with a modern adaptive hash | Applicable | Argon2id documented; BouncyCastle dependency | Pending | NOT VERIFIED | Parameters not calibrated | None |
| V2 | Authentication errors do not disclose account existence | Applicable | Password reset/login design | Pending | NOT VERIFIED | UI/API pending | None |
| V2 | Password reset tokens are random, short-lived, hashed, and one-time use | Applicable | Authentication design | Pending | NOT VERIFIED | Migration/service pending | None |
| V2 | MFA secrets are protected at rest | Applicable | Crypto design, `.env.example` key placeholders | Pending | NOT VERIFIED | Key management external | None |
| V2 | Rate limiting protects login, reset, and MFA flows | Applicable | Authentication design | Pending | NOT VERIFIED | Redis policy pending | None |
| V3 | Session IDs are never exposed to JavaScript | Applicable | Session BFF ADR | Pending | NOT VERIFIED | Security config pending | None |
| V3 | Session ID rotates after login | Applicable | Session design | Pending | NOT VERIFIED | Code pending | None |
| V3 | Session timeout and invalidation policies exist | Applicable | `.env.example`, session design | Pending | NOT VERIFIED | Runtime enforcement pending | None |
| V3 | CSRF protects unsafe browser requests | Applicable | ADR-0002, auth design | Pending | NOT VERIFIED | Filter config pending | None |
| V3 | Logout invalidates server-side session | Applicable | Session sequence | Pending | NOT VERIFIED | Code pending | None |
| V4 | Access control is deny-by-default | Applicable | ADR-0003, authorization design | Pending | NOT VERIFIED | Code pending | None |
| V4 | Privileged functions require explicit permissions | Applicable | RBAC design, authorization matrix | Pending | NOT VERIFIED | Controllers pending | None |
| V4 | Horizontal access control is enforced for target resources | Applicable | Authorization design | Pending | NOT VERIFIED | Resource model pending | None |
| V4 | Privilege escalation paths are tested | Applicable | RBAC test plan pending | Pending | NOT VERIFIED | Test suite pending | None |
| V7 | Logs and audit events do not contain secrets | Applicable | Logging threat model, secure coding standard | Pending | NOT VERIFIED | Redaction implementation pending | None |
| V7 | Security-relevant events are logged/audited | Applicable | Audit integrity design | Pending | NOT VERIFIED | Event catalog pending | None |
| V7 | Audit integrity is protected | Applicable | ADR-0004, audit hash-chain design | Pending | NOT VERIFIED | DB privileges pending | None |
| V7 | Logs include correlation identifiers | Applicable | Logging design | Pending | NOT VERIFIED | Middleware pending | None |
| V8 | Sensitive data is classified | Applicable | Data classification document | Pending | NOT VERIFIED | Data inventory pending | None |
| V8 | Sensitive data is encrypted where required | Applicable | Crypto design | Pending | NOT VERIFIED | MFA encryption pending | None |
| V8 | Secrets are not committed to source control | Applicable | `.env.example`, `.gitignore` | Pending repository scan | IN PROGRESS | Full scan pending | None |
| V8 | Personal data retention is defined | Applicable | Data model retention notes | Pending | NOT VERIFIED | Policy approval pending | None |
| V8 | Backups protect sensitive and audit data | Applicable | Operations runbook | Pending | NOT VERIFIED | Environment-specific | None |

## Evidence rules

- Mark `PASS` only with linked tests, code references, or CI artifacts.
- Mark `FAIL` when a test demonstrates non-compliance.
- Use `NOT APPLICABLE` only with a short rationale in the limitation column.
- Accepted risks require owner, expiry, compensating controls, and entry in `accepted-risks.md`.
