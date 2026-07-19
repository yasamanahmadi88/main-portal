# Authentication Design

## Overview

Authentication follows a same-origin Backend-for-Frontend design. The Angular SPA never stores bearer tokens. The backend authenticates users, creates an opaque server-side session, and returns an HttpOnly session cookie.

## Session BFF pattern

| Decision | Design |
|----------|--------|
| Browser token storage | None. No JWT in `localStorage` or `sessionStorage`. |
| Session identifier | Opaque cookie such as `PORTALSESSION`. |
| Session state | Redis via Spring Session. |
| Cookie flags | `HttpOnly`, `Secure` in production, `SameSite=Lax` unless stricter policy is validated. |
| Session rotation | Rotate on login and after material credential/MFA changes. |

## Login flow

1. Browser obtains initial CSRF token.
2. User submits email and password to `/api/auth/login`.
3. Backend canonicalizes the email and verifies the password with Argon2id.
4. On success, backend creates a new session, rotates the identifier, resolves authorities, and emits audit/security events.
5. On failure, backend returns a generic error and increments rate-limit/security counters.

## CSRF

- CSRF protection is mandatory for unsafe HTTP methods.
- Angular sends the `X-XSRF-TOKEN` header using the non-secret `XSRF-TOKEN` cookie.
- The backend validates that the submitted token matches the session-bound expected token.
- CSRF failures return `403` and must not reveal session internals.

## Password hashing

- Passwords use Argon2id through Spring Security `Argon2PasswordEncoder` and BouncyCastle.
- Parameters must be calibrated for production hardware before release.
- Hashes include algorithm parameters and salts.
- Passwords are never logged, returned, cached, or included in audit metadata.

## Rate limiting

Target rate-limit buckets:

| Flow | Bucket key | Response |
|------|------------|----------|
| Login | canonical email + IP hash | generic `401` or `429` when blocked |
| Password reset request | canonical email + IP hash | always `202` unless globally blocked |
| Password reset confirm | token hash + IP hash | `400`/`429` without token validity leaks |
| MFA challenge | user id + session id hash | `401`/`429` and security event |
| Bootstrap login/admin creation | environment + IP hash | fail closed after safe limit |

Redis stores counters and block windows. Rate-limit events are observable but must not expose raw IP addresses where hashing is required by policy.

## Password reset

- Request response is intentionally indistinguishable for known and unknown email addresses.
- Raw reset token is generated with a CSPRNG and displayed/sent once.
- Only a hash of the token is persisted.
- Reset tokens expire quickly and are one-time use.
- Successful reset revokes existing sessions and writes an audit event.

## MFA

- TOTP is the baseline MFA factor.
- TOTP secrets are encrypted at rest with an environment-supplied key and `key_id`.
- Recovery codes are generated once and stored only as hashes.
- MFA enrollment, disablement, and recovery-code use are audited.

## Failure behavior

- Authentication failures use generic user-facing messages.
- Account lockouts and high-risk failures create security events.
- Critical failures in audit persistence fail closed for login success, password reset completion, MFA changes, and privileged account changes.

## Test evidence to add

- Login success and failure tests.
- CSRF rejection tests for unsafe methods.
- Session rotation test after login.
- Password reset token hash/expiry tests.
- Argon2id encoder configuration test.
- Rate-limit tests using Redis/Testcontainers.
