# RBAC Design

## Purpose

RBAC grants portal capabilities through roles and stable permission codes. Application code checks permissions with `hasAuthority('PERMISSION_CODE')`; roles are administrative bundles, not hard-coded authorization branches.

The catalog below is the baseline requirements-derived seed set for implementation. It is not yet verified in migrations or tests.

## System roles

| Role | Purpose |
|------|---------|
| `SUPER_ADMIN` | Break-glass and full platform administration. Must always have at least one active holder. |
| `ADMIN` | Broad day-to-day administration excluding break-glass-only controls. |
| `SECURITY_ADMIN` | Security posture, MFA, security events, hardening settings, and incident response. |
| `USER_MANAGER` | User lifecycle operations. |
| `ROLE_MANAGER` | Role and permission administration. |
| `AUDITOR` | Read-only audit, security-event, and compliance evidence review. |
| `SUPPORT` | Limited support troubleshooting and account assistance. |
| `USER` | Standard authenticated portal user. |

## Permission catalog

### Profile and dashboard

| Code | Description |
|------|-------------|
| `PROFILE_READ` | Read own profile and preferences. |
| `PROFILE_UPDATE` | Update own display preferences such as locale, direction, timezone, and theme. |
| `PROFILE_PASSWORD_CHANGE` | Change own password. |
| `PROFILE_MFA_MANAGE` | Enroll, verify, regenerate, or disable own MFA according to policy. |
| `DASHBOARD_VIEW` | View the authenticated dashboard. |

### User administration

| Code | Description |
|------|-------------|
| `USER_READ` | Read user directory and user details. |
| `USER_CREATE` | Create users. |
| `USER_UPDATE` | Update user profile/admin-managed attributes. |
| `USER_DISABLE` | Disable or enable users. |
| `USER_DELETE` | Delete or deprovision users where policy allows. |
| `USER_UNLOCK` | Clear lockouts or administrative holds. |
| `USER_PASSWORD_RESET` | Initiate or force password reset for another user. |
| `USER_SESSION_REVOKE` | Revoke another user's active sessions. |
| `USER_MFA_RESET` | Reset another user's MFA enrollment or recovery codes. |
| `USER_EXPORT` | Export user lists or user evidence. |

### Roles and permissions

| Code | Description |
|------|-------------|
| `ROLE_READ` | Read roles and role membership. |
| `ROLE_CREATE` | Create custom roles. |
| `ROLE_UPDATE` | Update role metadata and non-reserved grants. |
| `ROLE_DELETE` | Delete non-reserved roles. |
| `ROLE_ASSIGN` | Assign roles to users. |
| `ROLE_REVOKE` | Revoke roles from users. |
| `PERMISSION_READ` | Read permission catalog. |
| `PERMISSION_MANAGE` | Manage permission grants on roles. |

### Audit and compliance

| Code | Description |
|------|-------------|
| `AUDIT_READ` | Search and read audit events. |
| `AUDIT_EXPORT` | Export audit evidence. |
| `AUDIT_VERIFY` | Run or read audit hash-chain verification. |

### Security events and security settings

| Code | Description |
|------|-------------|
| `SECURITY_EVENT_READ` | Read security events and account-risk signals. |
| `SECURITY_EVENT_MANAGE` | Triage, annotate, or resolve security events. |
| `SECURITY_EVENT_EXPORT` | Export security-event evidence. |
| `SETTINGS_SECURITY_UPDATE` | Update security-sensitive settings such as session, MFA, or lockout policies. |

### Settings and notifications

| Code | Description |
|------|-------------|
| `SETTINGS_READ` | Read runtime settings. |
| `SETTINGS_UPDATE` | Update non-security runtime settings. |
| `NOTIFICATION_READ` | Read notification templates and delivery status. |
| `NOTIFICATION_SEND` | Send or requeue operational notifications. |
| `NOTIFICATION_TEMPLATE_MANAGE` | Manage localized notification templates. |

### Observability and operations

| Code | Description |
|------|-------------|
| `OBSERVABILITY_READ` | Read operational dashboards exposed by the portal. |
| `METRICS_READ` | Access application metrics where exposed through authenticated APIs. |
| `HEALTH_READ` | Read detailed health information. |
| `BOOTSTRAP_RUN` | Execute one-time bootstrap routines. Should not be grantable after initialization. |

## Baseline role grants

| Role | Baseline permissions |
|------|----------------------|
| `SUPER_ADMIN` | All permissions. Protected by final-super-admin invariant. |
| `ADMIN` | `DASHBOARD_VIEW`, profile permissions, user management except `USER_DELETE` where policy forbids, role read/assign/revoke, settings read/update, notification read/send, observability read, health read |
| `SECURITY_ADMIN` | `DASHBOARD_VIEW`, profile permissions, `USER_READ`, `USER_UNLOCK`, `USER_SESSION_REVOKE`, `USER_MFA_RESET`, `AUDIT_READ`, `AUDIT_VERIFY`, all `SECURITY_EVENT_*`, `SETTINGS_READ`, `SETTINGS_SECURITY_UPDATE`, observability/metrics/health read |
| `USER_MANAGER` | `DASHBOARD_VIEW`, profile permissions, `USER_READ`, `USER_CREATE`, `USER_UPDATE`, `USER_DISABLE`, `USER_UNLOCK`, `USER_PASSWORD_RESET`, `USER_SESSION_REVOKE`, `USER_EXPORT`, `ROLE_READ` |
| `ROLE_MANAGER` | `DASHBOARD_VIEW`, profile permissions, `USER_READ`, all `ROLE_*`, `PERMISSION_READ`, `PERMISSION_MANAGE`, `AUDIT_READ` for RBAC events |
| `AUDITOR` | `DASHBOARD_VIEW`, `PROFILE_READ`, `PROFILE_UPDATE`, `AUDIT_READ`, `AUDIT_EXPORT`, `AUDIT_VERIFY`, `SECURITY_EVENT_READ`, `SECURITY_EVENT_EXPORT`, `SETTINGS_READ`, `OBSERVABILITY_READ`, `METRICS_READ`, `HEALTH_READ` |
| `SUPPORT` | `DASHBOARD_VIEW`, profile permissions, `USER_READ`, `USER_UNLOCK`, `USER_PASSWORD_RESET`, `USER_SESSION_REVOKE`, `NOTIFICATION_READ`, `NOTIFICATION_SEND` |
| `USER` | `DASHBOARD_VIEW`, `PROFILE_READ`, `PROFILE_UPDATE`, `PROFILE_PASSWORD_CHANGE`, `PROFILE_MFA_MANAGE` |

## Reserved-role invariants

- Reserved roles cannot be deleted.
- Reserved role codes cannot be renamed.
- `SUPER_ADMIN` must retain all permissions.
- The final active `SUPER_ADMIN` cannot be disabled, deleted, demoted, or have its sessions revoked as a lockout mechanism unless an approved break-glass path exists.

## Testing requirements

- Seed-data test confirms every permission code exists exactly once.
- Matrix tests prove each role can and cannot perform key operations.
- Escalation tests cover self-grant, final `SUPER_ADMIN`, reserved-role mutation, and stale-session authority refresh.
