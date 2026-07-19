# Security Policy

## Supported status

Core authentication, CSRF, Redis sessions, RBAC, audit integrity, and compose-stack controls are implemented and evidenced by green `fullstack-verify` CI. Residual gaps (live MFA e2e suite, production TLS/WAF, external pen-test, observability overlay) are tracked in `QUALITY-GATES.md`, `docs/security/asvs-5-checklist.md`, and `docs/security/accepted-risks.md`. Treat as **candidate for human security review**, not auto-approved for production go-live.

## Reporting vulnerabilities

Do not open public issues containing exploit details, secrets, account data, or sensitive logs. Use the private reporting channel defined by the repository owner or organization. If no private channel is configured yet, contact the project maintainer directly before disclosing details.

Include:

- Affected component and version/commit.
- Reproduction steps.
- Impact and required privileges.
- Evidence with secrets redacted.
- Suggested remediation if known.

## Security design baseline

The target security architecture includes:

- Same-origin session BFF.
- Opaque HttpOnly Redis-backed session cookies.
- Mandatory CSRF protection.
- Argon2id password hashing.
- Encrypted TOTP secrets and hashed recovery codes.
- Database-backed permission RBAC.
- Tamper-evident append-only audit events.
- Structured log redaction.

See `docs/security/` for details.

## Secret handling

- Never commit `.env`, production secrets, keys, certificates, database dumps, or real user data.
- `.env.example` contains placeholders only.
- Rotate any secret that may have been exposed in source, logs, telemetry, or screenshots.

## Security review expectations

Changes touching authentication, session handling, CSRF, RBAC, cryptography, audit, logging, or deployment hardening require:

- Tests for success and failure paths.
- Documentation updates where behavior changes.
- Review for secret leakage in logs, metrics, traces, audit, and errors.
- Updates to ASVS evidence when controls become verified.
