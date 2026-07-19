# Comparison: `secure-portal` vs `main-portal`

Date: 2026-07-19  
Compared:

| Repository | Branch / tip |
|------------|--------------|
| [yasamanahmadi88/secure-portal](https://github.com/yasamanahmadi88/secure-portal) | `cursor/enterprise-portal-foundation-6dc2` |
| [yasamanahmadi88/main-portal](https://github.com/yasamanahmadi88/main-portal) | `main` (consolidated from `cursor/complete-enterprise-portal`) |

## Verdict

**`main-portal` is the advanced, production-oriented evolution of both codebases.**  
`secure-portal` remains a solid foundation; it is not a strict security superset. No critical security/login capability needs to be ported from B → A for correctness.

## Capability matrix

| Capability | secure-portal | main-portal |
|------------|---------------|-------------|
| Session auth + CSRF (no browser JWT) | Yes | Yes (hardened: session fixation migrate, Redis SecurityContext persist) |
| Argon2id passwords | Yes | Yes |
| MFA TOTP + recovery codes | Yes | Yes (Redis challenge before session; multi-key MFA ring) |
| RBAC + seeded roles | Yes | Yes (+ USER_MANAGER / ROLE_MANAGER / SUPPORT, SUPER_ADMIN final-admin guards) |
| Audit trail | Insert-only style | Hash-chain + append-only DB grants + integrity API |
| Live CI stack verify | Basic CI | `fullstack-verify` (compose, RBAC matrix, audit DB, Playwright+Axe, ZAP, rate-limit) |
| Security headers / nginx rate zones | Partial | Full CSP/COOP/CORP + login zone |
| OpenAPI contract | Limited | `contracts/openapi/portal-v1.yaml` + Spectral |
| Observability overlay | Monitoring profile | Prometheus/Grafana/Loki/Tempo compose overlay |
| ASVS evidence discipline | Checklist present | Checklist + green live CI links |

## Intentionally ported from secure-portal

- `docs/operations/key-rotation.md` (adapted for MFA key ring)
- `docs/operations/incident-response.md`
- `docs/operations/backup-restore.md`
- `compose.dev.yaml` (dev host-port overlay)

## Not ported (weaker or redundant in secure-portal)

- `sessionFixation().none()`
- Progressive `Thread.sleep` login delay (A already has Redis + nginx limits)
- Single-key MFA encryptor with zero-key fallback
- Fail-open audit writes

## Remaining limitations (truthful)

- Dedicated live MFA enroll/challenge/recovery Playwright suite still expanding
- Full Tempo/Loki correlation requires `compose.observability.yaml`
- Production TLS/WAF/backup immutability remain environment controls
- External pen-test still required before production go-live
