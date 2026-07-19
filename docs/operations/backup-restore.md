# Backup and Restore

Procedures for PostgreSQL (authoritative data) and Redis (ephemeral sessions).

## What to backup

| Store | Data | Priority |
|-------|------|----------|
| **PostgreSQL** | Users, RBAC, audit, settings, MFA metadata | **Critical** — daily minimum |
| **Redis** | Sessions, rate-limit counters | Low — acceptable to lose (users re-login) |
| **Configuration** | `.env`, TLS certs, keys (secure vault) | Critical — out of band |

## PostgreSQL backup

### Logical backup (recommended)

```bash
# From host with access to portal-internal network
docker compose exec -T postgres \
  pg_dump -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" --format=custom \
  > "portal-$(date +%Y%m%d-%H%M%S).dump"
```

### Restore

```bash
# Stop backend to prevent writes during restore
docker compose stop backend

docker compose exec -T postgres \
  pg_restore -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" --clean --if-exists \
  < portal-YYYYMMDD-HHMMSS.dump

docker compose start backend
./infra/scripts/wait-for-healthy.sh backend
```

### Verify backup

```bash
pg_restore --list portal-YYYYMMDD.dump | head
```

Periodically restore to a staging environment and run smoke tests.

## Volume backup

Docker named volume `postgres-data`:

```bash
docker run --rm \
  -v main-portal_postgres-data:/data:ro \
  -v "$(pwd)/backups:/backup" \
  alpine tar czf /backup/postgres-volume-$(date +%Y%m%d).tar.gz -C /data .
```

## Redis

Sessions are transient. **Do not rely on Redis backup** for disaster recovery.

If needed for debugging:

```bash
docker compose exec redis redis-cli -a "${REDIS_PASSWORD}" SAVE
```

## Retention

| Environment | Suggested retention |
|-------------|---------------------|
| Production | 30 daily, 12 monthly |
| Staging | 7 daily |
| Development | Optional |

Store backups encrypted at rest; restrict access to DBAs and break-glass roles.

## Recovery objectives

| Metric | Target (adjust per SLA) |
|--------|-------------------------|
| RPO | ≤ 24 hours |
| RTO | ≤ 4 hours |

## Disaster recovery drill

Quarterly:

1. Restore latest backup to isolated environment
2. Start Compose with restored volume
3. Verify admin login and audit log integrity
4. Document duration and issues

## Related

- [deployment.md](./deployment.md)
- [incident-response.md](./incident-response.md)
