# Accepted Risks

Accepted risks require explicit ownership, expiry, compensating controls, and review. This file starts empty because no security risk has been formally accepted for production.

## Current accepted risks

| ID | Risk | Owner | Compensating controls | Accepted until | Status |
|----|------|-------|-----------------------|----------------|--------|
| _None_ | No accepted security risks yet. | - | - | - | - |

## Candidate risks not yet accepted

| Source | Risk | Current handling |
|--------|------|------------------|
| `docs/risk-register.md` R1 | Docker verification may be blocked in some environments | Keep quality gate NOT VERIFIED until CI or local evidence exists. |
| `docs/risk-register.md` R5 | MFA encryption key mismanagement | Mitigate by external key management and key IDs; not accepted as residual production risk. |
| `docs/risk-register.md` R7 | Redis session and CSRF cookie naming across environments | Requires environment-specific tests before production. |
| `docs/risk-register.md` R8 | Final `SUPER_ADMIN` edge cases | Requires explicit authorization tests before production. |

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
