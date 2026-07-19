# Operations Runbook

## Startup

### Local development

Backend:

```bash
cd backend
./mvnw spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm start
```

The local stack is Docker-first via `compose.yaml` (optional `compose.dev.yaml` / `compose.observability.yaml`). Configuration matches `.env.example`.

### Production startup checklist

1. Confirm environment variables and secrets are present.
2. Run Flyway migrations with migration credentials.
3. Start PostgreSQL and Redis or confirm managed services are healthy.
4. Start backend.
5. Start nginx or edge route serving Angular and proxying `/api`.
6. Confirm `/actuator/health` liveness and private readiness checks.
7. Confirm logs and OTLP telemetry are flowing.

## Bootstrap first administrator

Public self-registration is disabled. The first administrator is created by one-time bootstrap environment variables:

- `BOOTSTRAP_ADMIN_EMAIL`
- `BOOTSTRAP_ADMIN_PASSWORD`
- `BOOTSTRAP_ADMIN_DISPLAY_NAME`

Procedure:

1. Set bootstrap values in the deployment secret mechanism.
2. Start the backend.
3. Confirm bootstrap creates the first `SUPER_ADMIN` and records an audit event.
4. Remove bootstrap values from the environment or secret store.
5. Restart or redeploy if required to ensure values are no longer present.
6. Confirm additional startup runs are idempotent and do not recreate or reset the admin.

Implementation is pending; do not treat this as operationally verified until tests and audit evidence exist.

## Backup notes

### PostgreSQL

- Back up the full portal database, including audit and security-event tables.
- Test restore procedures before production.
- Preserve audit hash-chain order and checkpoint records.
- Restrict backup access because backups contain Confidential, Restricted, and Evidence data.

### Redis

Redis stores sessions and rate-limit state. Losing Redis may log users out and reset counters. Decide whether persistence is required for the deployment's availability and security needs.

### Configuration and secrets

- Back up configuration definitions, not raw secret values in source control.
- Keep MFA encryption keys available for restore; losing keys can make enrolled TOTP secrets unrecoverable.
- Record key IDs and rotation history.

## Incident response quick actions

| Symptom | Action |
|---------|--------|
| Audit append failures | Treat as high severity; pause privileged operations if fail-closed behavior is triggered. |
| Audit hash mismatch | Preserve database and logs, stop destructive maintenance, start security investigation. |
| Credential stuffing spike | Review rate-limit events, consider temporary stricter thresholds, notify security owner. |
| Redis outage | Expect login/session failures; restore Redis, verify session behavior, review security events. |
| Suspected secret leak | Rotate affected secret, search logs/telemetry, open risk/incident record. |

## Routine maintenance

- Review dependency updates and vulnerability scans.
- Review ASVS evidence and quality gates before release.
- Test backup restore.
- Run audit hash-chain verifier.
- Review accepted risks for expiry.
