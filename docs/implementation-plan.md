# Implementation Plan

## Goal

Deliver a reusable enterprise portal foundation: session auth, CSRF, MFA, RBAC, bilingual RTL/LTR UI, themes, audit integrity, observability, Docker, and CI — as a modular monolith.

## Phases

| Phase | Scope | Status |
|-------|-------|--------|
| 0 | Repo/env discovery, branch, plans | In progress |
| 1 | Architecture & security design docs | Pending |
| 2 | Repository foundation | Pending |
| 3 | Backend platform | Pending |
| 4 | Identity & authentication | Pending |
| 5 | MFA | Pending |
| 6 | RBAC & administration | Pending |
| 7 | Audit & notifications | Pending |
| 8 | Frontend foundation | Pending |
| 9 | Authentication UI | Pending |
| 10 | Portal UI | Pending |
| 11 | Infrastructure & observability | Pending |
| 12 | CI & scanning | Pending |
| 13 | Hardening | Pending |
| 14 | Final verification | Pending |
| 15 | Pull Request completion | Pending |

## Assumptions

1. Same-origin BFF deployment via nginx in production; Angular proxy in local dev.
2. Public self-registration remains disabled; first admin via one-time bootstrap env vars.
3. Docker may be limited in some Cloud Agent environments; CI validates images.
4. Mailpit is used for non-production email capture.
5. Persian (`fa-IR`) is the default language for first-time users.

## Risks

See `docs/risk-register.md`.
