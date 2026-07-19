# Incident Response

Runbook for security and availability incidents affecting Enterprise Portal.

## Severity levels

| Level | Examples | Response time |
|-------|----------|---------------|
| **SEV-1** | Data breach, total outage, auth bypass | Immediate (< 15 min) |
| **SEV-2** | Partial outage, elevated failed logins, MFA bypass suspicion | < 1 hour |
| **SEV-3** | Degraded performance, non-critical bug | < 1 business day |
| **SEV-4** | Cosmetic, low-risk | Next sprint |

## On-call checklist

1. **Acknowledge** alert and assign incident commander
2. **Assess** scope: which services, users, data classes
3. **Contain** — see playbooks below
4. **Communicate** to stakeholders per org policy
5. **Eradicate** root cause
6. **Recover** services with verified backups if needed
7. **Post-incident review** within 5 business days

## Playbook: Suspected credential compromise

1. Disable affected user accounts via admin API or database (last resort)
2. Force session invalidation:
   ```bash
   docker compose exec redis redis-cli -a "${REDIS_PASSWORD}" FLUSHDB
   ```
   (Logs all users out — use only when necessary)
3. Rotate passwords and MFA recovery codes for affected accounts
4. Review audit log: `/api/v1/admin/audit` for anomalous IPs and actions
5. If bootstrap or admin password leaked, rotate immediately and review bootstrap env removal

## Playbook: Authentication outage (Redis down)

**Symptoms:** Readiness probe fails; new logins fail; existing sessions may error.

1. `docker compose ps redis`
2. `docker compose logs redis`
3. Restart Redis: `docker compose restart redis`
4. If persistent corruption, restore Redis volume or accept session loss and restart
5. Verify: `curl http://localhost/actuator/health/readiness`

## Playbook: Database unavailable

**Symptoms:** Readiness fails; 503 on API.

1. Check PostgreSQL logs and disk space
2. `docker compose exec postgres pg_isready`
3. Failover to replica (if configured) or restore from backup — see [backup-restore.md](./backup-restore.md)
4. Do not run Flyway clean in production

## Playbook: Suspected SQL injection / IDOR

1. Capture request logs and audit entries with correlation IDs
2. Block offending IP at load balancer if active attack
3. Patch and deploy hotfix via normal change process
4. Notify security team; consider [penetration test checklist](../security/penetration-test-checklist.md) re-run

## Playbook: MFA encryption key exposure

1. Treat as SEV-1 — see [key-rotation.md](./key-rotation.md) emergency procedure
2. Generate new key with new `PORTAL_MFA_ENCRYPTION_KEY_ID`
3. Force MFA re-enrollment for all users with MFA enabled
4. Invalidate all sessions

## Evidence preservation

- Export audit logs before destructive recovery steps
- Snapshot PostgreSQL volume if investigating data integrity
- Preserve container logs: `docker compose logs --no-color > incident-logs.txt`

## Communication template

```
Incident: [SEV-N] [short title]
Status: Investigating | Identified | Monitoring | Resolved
Impact: [who/what affected]
Actions: [current steps]
Next update: [time UTC]
```

## Escalation

| Role | Responsibility |
|------|----------------|
| Incident commander | Coordination, decisions |
| Backend on-call | API, DB, Redis |
| Platform on-call | Nginx, TLS, Compose/K8s |
| Security | Breach assessment, forensics |

## Related

- [monitoring.md](./monitoring.md)
- [backup-restore.md](./backup-restore.md)
- [production-hardening.md](./production-hardening.md)
- [../security/threat-model.md](../security/threat-model.md)
