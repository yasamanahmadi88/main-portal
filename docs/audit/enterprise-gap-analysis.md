# Enterprise Portal — Gap Analysis

**Date:** 2026-07-19
**Repository tip audited:** `1af0a72` (`main`)
**Upgrade branch:** `cursor/enterprise-final-upgrade-4bed`
**Method:** AUDIT → ANALYZE → COMPARE (no production rewrite)

---

## 1. Repository & branch state

| Branch | Relation to `main` | Verdict |
|--------|--------------------|---------|
| `main` | Tip; consolidated enterprise portal | **Keep** |
| `origin/cursor/complete-enterprise-portal` | 0 ahead / 3 behind `main` (fully merged via PR #2) | Obsolete — safe to delete after upgrade PR |
| `origin/cursor/portal-foundation` | 1 unique commit (React/Vite scaffold); superseded by Angular 22 portal | Obsolete — no valuable commits to retain |

**PRs:** PR #2 merged (feat complete portal); PR #1 closed (foundation).

**Conclusion:** Single upgrade branch from current `main`. No commit salvage required from obsolete remotes.

---

## 2. Current state summary (strengths)

| Area | Status |
|------|--------|
| Stack | Java 25, Spring Boot 4.1.0, Angular 22, Material 22, PostgreSQL, Redis, Flyway |
| Auth | Session + CSRF, Argon2id, TOTP MFA, lockout, login rate limit |
| RBAC | Deny-by-default `hasAuthority`, SUPER_ADMIN guards |
| Audit | Append-only hash chain + integrity API |
| i18n | fa-IR default, en-US, RTL/LTR, 333-key parity |
| Theme | LIGHT / DARK / SYSTEM + FOUC bootstrap |
| Docker | Multi-stage non-root images, compose networks, healthchecks |
| CI | Build/test, Gitleaks, Trivy, SBOM, fullstack-verify, ZAP |
| Live gates | Green `fullstack-verify` evidence recorded in `QUALITY-GATES.md` |

---

## 3. Gap analysis — Current vs Required Enterprise State

| # | Requirement | Current | Gap | Priority |
|---|-------------|---------|-----|----------|
| G1 | CAPTCHA on authentication | Absent (UI + API + contract) | Full CAPTCHA lifecycle | **P0** |
| G2 | `/monitoring` permissioned UI | Absent; `observability` module stub | Permissions + API + SPA | **P0** |
| G3 | `MONITORING_*` RBAC | Not in enum/SQL/OpenAPI | V10 seed + enforcement | **P0** |
| G4 | Navbar: security + monitoring shortcuts | Notifications stub; no monitoring | Extend topbar/user menu | **P1** |
| G5 | Premium enterprise UI polish | Solid Material shell; hardcoded whites; incomplete Mat sys bridge | Design-token completion | **P1** |
| G6 | Theme: no hardcoded component colors | Brand/auth/profile use `#fff` | Replace with tokens | **P1** |
| G7 | Full i18n (no English prompts) | `prompt('Confirm your password')` | Dialog + keys | **P1** |
| G8 | Rate-limit buckets wired | Only login bucket used; fail-open on Redis down | Wire reset/MFA; fail-closed option | **P1** |
| G9 | Auth metrics for Grafana | Dashboard queries non-existent series | Micrometer counters | **P1** |
| G10 | Observability overlay verified | Overlay exists; not in CI; Prometheus UID missing | Fix datasource UID + verify script | **P1** |
| G11 | Docker-first DX docs | README still lists host Java/Node first | Reorder docs; fix stale compose claims | **P2** |
| G12 | Security permission bug | SPA checks `SECURITY_EVENT_READ` vs API `SECURITY_READ` | Align to `SECURITY_READ` | **P0** |
| G13 | CAPTCHA + monitoring tests | Missing | Unit/IT/e2e/a11y | **P0** |
| G14 | Image publish/signing | Build-only, no GHCR push/cosign | Optional hardening | **P2** |
| G15 | External pen-test / MFA e2e depth | Documented residual | Keep as residual | Residual |

---

## 4. Architecture decisions for the upgrade (no rewrite)

1. **CAPTCHA:** Self-hosted SVG challenge in Redis (TTL, one-time consume). No third-party keys required for docker-first. Answer reveal only via `CAPTCHA_REVEAL_ANSWER` for test/e2e profiles — forbidden in `prod`.
2. **Monitoring:** Portal `/monitoring` aggregates safe health/metric summaries via authenticated API (`monitoring:*` authorities). Prometheus scrape on mgmt port 8081 remains network-isolated; SPA never exposes secrets.
3. **Branch policy:** One upgrade branch from `main`; delete obsolete remotes after merge verification.
4. **Preserve:** All existing features, CSRF, session model, audit chain, bilingual UX, compose stack.

---

## 5. Implementation phases mapped to gaps

| Phase | Gaps closed |
|-------|-------------|
| 3–6 UI/theme/i18n/navbar | G4–G7, G12 |
| 7 CAPTCHA | G1, G13 |
| 8 AuthZ polish | G8, G12 |
| 9 Monitoring | G2, G3, G9, G10 |
| 10–13 Docker/security/tests/CI | G10, G11, G13, G14 |
| 14 Verification | Evidence in PROGRESS / QUALITY-GATES |

---

## 6. Explicit non-goals

- Rewriting the modular monolith or replacing Angular/Spring stacks
- Storing JWTs in browser storage
- Disabling CSRF
- Auto-merging to `main` without human review
