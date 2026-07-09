# Production Hardening

## Application configuration

- Set `PORTAL_ENV=production`.
- Use production database, Redis, SMTP, and OTLP endpoints from secret-managed configuration.
- Enable `SESSION_COOKIE_SECURE=true`.
- Use strong, unique database passwords for migration and runtime users.
- Remove bootstrap admin environment variables after first successful bootstrap.
- Disable development-only OpenAPI and verbose error details unless admin-restricted.

## nginx and HTTP

- Enforce HTTPS and redirect HTTP to HTTPS.
- Enable HSTS after validating HTTPS-only operation.
- Add:
  - `X-Content-Type-Options: nosniff`
  - `Referrer-Policy: strict-origin-when-cross-origin`
  - `Content-Security-Policy` appropriate for Angular assets
  - `X-Frame-Options` or CSP `frame-ancestors`
- Restrict request body sizes and timeouts.
- Serve only expected static asset paths.

## Spring Boot

- Expose only required actuator endpoints.
- Keep `/actuator/prometheus` on a private network.
- Use problem details without stack traces for users.
- Enforce CSRF for unsafe browser requests.
- Enable method security for privileged services.
- Confirm session fixation protection is active.

## PostgreSQL

- Use separate migration and runtime users.
- Deny runtime `UPDATE`/`DELETE` on `audit_events`.
- Enable backups and restore testing.
- Restrict network access to application hosts.
- Monitor failed logins and privileged schema changes.

## Redis

- Require authentication where supported.
- Bind to private interfaces only.
- Configure persistence according to session-loss tolerance.
- Monitor memory, evictions, and connection failures.

## Secrets

- Use a secret manager or platform secrets.
- Rotate MFA encryption keys with documented `key_id` migration.
- Never commit `.env`, keys, database dumps, or production certificates.
- Review CI logs for accidental secret output.

## Observability

- Confirm logs, metrics, and traces do not contain restricted data.
- Configure alerts for:
  - login failure spikes
  - lockout spikes
  - audit persistence failures
  - audit verifier mismatch
  - Redis unavailability
  - PostgreSQL connection exhaustion

## Release gate

Before production:

- Backend and frontend builds pass.
- Auth/session/CSRF/RBAC/audit integration tests pass.
- RBAC matrix tests pass.
- Dependency and secret scans pass.
- Penetration-test critical/high findings are resolved or accepted.
- ASVS checklist contains implementation and test evidence for applicable key controls.
