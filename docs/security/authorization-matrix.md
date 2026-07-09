# Authorization Matrix

Legend: `Y` = allowed by baseline role, `N` = denied, `C` = conditional/invariant-protected.

| Key permission | SUPER_ADMIN | ADMIN | SECURITY_ADMIN | USER_MANAGER | ROLE_MANAGER | AUDITOR | SUPPORT | USER |
|----------------|-------------|-------|----------------|--------------|--------------|---------|---------|------|
| `DASHBOARD_VIEW` | Y | Y | Y | Y | Y | Y | Y | Y |
| `PROFILE_READ` | Y | Y | Y | Y | Y | Y | Y | Y |
| `PROFILE_UPDATE` | Y | Y | Y | Y | Y | Y | Y | Y |
| `PROFILE_PASSWORD_CHANGE` | Y | Y | Y | Y | Y | N | Y | Y |
| `PROFILE_MFA_MANAGE` | Y | Y | Y | Y | Y | N | Y | Y |
| `USER_READ` | Y | Y | Y | Y | Y | N | Y | N |
| `USER_CREATE` | Y | Y | N | Y | N | N | N | N |
| `USER_UPDATE` | Y | Y | N | Y | N | N | N | N |
| `USER_DISABLE` | C | C | N | C | N | N | N | N |
| `USER_DELETE` | C | N | N | N | N | N | N | N |
| `USER_UNLOCK` | Y | Y | Y | Y | N | N | Y | N |
| `USER_PASSWORD_RESET` | Y | Y | N | Y | N | N | Y | N |
| `USER_SESSION_REVOKE` | C | Y | Y | Y | N | N | Y | N |
| `USER_MFA_RESET` | Y | Y | Y | N | N | N | N | N |
| `USER_EXPORT` | Y | Y | N | Y | N | N | N | N |
| `ROLE_READ` | Y | Y | N | Y | Y | N | N | N |
| `ROLE_CREATE` | Y | N | N | N | Y | N | N | N |
| `ROLE_UPDATE` | C | N | N | N | C | N | N | N |
| `ROLE_DELETE` | C | N | N | N | C | N | N | N |
| `ROLE_ASSIGN` | C | C | N | N | C | N | N | N |
| `ROLE_REVOKE` | C | C | N | N | C | N | N | N |
| `PERMISSION_READ` | Y | N | N | N | Y | N | N | N |
| `PERMISSION_MANAGE` | C | N | N | N | C | N | N | N |
| `AUDIT_READ` | Y | N | Y | N | Y | Y | N | N |
| `AUDIT_EXPORT` | Y | N | N | N | N | Y | N | N |
| `AUDIT_VERIFY` | Y | N | Y | N | N | Y | N | N |
| `SECURITY_EVENT_READ` | Y | N | Y | N | N | Y | N | N |
| `SECURITY_EVENT_MANAGE` | Y | N | Y | N | N | N | N | N |
| `SECURITY_EVENT_EXPORT` | Y | N | Y | N | N | Y | N | N |
| `SETTINGS_READ` | Y | Y | Y | N | N | Y | N | N |
| `SETTINGS_UPDATE` | Y | Y | N | N | N | N | N | N |
| `SETTINGS_SECURITY_UPDATE` | Y | N | Y | N | N | N | N | N |
| `NOTIFICATION_READ` | Y | Y | N | N | N | N | Y | N |
| `NOTIFICATION_SEND` | Y | Y | N | N | N | N | Y | N |
| `NOTIFICATION_TEMPLATE_MANAGE` | Y | N | N | N | N | N | N | N |
| `OBSERVABILITY_READ` | Y | Y | Y | N | N | Y | N | N |
| `METRICS_READ` | Y | N | Y | N | N | Y | N | N |
| `HEALTH_READ` | Y | Y | Y | N | N | Y | N | N |
| `BOOTSTRAP_RUN` | C | N | N | N | N | N | N | N |

## Conditional rules

- `C` entries require target-specific checks and audit events.
- No role may remove the final active `SUPER_ADMIN`.
- `BOOTSTRAP_RUN` is only valid before initialization and should not remain grantable afterward.
- `USER_SESSION_REVOKE` cannot be abused to lock out the final `SUPER_ADMIN`.
- `ROLE_ASSIGN`, `ROLE_REVOKE`, and `PERMISSION_MANAGE` must block self-escalation unless the actor already has equivalent authority and the action does not violate reserved-role policy.

## Required tests

- One positive and one negative test per key permission.
- Self-escalation tests for `ADMIN`, `ROLE_MANAGER`, and `USER_MANAGER`.
- Final `SUPER_ADMIN` protection tests.
- Stale-session refresh test after role revocation.
