# Next Steps

1. Human review of PR #2 using green `fullstack-verify` evidence (run 29050258913 / 29050262164) and ZAP artifact triage.
2. Expand live MFA enroll/challenge/recovery-code Playwright coverage.
3. Add explicit before/after session-id rotation assertion to `live-api-verify.sh`.
4. Run `compose.observability.yaml` in CI (or a scheduled job) to evidence Prometheus/Grafana/Loki/Tempo end-to-end.
5. Decide acceptance for ZAP Medium `style-src 'unsafe-inline'` (Angular Material) or harden with nonces/hashes if feasible.
6. Schedule an external penetration test per `docs/security/penetration-test-plan.md` before production.
7. Rotate MFA encryption keys and bootstrap credentials for any shared environments.
8. Merge only after human security/product review — do not auto-merge.
