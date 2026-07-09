# Architecture — Portal Foundation

## Context

`main-portal` starts as a greenfield repository. The foundation phase establishes a thin vertical slice that is reviewable and buildable, not the full platform.

## Confirmed constraints

- Repository: `yasamanahmadi88/main-portal`
- Base branch: `main`
- First delivery branch: `cursor/portal-foundation`
- Cloud toolchain observed: Java 21, Node 22, Maven installable via apt, Docker absent

## High-level shape

```
browser (Vite/React)
    |  /api/*  (dev proxy → :8080)
Spring Boot API
    |  (later) persistence / auth / modules
```

## Backend decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Runtime | Java 21 | Available in cloud image; Spring Boot 3.4 supports 21; Java 25 not present |
| Framework | Spring Boot + Web + Actuator + Validation | Standard enterprise API baseline |
| Endpoints | `/api/health`, `/api/info` | Prove wiring without domain complexity |
| Config | `application.yml` + env placeholders | 12-factor friendly; secrets stay out of git |
| CORS | Configurable allow-list | Frontend on `:5173` during local/dev |

## Frontend decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Stack | React 19 + Vite + TypeScript | Fast DX, strict typing |
| API access | Relative `/api` + optional `VITE_API_BASE_URL` | Works with Vite proxy and later reverse proxies |
| Tests | Vitest + Testing Library | Lightweight unit coverage for the shell |
| Visual | Teal/steel gradient + Syne / IBM Plex Sans | Brand-forward foundation shell without card clutter |

## Security posture (foundation)

- Commit `.env.example` only
- Ignore `.env`, keys, logs, `node_modules`, `target`, coverage
- Actuator limited to health/info; health details hidden
- No authentication yet (explicitly deferred)

## Buildability

`./scripts/setup-dev.sh` installs missing Maven when possible and npm-installs the frontend.  
`./scripts/verify.sh` runs backend tests/package and frontend lint/test/build.
