# Audit Integrity Design

## Goals

- Make privileged actions attributable.
- Detect modification, deletion, reordering, or insertion of audit records.
- Fail closed when critical security operations cannot be audited.
- Preserve privacy by recording only necessary metadata.

## Audit event structure

| Field | Purpose |
|-------|---------|
| `id` | Monotonic or sortable event identifier. |
| `occurred_at` | Server-side timestamp. |
| `actor_user_id` | Authenticated actor or system principal. |
| `action` | Stable action code such as `USER_DISABLED`. |
| `target_type` / `target_id` | Affected entity. |
| `outcome` | Success, denied, failed, or system outcome. |
| `correlation_id` | Links request logs, traces, and audit. |
| `metadata_json` | Redacted structured metadata. |
| `previous_hash` | Prior audit event hash. |
| `current_hash` | Hash of canonical current event plus previous hash. |

## Hash-chain process

```mermaid
flowchart LR
  previous[Previous event current_hash]
  event[Canonical event payload]
  hash[Compute digest]
  insert[Insert audit_events row]
  previous --> hash
  event --> hash
  hash --> insert
```

## Database hardening

- Application runtime user: `INSERT` and `SELECT` only on `audit_events` (no `UPDATE`/`DELETE`/`TRUNCATE`).
- Application runtime user: `SELECT`/`INSERT`/`UPDATE` on `audit_event_chain` (stream cursor); no `DELETE`/`TRUNCATE`.
- Migration user: schema changes only during deployment.
- Backups must preserve audit rows and verification checkpoints.

## Critical audited operations

- Login success and selected login failures.
- Logout.
- Password reset completion.
- MFA enrollment, disablement, reset, and recovery-code use.
- User create/update/disable/delete/unlock.
- Role assignment/revocation.
- Permission grant changes.
- Security-sensitive setting changes.
- Audit export and integrity verification.
- Bootstrap first-admin creation.

## Integrity verifier

The verifier should:

1. Read events in canonical order.
2. Recompute every `current_hash`.
3. Confirm each `previous_hash` matches the prior event.
4. Store a checkpoint with checked range, last hash, status, and timestamp.
5. Alert on mismatch, gap, unexpected deletion, or query failure.

## Privacy rules

Audit metadata must not include:

- Passwords or password hashes.
- Session identifiers.
- CSRF tokens.
- Reset tokens.
- TOTP secrets.
- Recovery codes.
- Full raw request/response bodies.

## Runtime privilege enforcement

Flyway `V7` / `V9` revoke `UPDATE`/`DELETE`/`TRUNCATE` on `audit_events` (append-only log) while allowing `UPDATE` on `audit_event_chain` so the stream cursor can advance under a row lock. `DELETE`/`TRUNCATE` remain revoked on the chain table.

## Verification evidence

| Check | Command / artifact |
|-------|--------------------|
| Runtime role lacks UPDATE/DELETE | `infrastructure/scripts/audit-db-verify.sh` (`has_table_privilege`) |
| Hash fields populated | `audit-db-verify.sh` + `LiveSecurityIntegrationTest` |
| API integrity verify | `POST /api/v1/audit-events/verify-integrity` in `live-api-verify.sh` |
| Unit hash-chain math | `AuditHashChainTest` |
| CI live evidence | `fullstack-verify` artifact `live-stack-evidence/audit-integrity.txt` (green run [29050258913](https://github.com/yasamanahmadi88/main-portal/actions/runs/29050258913): runtime UPDATE/DELETE denied; hash-chain API PASS; no secrets in payload_json) |

## Limitations

The hash chain detects tampering but does not itself prevent privileged database administrators from modifying both data and hashes. Production assurance also requires restricted database administration, immutable backups, monitoring, and separation of duties.
