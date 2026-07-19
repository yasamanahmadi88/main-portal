# Next Steps

1. Human review of the enterprise final upgrade PR; merge only after security/product review.
2. Confirm green `fullstack-verify` on the upgrade branch (CAPTCHA-aware login + monitoring permissions).
3. Optionally add a scheduled CI job that starts `compose.observability.yaml` and asserts Prometheus/Grafana/Loki/Tempo + SPA `/monitoring`.
4. Expand live MFA enroll/challenge/recovery-code Playwright coverage.
5. Add explicit before/after session-id rotation assertion to `live-api-verify.sh`.
6. Decide acceptance for ZAP Medium `style-src 'unsafe-inline'` (Angular Material) or harden with nonces/hashes if feasible.
7. Schedule an external penetration test per `docs/security/penetration-test-plan.md` before production.
8. Rotate MFA encryption keys and bootstrap credentials for any shared environments.
9. After merge: keep only `main` (+ short-lived upgrade branch until merge); delete obsolete feature remotes.
