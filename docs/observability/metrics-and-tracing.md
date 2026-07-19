# Metrics and Tracing

## Goals

- Understand portal health and user-impacting failures.
- Detect security anomalies.
- Support performance tuning without collecting sensitive data.

## Metrics

| Metric | Type | Labels | Notes |
|--------|------|--------|-------|
| `http.server.requests` | timer | method, route, status | Use templated route, not raw URL. |
| `portal.auth.login.attempts` | counter | outcome | No email or IP labels. |
| `portal.auth.password_reset.requests` | counter | outcome | Generic outcome labels only. |
| `portal.auth.mfa.challenges` | counter | outcome | Track failures and lockouts. |
| `portal.rbac.denials` | counter | permission, module | Permission code is bounded. |
| `portal.audit.append.failures` | counter | action | Alert immediately. |
| `portal.audit.verify.status` | gauge | status | Last verifier result. |
| `portal.notification.delivery.attempts` | counter | template, outcome | Template code is bounded. |
| `portal.redis.session.active` | gauge | none | If available from Spring Session/Redis. |
| `portal.bootstrap.status` | gauge | status | One-time startup status. |

## Metrics rules

- Do not use email, user ID, IP address, token, or free-form search values as labels.
- Keep label cardinality bounded.
- Prefer counters and timers for request/security flows.
- Alert on absence of expected telemetry from critical services.

## Tracing

Trace boundaries:

- HTTP request entry.
- Authentication verification.
- Authorization decision for privileged operations.
- Database calls where instrumentation supports it.
- Redis calls for session/rate-limit operations.
- SMTP notification delivery.
- Audit append and verifier jobs.

Recommended span attributes:

| Attribute | Example | Notes |
|-----------|---------|-------|
| `portal.module` | `identity` | Bounded module name. |
| `portal.action` | `USER_DISABLED` | Bounded action code. |
| `portal.permission` | `USER_DISABLE` | Only for authorization spans. |
| `portal.outcome` | `success` | Bounded outcome. |
| `enduser.id` | internal UUID | Use only when policy allows; never email. |

## Health checks

| Check | Exposure |
|-------|----------|
| Liveness | Public/load-balancer safe, no details. |
| Readiness | Private or authenticated; includes DB/Redis/mail readiness. |
| Prometheus | Private network only. |

## Alert candidates

- Audit append failure > 0.
- Audit hash verifier mismatch.
- Login failure spike by global rate.
- MFA failure spike.
- Redis unavailable or high latency.
- PostgreSQL connection pool exhaustion.
- 5xx error rate above threshold.
- Notification delivery failures above threshold.

## Implementation status

Dependencies for Spring Boot Actuator, Micrometer Prometheus, and OpenTelemetry are present in `backend/pom.xml`. Application-specific meters, spans, dashboards, and alerts are pending.
