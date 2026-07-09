# Main Portal

Enterprise-ready web portal foundation for the `yasamanahmadi88/main-portal` repository.

## Repository confirmation

| Item | Value |
|------|-------|
| GitHub repository | `yasamanahmadi88/main-portal` |
| Repository URL | https://github.com/yasamanahmadi88/main-portal |
| Base branch | `main` |
| Foundation branch | `cursor/portal-foundation` |

## Stack (foundation phase)

| Layer | Choice | Notes |
|-------|--------|-------|
| Backend | Spring Boot 3.4 / Java 21 | Cloud image has OpenJDK 21 (not Java 25) |
| Frontend | React 19 + Vite + TypeScript | Node.js 22 |
| Build | Maven (backend), npm (frontend) | Setup scripts install Maven when missing |
| Config | `.env.example` placeholders only | Never commit real secrets |

## Quick start

```bash
# One-time toolchain bootstrap (Maven if missing)
./scripts/setup-dev.sh

# Verify foundation builds
./scripts/verify.sh

# Backend (http://localhost:8080)
cd backend && mvn spring-boot:run

# Frontend (http://localhost:5173)
cd frontend && npm run dev
```

## Layout

```
backend/          Spring Boot API + health endpoint
frontend/         React + Vite SPA shell
scripts/          Reproducible setup and verification
docs/             Architecture and phase notes
PROGRESS.md       Phase progress log
QUALITY-GATES.md  Quality / security gates
NEXT-STEPS.md     Remaining phases
```

## Security

- Commit only `.env.example` with safe placeholders.
- Do not commit tokens, passwords, private keys, real `.env` files, logs, `node_modules`, or `target/`.
- See `QUALITY-GATES.md` for the checklist used each phase.

## Documentation

- [PROGRESS.md](./PROGRESS.md) — what landed in each phase
- [QUALITY-GATES.md](./QUALITY-GATES.md) — gates and results
- [NEXT-STEPS.md](./NEXT-STEPS.md) — planned follow-on phases
- [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md) — foundation architecture
