# Next Steps

1. Confirm `fullstack-verify` is green on PR #2 and review the `live-stack-evidence` + ZAP artifacts.
2. Expand live MFA enroll/challenge/recovery-code Playwright coverage once the stack job is stable.
3. Add an explicit login rate-limit burst assertion to `live-api-verify.sh`.
4. Run `compose.observability.yaml` in CI (or a scheduled job) to evidence Prometheus/Grafana/Loki/Tempo end-to-end.
5. Schedule an external penetration test per `docs/security/penetration-test-plan.md` before production.
6. Rotate MFA encryption keys and bootstrap credentials for any shared environments.
7. Human review and merge of PR #2 when gates and evidence are acceptable.
