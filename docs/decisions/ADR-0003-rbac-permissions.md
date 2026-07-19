# ADR-0003: Database-Backed Permission RBAC

## Status
Accepted

## Context
Role-name checks scatter and drift; fine-grained admin operations need explicit permissions.

## Decision
Users ↔ Roles ↔ Permissions. Authorize with `hasAuthority('PERMISSION_CODE')`. Reserved system roles; final SUPER_ADMIN protection; session privilege refresh on RBAC changes.

## Consequences
Richer admin UI and test matrix; authorization cache invalidation required.
