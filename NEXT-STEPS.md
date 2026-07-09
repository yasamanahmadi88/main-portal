# Next Steps

1. **First incomplete task:** Run the newly-committed GitHub Actions
   workflows against `cursor/complete-enterprise-portal`, collect the
   first successful runs, and update `QUALITY-GATES.md` (Mandatory CI
   green, Container scanning, Secret scanning) with the run URLs.
2. **Files to inspect:** `.github/workflows/{ci,container,sbom}.yml`,
   `compose.yaml`, `compose.observability.yaml`, `infrastructure/**`.
3. **Commands to run locally:**
   ```bash
   docker compose -f compose.yaml config -q                                  # already PASS
   docker compose -f compose.yaml -f compose.observability.yaml config -q    # already PASS
   docker compose -f compose.yaml up -d --build                              # needs buildx
   infrastructure/scripts/wait-for-healthy.sh -t 300 postgres redis backend frontend mailpit
   ./backend/mvnw -B verify                                                   # runs testcontainers
   cd frontend && npm ci && npm run lint && npm test && npm run build
   ```
4. **Failing tests:** None locally.  Live end-to-end verification against
   Postgres + Redis + Redis Session storage still pending — requires a
   sandbox where `docker buildx` is available.
5. **Relevant docs:** `docs/operations/deployment.md`,
   `docs/operations/runbook.md`, `PROGRESS.md` Phase 5 section.
6. **Unresolved risks:**
   - R1 (Docker verification) — compose config validated but image build
     unverified locally.
   - R2 (First-run CI) — GitHub Actions have never executed against this
     branch; expect small YAML fix-ups on the first run.
   - R7 (OTel + Loki OTLP endpoint) — Loki OTLP ingestion path may need
     tenant-id headers depending on the deployed Loki version; verify on
     first Grafana session.
