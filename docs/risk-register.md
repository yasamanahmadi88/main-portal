# Risk Register

| ID | Risk | Impact | Mitigation | Status |
|----|------|--------|------------|--------|
| R1 | Docker daemon unavailable or restricted in Cloud Agent | Cannot fully verify compose locally | Validate compose syntax; rely on CI build/scan jobs | Open |
| R2 | Node engine mismatch (Angular 22 wants >=22.22.3) | Frontend install/build failures | Use Node 24.15.0 in local/CI | Mitigated |
| R3 | Spring Boot 4.1 API surface differences vs 3.x docs | Incorrect config/security APIs | Prefer Boot 4 docs; compile early | Open |
| R4 | Testcontainers needing Docker | Integration tests fail without Docker | Start dockerd when possible; mark gates NOT VERIFIED if blocked | Open |
| R5 | MFA encryption key mismanagement | Secret leakage or unreadable MFA secrets | Env-only keys, key IDs, never commit | Mitigated by design |
| R6 | Scope breadth vs time | Incomplete quality gates | Prioritize security-critical paths; evidence-based gate status | Open |
| R7 | Redis session + CSRF cookie naming across environments | Auth breakage | Separate dev/prod cookie config; e2e coverage | Open |
| R8 | Final SUPER_ADMIN protection edge cases | Lockout or privilege loss | Explicit policy tests | Open |
