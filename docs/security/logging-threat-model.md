# Logging Threat Model

## Scope

This model covers application logs, access logs, security events, audit metadata, metrics labels, traces, and notification delivery diagnostics.

## Sensitive values never allowed in logs

- Passwords and password hashes.
- Session IDs or raw cookie headers.
- CSRF tokens.
- Password reset tokens.
- TOTP secrets, QR provisioning URIs, and recovery codes.
- MFA encryption keys and database credentials.
- Full authorization headers if future integrations add them.

## Threats and controls

| Threat | Impact | Control | Status |
|--------|--------|---------|--------|
| Secret logged during authentication failure | Account compromise | Field allowlist logging, tests, redaction filters | Planned |
| Full request body logged for admin mutation | Privacy breach | Disable body logging by default; explicit safe summaries only | Planned |
| Session ID appears in access logs | Session replay risk | Mask cookie headers at edge and app | Planned |
| High-cardinality user data in metric labels | Cost and privacy issue | Use bounded labels; avoid email/user ID labels | Planned |
| Trace attributes include tokens | Secret propagation to telemetry | Attribute allowlist and review | Planned |
| Log forging through user input | Investigation confusion | JSON encoder, newline escaping, structured fields | Planned |
| Audit metadata over-collection | Compliance and privacy risk | Minimal schema per action | Planned |

## Structured logging standard

Required common fields:

- `timestamp`
- `level`
- `service`
- `environment`
- `correlation_id`
- `trace_id` and `span_id` when available
- `event_type`
- `outcome`
- `actor_user_id` only where appropriate

Avoid raw free-form concatenation of user input. Use structured key-value fields with sanitized values.

## Security event vs application log

- Application logs support debugging and operations.
- Security events support detection, triage, and response.
- Audit events support accountability and evidence.

Do not rely on application logs as the only record of privileged actions.

## Verification checklist

- Unit tests for redaction helpers.
- Integration test proving login failure logs do not contain submitted password.
- Integration test proving reset-token flow logs do not contain raw token.
- Review of nginx access log format before production.
- Review of OpenTelemetry attributes before production.
