# Progress

## Current phase

Phases 0–12 largely implemented. Hardening and CI green-up in progress.

## Environment

| Tool | Version / status |
|------|------------------|
| Branch | `cursor/complete-enterprise-portal` |
| Java | Temurin 25.0.3+9 |
| Spring Boot | 4.1.0 |
| Angular | 22.0.5 / Material 22.0.4 |
| Node | 24.15.0 |
| Backend tests (local) | 36 run, 0 fail, 1 skipped — BUILD SUCCESS |
| Frontend tests (local) | 39 passed |
| Frontend build | SUCCESS |
| Compose config | VALIDATED |
| Docker image build (local) | NOT VERIFIED (buildx missing in sandbox) |

## Completed

- Architecture, security docs, ADRs, OpenAPI portal-v1 (53 ops)
- Backend modular platform: identity, MFA, RBAC, audit hash-chain, security events, outbox email, dashboard, settings
- Frontend: bilingual fa-IR/en-US, themes, auth UI, admin UI, CSRF, guards, Playwright smoke
- Docker compose + observability overlay + nginx + CI workflows
- Flyway V1–V6 (incl. Modulith `event_publication`)

## Incomplete / limitations

- Full Testcontainers integration path blocked locally by overlay mount; CI runners have Docker
- Local `docker compose build` blocked (no buildx)
- Some ASVS items remain NOT VERIFIED pending CI/e2e evidence
- Playwright e2e skips without running stack

## Exact next action

Land CI fixes (Spectral ruleset, Trivy 0.35.0, SBOM paths, Modulith migration) and open/update PR targeting `main`.
