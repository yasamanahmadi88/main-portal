# Accepted Risks

Accepted risks require explicit ownership, expiry, compensating controls, and review. No production security risk has been formally accepted yet.

## Current accepted risks

| ID | Risk | Owner | Compensating controls | Accepted until | Status |
|----|------|-------|-----------------------|----------------|--------|
| _None_ | No accepted security risks yet. | - | - | - | - |

## Candidate risks not yet accepted

| Source | Risk | Current handling |
|--------|------|------------------|
| Cloud Agent / local Docker | Overlay mount prevents container execution in Cursor Cloud | Live verification runs in GitHub Actions `fullstack-verify`; quality gates stay truthful until that job is green. |
| ZAP baseline | Baseline scanner may report informational/low noise on login pages | Artifact retained; `fail_action: false` until triage policy is defined; critical/high must be fixed or justified. |
| ASVS V8.5 / TLS / WAF | Production TLS termination, WAF, and backup immutability are environment controls | Documented in pen-test plan; not claimed VERIFIED from application CI alone. |
| MFA live e2e | Full TOTP enroll/challenge/recovery Playwright path still expanding | Unit + encryption key wiring verified; live MFA scenarios tracked in NEXT-STEPS. |
| Rate-limit burst | Redis rate limiter implemented; automated burst assertion pending | Code path present; add shell burst test before production. |

## Acceptance template

| Field | Required content |
|-------|------------------|
| Risk ID | Stable identifier linked to finding or risk register. |
| Description | What can go wrong and affected assets. |
| Owner | Person or team accountable for review. |
| Severity | Critical, High, Medium, Low. |
| Compensating controls | Concrete controls that reduce likelihood or impact. |
| Expiry | Date when risk must be remediated or re-approved. |
| Approval | Recorded approval channel or ticket. |
