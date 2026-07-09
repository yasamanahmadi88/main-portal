# RBAC Test Plan

## Objectives

- Prove role-permission behavior matches `docs/security/authorization-matrix.md`.
- Detect privilege escalation and stale-session authorization defects.
- Provide ASVS V4 evidence.

## Test levels

| Level | Purpose |
|-------|---------|
| Unit | Permission catalog, role seed data, invariant functions. |
| Integration | Spring Security method/API enforcement with database-backed roles. |
| End-to-end | UI hides unavailable actions and backend denies direct calls. |
| Architecture | Modules cannot bypass `accesscontrol` repositories or services. |

## Required fixtures

Create one active user per baseline role:

- `SUPER_ADMIN`
- `ADMIN`
- `SECURITY_ADMIN`
- `USER_MANAGER`
- `ROLE_MANAGER`
- `AUDITOR`
- `SUPPORT`
- `USER`

Each fixture should have a known password, optional MFA state, and predictable locale (`fa-IR` by default unless test requires another locale).

## Matrix testing

For each key permission:

1. Identify at least one endpoint or service method requiring the permission.
2. Test every role expected to allow it.
3. Test at least two roles expected to deny it, including `USER` when relevant.
4. Assert `401` for anonymous and `403` for authenticated unauthorized access.
5. Assert successful privileged operations create audit events.

## Escalation scenarios

| Scenario | Expected result |
|----------|-----------------|
| `USER_MANAGER` assigns self `ADMIN` | Denied |
| `ADMIN` grants self `PERMISSION_MANAGE` | Denied |
| `ROLE_MANAGER` removes final `SUPER_ADMIN` | Denied |
| `SUPER_ADMIN` attempts to disable own final account | Denied unless another active `SUPER_ADMIN` exists |
| User with revoked permission uses old session | Denied after refresh/invalidation |
| `SUPPORT` resets password for privileged user | Denied unless explicit policy grants it |
| `AUDITOR` attempts audit export | Allowed |
| `AUDITOR` attempts user update | Denied |

## Seed-data tests

- Every permission code in `rbac-design.md` exists exactly once.
- Reserved role codes exist exactly once.
- `SUPER_ADMIN` contains all permissions.
- `USER` contains only self-service baseline permissions.
- No role references unknown permission codes.

## Evidence outputs

- Test class names and CI run links should be added to ASVS checklist.
- Failures that represent accepted behavior changes require updates to RBAC design and matrix docs.
- Security defects require risk-register entries until remediated.
