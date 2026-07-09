# Accepted Risks

Accepted risks require explicit ownership, expiry, compensating controls, and review. No production security risk has been formally accepted yet.

## Current accepted risks

| ID | Risk | Owner | Compensating controls | Accepted until | Status |
|----|------|-------|-----------------------|----------------|--------|
| _None_ | No accepted security risks yet. | - | - | - | - |

## Candidate risks not yet accepted

| Source | Risk | Current handling |
|--------|------|------------------|
| Cloud Agent / local Docker | Overlay mount prevents container execution in Cursor Cloud | Live verification runs in GitHub Actions `fullstack-verify` (green as of run 29050258913). |
| ZAP Medium — CSP `style-src 'unsafe-inline'` | Required for Angular Material runtime styles | Documented; scripts remain `script-src 'self'`; no High findings. Triage before production acceptance. |
| ZAP Low — Cookie No HttpOnly on `XSRF-TOKEN` | Double-submit CSRF cookie must be JS-readable | Expected for CookieCsrfTokenRepository; session cookie remains HttpOnly (verified). |
| ASVS V8.5 / TLS / WAF | Production TLS termination, WAF, and backup immutability are environment controls | Documented in pen-test plan; not claimed VERIFIED from application CI alone. |
| MFA live e2e | Full TOTP enroll/challenge/recovery Playwright path still expanding | Unit + encryption key wiring verified; live MFA scenarios tracked in NEXT-STEPS. |
| Observability overlay | Prometheus/Grafana/Loki/Tempo full correlation not started in default fullstack job | Core actuator scrape + log redaction verified; enable `compose.observability.yaml` for full stack. |

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
