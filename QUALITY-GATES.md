# Quality Gates

Gates that must pass before a phase is considered complete.

## Gate checklist — portal-foundation

| Gate | Requirement | Result |
|------|-------------|--------|
| G1 Repo identity | Work only in `yasamanahmadi88/main-portal`, base `main` | PASS |
| G2 Branch policy | Feature branch `cursor/portal-foundation`; no force-push; no main rewrite | PASS |
| G3 Secrets hygiene | No tokens/keys/real `.env`; only `.env.example` placeholders | PASS |
| G4 Ignore hygiene | `.gitignore` covers `node_modules`, `target`, logs, coverage, `.env` | PASS |
| G5 Backend build | `mvn test` and `mvn package` succeed | PASS |
| G6 Frontend build | `npm run lint`, `npm test`, `npm run build` succeed | PASS |
| G7 Docs | `PROGRESS.md`, `QUALITY-GATES.md`, `NEXT-STEPS.md` present | PASS |
| G8 Buildable HEAD | Repository remains buildable after the phase commit | PASS |

## Commands executed (this phase)

```bash
./scripts/setup-dev.sh
./scripts/verify.sh
# Equivalent explicit commands after fix:
cd backend && mvn -B -q test && mvn -B -q -DskipTests package
cd frontend && npm run lint && npm test && npm run build
```

## Test results

| Suite | Result |
|-------|--------|
| Backend `PortalApplicationTests` (context + `/api/health` + `/api/info`) | PASS |
| Frontend Vitest `App` brand + API status | PASS (1 test) |
| Frontend `tsc --noEmit` | PASS |
| Frontend Vite production build | PASS |
| Backend `mvn package` | PASS |

## Security checks (foundation)

- [x] No committed secrets or credential files
- [x] `.env` generated locally from example and gitignored
- [x] Actuator exposure limited to `health` and `info`
- [x] Health details set to `never`
- [x] CORS origins configurable via env (default localhost Vite)
- [x] `npm audit` reviewed (5 advisories in transitive deps; remediation deferred to hardening phase — no force upgrades in foundation)
- [ ] OWASP Dependency-Check / full SCA — scheduled for later phase
- [ ] Container image scan — blocked until Docker is available in the environment

## Tooling gaps recorded

| Tool | Status |
|------|--------|
| Java 25 | Not installed; foundation uses OpenJDK 21 |
| Docker | Not installed; compose deferred |
| Maven | Installed via apt; bootstrap scripted |

## How to re-run

```bash
./scripts/setup-dev.sh
./scripts/verify.sh
```
