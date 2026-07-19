# Secure Coding Standard

## General rules

- Prefer framework security controls over custom security code.
- Validate all external input at API boundaries.
- Encode output in the correct context.
- Fail closed for authentication, authorization, audit persistence, and cryptographic errors.
- Keep implementation evidence current in ASVS and quality-gate documents.

## Authentication and sessions

- Never store JWTs or bearer tokens in browser storage.
- Never disable CSRF for browser-facing unsafe methods.
- Rotate sessions after login and material credential changes.
- Use generic authentication failure messages.
- Do not log credentials, tokens, cookies, MFA secrets, or recovery codes.

## Authorization

- Use permission checks such as `hasAuthority('USER_UPDATE')`.
- Do not rely on frontend route guards as enforcement.
- Add tests for both allow and deny cases.
- Re-check high-risk invariants in services, especially final `SUPER_ADMIN` protection.

## Input validation

- Use Bean Validation and explicit allowlists for enums, locales, themes, and settings keys.
- Bound lengths for names, descriptions, metadata, and search queries.
- Use pagination for collection endpoints.
- Reject unknown or unsupported values rather than silently accepting them.

## Persistence

- Use parameterized queries or JPA repositories; no string-concatenated SQL.
- Model uniqueness and foreign-key constraints in the database.
- Keep Flyway migrations deterministic and reviewable.
- Separate migration credentials from runtime credentials.

## Logging and audit

- Log structured summaries, not raw request bodies.
- Audit privileged state changes.
- Use correlation IDs across logs, traces, and audit records.
- Keep audit metadata minimal and redacted.

## Frontend

- Treat all API data as untrusted.
- Do not use `innerHTML` with untrusted content.
- Keep Persian (`fa-IR`) and RTL support data-driven without bypassing validation.
- Use Angular's built-in sanitization; document any bypass and security review.

## Dependencies

- Prefer managed BOM versions.
- Do not add dependencies for small utilities unless justified.
- Run dependency and SBOM checks in CI once configured.
- Remove unused dependencies.

## Code review checklist

- Are authentication and authorization controls explicit?
- Are tests present for denied access?
- Could any log, metric, trace, or audit metadata include a secret?
- Are database constraints enforcing important invariants?
- Are error messages safe and useful?
- Is the change reflected in architecture/security docs when needed?
