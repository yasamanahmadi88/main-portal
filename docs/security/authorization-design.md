# Authorization Design

## Model

Authorization is permission-based. Roles grant permissions, users receive roles, and application checks use stable permission codes.

```mermaid
flowchart LR
  user[User] --> userRoles[user_roles]
  userRoles --> role[Role]
  role --> rolePerms[role_permissions]
  rolePerms --> perm[Permission]
  perm --> check[hasAuthority('PERMISSION_CODE')]
```

## Principles

- Deny by default.
- Check permissions at controller and service boundaries for sensitive operations.
- Do not authorize by role name except for reserved-role invariants such as final `SUPER_ADMIN` protection.
- Separate read, write, assign, export, and security-sensitive permissions.
- Treat authorization failures as security-relevant events when repeated or high risk.

## Enforcement points

| Layer | Responsibility |
|-------|----------------|
| Route/controller | Require coarse permission for endpoint access. |
| Application service | Re-check sensitive state transitions and target-specific rules. |
| Repository/query | Scope data reads to permitted subjects when needed. |
| UI | Hide unavailable actions for usability, never as the only control. |
| Tests | Prove allow and deny paths for each key permission. |

## Target-specific rules

- A user cannot disable or delete themselves through administrative APIs.
- The final active `SUPER_ADMIN` cannot be disabled, deleted, demoted, or stripped of required permissions.
- `SUPPORT` can perform support workflows but cannot grant roles or reset privileged users unless explicitly granted.
- `AUDITOR` can read audit evidence but cannot mutate users, roles, or settings.
- `SECURITY_ADMIN` can manage security posture but should not automatically receive broad user-management powers.

## Permission evaluation

1. Load current principal from session.
2. Resolve authority set from session/cache.
3. Compare session authorization version with current version.
4. If stale, refresh or invalidate the session.
5. Evaluate required permission.
6. Evaluate target-specific invariants.
7. Write audit event for privileged success and selected failures.

## Error handling

- Return `401` for unauthenticated requests.
- Return `403` for authenticated users without required permission.
- Do not disclose whether hidden resources exist unless the caller has read permission.
- Use RFC 9457-style problem details without secrets.

## Implementation evidence status

The ADR for permission-based RBAC is accepted. Controllers, services, migrations, and tests are pending, so all permission enforcement should be treated as design until code evidence is added.
