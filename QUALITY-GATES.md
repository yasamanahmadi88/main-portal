# Quality Gates

| Gate | Status | Evidence |
|------|--------|----------|
| Repository structure | PASS | Root layout |
| Java 25 compile + tests | PASS | CI backend job |
| Angular 22 build + unit tests | PASS | CI frontend job |
| Architecture / Modulith tests | PASS | CI + local |
| OpenAPI lint | PASS | Spectral CI |
| Flyway migrations | PASS | CI contextLoads + V1–V6 |
| Redis indexed sessions | PASS | CI contextLoads |
| CSRF / session auth implemented | PASS (code) | SecurityConfig + AuthController |
| MFA / RBAC / audit unit evidence | PASS | Dedicated unit tests |
| Full e2e / axe live | NOT VERIFIED | Playwright skips without stack |
| Compose config | PASS | CI + local |
| Container build + Trivy CRITICAL | PASS | container-build workflow |
| SBOM | PASS | sbom workflow |
| Secret scan | PASS | Gitleaks CI |
| No committed secrets | PASS | placeholders only |
| ASVS full evidence | PARTIAL | checklist present |
| Pull Request | PASS | https://github.com/yasamanahmadi88/main-portal/pull/2 |
