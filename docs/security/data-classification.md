# Data Classification

## Classification levels

| Level | Description | Examples | Handling |
|-------|-------------|----------|----------|
| Public | Safe for unauthenticated public disclosure | Product name, public health status without details | May be cached and logged normally |
| Internal | Operational data for authenticated staff | Non-sensitive settings, feature flags, aggregate dashboard counts | Authenticated access; avoid unnecessary exposure |
| Confidential | Personal or business-sensitive data | Email, display name, locale, timezone, user status, role membership | Access controlled; redact in logs; retention policy |
| Restricted | Secrets or security-critical data | Passwords, session IDs, CSRF tokens, reset tokens, TOTP secrets, recovery codes, encryption keys | Never log; encrypt/hash as designed; least privilege |
| Regulated/Evidence | Audit and compliance records | Audit events, security events, exports, integrity checkpoints | Append-only where required; controlled export; retention |

## Data inventory

| Data | Classification | Storage | Notes |
|------|----------------|---------|-------|
| Email address | Confidential | `users` | Unique identity attribute. |
| Display name | Confidential | `users` | Localized display support. |
| Locale and direction | Internal/Confidential | `users`, `user_preferences` | Default `fa-IR`, `rtl`. |
| Password | Restricted | Not stored | Only Argon2id hash stored. |
| Password hash | Restricted | `user_credentials` | Never returned or logged. |
| Session ID | Restricted | Redis/cookie | HttpOnly cookie; never logged. |
| CSRF token | Restricted operational token | Session/cookie | Non-secret to JS but still not logged. |
| TOTP secret | Restricted | `mfa_factors` encrypted | Key external to DB. |
| Recovery code | Restricted | Not stored raw | Store hash only. |
| Role membership | Confidential | `user_roles` | Exposes privilege posture. |
| Audit event | Regulated/Evidence | `audit_events` | Append-only and hash chained. |
| Security event | Regulated/Evidence | `security_events` | May contain hashed IP/user-agent metadata. |
| Notification payload | Confidential | outbox | Minimize and expire. |
| Metrics | Internal | telemetry backend | Avoid user-identifying labels. |
| Traces | Internal/Confidential | telemetry backend | Attribute allowlist. |

## Handling rules

- Restricted values must not appear in logs, traces, metrics labels, audit metadata, screenshots, or support tickets.
- Confidential data requires authenticated and authorized access.
- Exports containing Confidential or Regulated/Evidence data require explicit permissions and audit.
- Data retention must be defined before production launch.
- Backups inherit the highest classification of included data.

## Localization note

Persian (`fa-IR`) is the default language and right-to-left direction is the default display mode. Locale and direction are preferences, not authorization attributes, and must not influence access-control decisions.
