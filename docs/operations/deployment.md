# Deployment guide

The enterprise portal ships as a set of container images orchestrated by
Docker Compose in development and by whatever container orchestrator you
prefer (Kubernetes, ECS, Nomad …) in production.  This document covers the
local Compose workflow and the production requirements that must hold true
for any deployment target.

## Contents

1. [Compose overview](#compose-overview)
2. [Local development](#local-development)
3. [Observability stack](#observability-stack)
4. [First-time admin bootstrap](#first-time-admin-bootstrap)
5. [Production requirements](#production-requirements)
6. [Rolling updates & rollback](#rolling-updates--rollback)

---

## Compose overview

| File                          | Purpose                                                             |
| ----------------------------- | ------------------------------------------------------------------- |
| `compose.yaml`                | Core stack — Postgres, Redis, Mailpit, backend, frontend (nginx).   |
| `compose.observability.yaml`  | Overlay — OpenTelemetry Collector, Prometheus, Loki, Tempo, Grafana. |
| `.env` / `.env.example`       | All configuration is read from environment variables.  Never commit `.env`. |
| `infrastructure/docker/**`    | Multi-stage Dockerfiles for backend and frontend images.            |
| `infrastructure/nginx/**`     | SPA + reverse-proxy configuration; served by the frontend image.    |
| `infrastructure/database/init/01-roles.sh` | Bootstraps the migration and app PostgreSQL roles on first-start. |

Networks:

* `frontend` — nginx <-> outside world.
* `backend`  — nginx <-> backend <-> mail/OTel collector.
* `data`     — backend <-> Postgres / Redis (marked `internal: true`; not
  reachable from any other compose service).
* `observability` — Grafana <-> Loki, Tempo, Prometheus.

## Local development

```bash
cp .env.example .env
# 1. Generate a real MFA key
openssl rand -base64 32 | xargs -I{} sed -i.bak 's|MFA_ENCRYPTION_KEY_BASE64=.*|MFA_ENCRYPTION_KEY_BASE64={}|' .env
# 2. Pick strong passwords for the local Postgres/Redis (never ship defaults).
$EDITOR .env

# 3. Validate the compose file.  This is the same command CI runs.
docker compose -f compose.yaml config -q

# 4. Bring the core stack up.
docker compose -f compose.yaml up -d --build

# 5. Wait for everything to become healthy.
infrastructure/scripts/wait-for-healthy.sh -t 300 postgres redis backend frontend mailpit

# 6. Browse the SPA
open http://localhost:8080

# 7. Watch mail deliveries
open http://localhost:8025
```

Tear down:

```bash
docker compose -f compose.yaml down                # keep volumes
docker compose -f compose.yaml down --volumes      # wipe DB + Redis + Mailpit
```

## Observability stack

Bring up the observability overlay side-by-side with the core stack:

```bash
docker compose -f compose.yaml -f compose.observability.yaml up -d --build
open http://localhost:3000          # Grafana (admin / GRAFANA_ADMIN_PASSWORD)
open http://localhost:9090          # Prometheus
```

Grafana is provisioned with three datasources (Prometheus, Loki, Tempo) and
one starter dashboard, `Portal / Portal Backend Overview`, panelling JVM
memory, HTTP throughput, HTTP p95 latency, DB pool activity and login
attempts.  Extend by dropping additional JSON dashboards into
`infrastructure/observability/grafana/provisioning/dashboards/portal/`.

## First-time admin bootstrap

The backend never seeds a default `admin/admin` account.  Instead, the
`BootstrapAdminRunner` reads three environment variables **once** at
start-up and creates the initial SUPER_ADMIN if the users table is empty:

```
BOOTSTRAP_ADMIN_EMAIL
BOOTSTRAP_ADMIN_PASSWORD
BOOTSTRAP_ADMIN_DISPLAY_NAME
```

`infrastructure/scripts/bootstrap-admin.sh` validates the variables and
password policy before you start the container.  **It never logs the
password.**

After first login:

1. Log in via the portal, force password rotation, enable MFA.
2. Remove all three variables from the deployment secret store.
3. Redeploy — the runner will refuse to run again once a user exists.

## Production requirements

The Compose stack is a fully working example, but a production deployment
should observe the following invariants:

* **TLS termination outside the container** — the frontend image only speaks
  cleartext HTTP on port 8080.  Front it with a TLS-terminating ingress
  (ALB, Cloud Load Balancer, Traefik, Nginx …) that sets `X-Forwarded-Proto`
  correctly.  HSTS is emitted unconditionally by the frontend.
* **No published Postgres/Redis ports** — delete the `ports:` block on the
  `postgres` and `redis` services (they exist only so local `psql`/`redis-cli`
  from the host machine works).  Rely on the internal `data` network.
* **Secrets from a real secret store** — mount `.env` from a KMS-backed
  file (SOPS, sealed-secrets, Vault agent, …); do not commit real values.
* **Distinct users for migration vs. runtime** — the app image is expected to
  run under `POSTGRES_USER` (`portal_app`), and Flyway migrations under
  `POSTGRES_MIGRATION_USER` (`portal_migration`).  The compose default
  wires these into `SPRING_FLYWAY_USER` / `SPRING_FLYWAY_PASSWORD`.
* **Backups** — snapshot the `postgres-data` volume + WAL archives on a
  schedule.  Redis is used only for sessions/rate-limiting and can be
  rebuilt cold; do NOT rely on it as a store of record.
* **Resource limits** — set `deploy.resources.limits` (or the equivalent in
  your orchestrator) once you have baselined the workload.
* **Read-only rootfs** — both application images already run with
  `read_only: true`.  Do not remove it.  Add tmpfs mounts if a future
  library needs a scratch directory.
* **Image provenance** — the `container.yml` workflow builds both images
  with docker buildx, scans them with Trivy (fail on CRITICAL), and
  uploads SARIF to the code-scanning tab.  The `sbom.yml` workflow
  produces CycloneDX SBOMs for both backend and frontend.

## Rolling updates & rollback

Zero-downtime deploy:

1. Build + tag new images (`portal-backend:<sha>`, `portal-frontend:<sha>`).
2. Push to your registry (`ghcr.io/<org>/main-portal/portal-*`).
3. Update `PORTAL_BACKEND_IMAGE` / `PORTAL_FRONTEND_IMAGE` in the deploy
   environment and `docker compose up -d` (or invoke the equivalent
   orchestrator update).
4. Wait for `wait-for-healthy.sh` to succeed against the new revision.
5. If health checks fail, roll back by pointing the image variables at the
   previous SHA and re-applying.

Flyway migrations are idempotent and forward-compatible with the previous
schema wherever possible; any breaking schema change must be split across
two releases (add, then remove) per the standard expand/contract pattern.
