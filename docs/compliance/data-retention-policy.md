# Data Retention Policy

**Effective Date:** 2024-01-15  
**Last Updated:** 2024-01-15  
**Next Review:** 2025-01-15  
**Classification:** Internal / Compliance

## Executive Summary

This document defines retention periods for all data types collected, processed, and stored by the Enterprise Portal in production. It ensures compliance with the OWASP Application Security Verification Standard (ASVS) V8.4 (Logging and Error Handling) and applicable privacy regulations.

**Key Principle:** Data is retained for the minimum time necessary to meet business, security, and compliance requirements.

## Regulatory context

### ASVS V8.4 Logging and Auditing

The portal implements ASVS V8.4 controls:
- **V8.4.1:** Ensure all authentication decisions are logged and can be correlated with application session IDs
- **V8.4.2:** Ensure all authentication decisions are logged without storing sensitive credentials
- **V8.4.3:** Ensure security events are logged to the application
- **V8.4.5:** Ensure that access to sensitive data and APIs is logged (or protected)
- **V8.4.6:** Ensure all events that modify data are logged
- **V8.4.7:** Ensure that high-value transactions have a complete, traceable audit trail
- **V8.4.8:** Ensure that all system logging is centralized

**Retention requirement from V8.4:** Audit logs must be retained for a minimum period to detect and investigate security incidents, per organizational policy. Default: **12 months**.

### Privacy regulations

- **GDPR:** Right to erasure; PII must not be retained longer than necessary
- **CCPA:** Users may request deletion of personal information (with exceptions for legal compliance)
- **PIPEDA:** Personal information retention must be limited to identified purposes
- **Local regulations:** Adjust retention as required by jurisdiction

## Data retention periods

### Category: User Account Data

| Data Type | Storage | Retention Period | Rationale | ASVS Reference |
|-----------|---------|------------------|-----------|-----------------|
| Email address | `users` table | Account lifetime + 90 days | Identify user; allow recovery after deletion | V8.4.1 |
| Display name | `users` table | Account lifetime + 90 days | Audit trail readability | V8.4.1 |
| User ID | `users` table | Account lifetime + 90 days | Join key for audit events | V8.4.1 |
| Locale / Timezone | `user_preferences` | Account lifetime + 30 days | Recreate user context; not PII-critical | V8.4.8 |
| Password hash | `user_credentials` | Account lifetime | Never delete while account active; delete on account close | V8.4.2 |
| Last login timestamp | `users` | Account lifetime | Track activity; audit trail | V8.4.1 |

**Deletion flow:**
1. User initiates account deletion (or admin deletes user)
2. User marked as `deleted_at` (soft delete for audit continuity)
3. PII (email, display name) pseudonymized or hashed after 90-day grace period
4. After 7 years: row may be physically deleted (complies with audit retention + legal hold)

### Category: Authentication & Session Data

| Data Type | Storage | Retention Period | Rationale | ASVS Reference |
|-----------|---------|------------------|-----------|-----------------|
| Session ID | Redis / `sessions` table | 30 days after session expiration | Session management; compliance with ASVS V3.2 | V8.4.1 |
| Session token (in cookies) | Client-side | Session lifetime (default 1 hour idle) | Prevent replay; HttpOnly cookie | V8.4.1 |
| MFA enrollment (key_id, encrypted TOTP) | `mfa_factors` table | Account lifetime + 90 days | Recover account if MFA disabled; audit trail | V8.4.1 |
| MFA challenge logs | `security_events` table | 12 months | Detect compromised MFA seeds; incident investigation | V8.4.3 |
| Recovery codes (hashed) | `mfa_recovery_codes` table | Account lifetime + 90 days | Recovery option if MFA lost; audit trail | V8.4.1 |
| Recovery code usage logs | `audit_events` table | 12 months | Detect brute-force attempts; compliance | V8.4.6 |
| Password reset tokens | In-memory / cache | 15 minutes | One-time use; prevent token reuse | V8.4.1 |
| Failed login attempts (transient) | In-memory rate limiter | 1 hour | Rate limiting; prevent brute force | V8.4.3 |
| Failed login logs (persistent) | `security_events` table | 12 months | Detect account compromise; alert on spikes | V8.4.3 |

### Category: Audit and Security Events

| Data Type | Storage | Retention Period | Rationale | ASVS Reference |
|-----------|---------|------------------|-----------|-----------------|
| Audit events (all actions) | `audit_events` table (append-only) | **12 months (minimum)** | ASVS V8.4 requirement; incident investigation; compliance | V8.4.1-8 |
| Security events (logins, MFA, etc.) | `security_events` table | **12 months (minimum)** | Threat detection; anomaly analysis; incident response | V8.4.3 |
| Admin actions (RBAC changes, data exports) | `audit_events` table with role context | **36 months (minimum)** | Governance; regulatory compliance; audit trails for privileged actions | V8.4.6 |
| Failed authentication attempts | `security_events` table | 12 months | Detect patterns; lockout policies; alert on spikes | V8.4.3 |
| Privilege changes (role grants/revokes) | `audit_events` table | **36 months** | Track authorization changes; compliance audits | V8.4.6 |
| Data exports and exports logs | `audit_events` table | **36 months** | Regulatory; detect data exfiltration | V8.4.6 |
| Permission denial events | `audit_events` table | 12 months | Detect unauthorized access attempts | V8.4.5 |
| System configuration changes | `audit_events` table | 24 months | Track deployments; rollback references | V8.4.8 |

**Immutability requirement (ASVS V8.4.1):** Audit events must be append-only and protected against deletion. Production audit tables must have a trigger to deny UPDATE/DELETE on regular app users:

```sql
-- Restrict non-superuser access to audit tables
REVOKE UPDATE, DELETE ON audit_events FROM portal_app;
REVOKE UPDATE, DELETE ON security_events FROM portal_app;
```

### Category: Error Logs and Technical Logs

| Data Type | Storage | Retention Period | Rationale | ASVS Reference |
|-----------|---------|------------------|-----------|-----------------|
| Application error logs (DEBUG level) | Elasticsearch / Loki | 7 days | Troubleshooting; does not contain PII if implemented correctly | V8.4.1 |
| Application logs (INFO level) | Elasticsearch / Loki | 30 days | Operational trends; compliance with V8.4 | V8.4.8 |
| HTTP access logs (nginx) | File storage | 30 days | Troubleshooting; not typically PII if headers redacted | V8.4.8 |
| Database query logs | Disabled in production | N/A | Data minimization; can expose PII; query performance use tools instead | V8.4.2 |
| JVM garbage collection logs | File storage | 7 days | Performance analysis; not security-critical | N/A |

**Filtering requirement:** Error logs and technical logs must never contain:
- Passwords or hashed credentials
- Session IDs or cookies
- CSRF tokens
- PII (email, user IDs in context where not necessary)

### Category: Trace and Metric Data

| Data Type | Storage | Retention Period | Rationale | ASVS Reference |
|-----------|---------|------------------|-----------|-----------------|
| Distributed traces (Tempo) | Tempo backend storage | 48 hours (default) | Performance analysis; does not require long retention | N/A |
| Metrics (Prometheus) | Prometheus time-series DB | 15 days (configurable) | Operational dashboards; aggregate data | N/A |
| Traces with PII attributes | Must be redacted | 24 hours max | Never retain user emails, IDs, passwords as trace attributes | V8.4.2 |

**Label cardinality guard:** Metrics labels must never include:
- User emails or IDs
- Session IDs
- API keys or tokens
- Passwords

### Category: Compliance and Legal Holds

| Data Type | Retention Period | Rationale |
|-----------|------------------|-----------|
| Archived audit logs (for legal disputes) | **7 years** | Regulatory; litigation support; assume any audit event may be needed |
| Data retention policy change logs | 7 years | Prove compliance with policy over time |
| Penetration test reports and findings | 3 years | Security baseline; regulatory evidence |
| Incident response reports (post-mortem) | 3 years | Prevent repeat incidents; compliance audits |
| Security scanning results (SAST, dependency) | 1 year | Track remediation; regulatory compliance |

**Legal hold:** If a user dispute, regulatory investigation, or litigation is initiated, all related data must be immediately placed on legal hold and retained indefinitely until cleared by legal counsel.

## Data deletion procedures

### User-initiated account deletion

```
Request received → Grace period (90 days)
  ├─ User can cancel and restore account
  └─ After 90 days:
      ├─ Pseudonymize PII (email → hash, display_name → "Deleted User")
      ├─ Mark deleted_at in users table
      ├─ Retain user_id for audit trail joins
      ├─ Audit events retain user_id, not email
      └─ After 7 years: Physical deletion (per audit retention policy)
```

### Admin-initiated user deletion

Similar to user-initiated, with additional logging of who deleted the account and why.

### Session expiration

- Sessions expire after configured idle timeout (default: 1 hour)
- Session record deleted from Redis immediately
- If persistent session storage used: delete after 30 days
- Audit event retained for login audit trail

### Password reset token expiration

- Reset tokens generated with 15-minute TTL
- Never stored in database; held only in memory/cache
- Automatic expiration via cache eviction
- Log password reset attempt in audit trail (redacted token)

### Audit log retention enforcement

```bash
# Script to archive and purge audit logs older than retention period
# Run daily via cron/scheduled job

# Example: Delete security_events older than 12 months
psql -U portal_migration -d portal -c \
  "DELETE FROM security_events WHERE created_at < NOW() - INTERVAL '12 months';"

# Archive to cold storage (S3, GCS, etc.) first if long-term compliance needed
pg_dump -U portal_migration -d portal -t security_events \
  --where="created_at < NOW() - INTERVAL '11 months'" \
  | gzip > /archive/security_events_$(date +%Y%m%d).sql.gz
```

## Compliance checklist

### Before first production deployment

- [ ] Data retention policy approved by legal and compliance teams
- [ ] GDPR Data Processing Agreement (DPA) signed (if EU customers)
- [ ] CCPA requirements assessed and documented
- [ ] Audit table immutability enforced (no UPDATE/DELETE on `portal_app` user)
- [ ] Log sanitization tested (no PII/secrets in app logs, traces, metrics)
- [ ] Retention enforcement script deployed and scheduled
- [ ] Monitoring alerts configured for audit table size and growth
- [ ] Backup/archive procedures tested for long-term audit retention
- [ ] Incident response playbook includes data preservation procedures

### Annual compliance review

- [ ] Policy reviewed for regulatory changes
- [ ] Retention periods validated against new requirements
- [ ] Audit logs spot-checked for PII/secrets leakage
- [ ] Archive and purge processes verified
- [ ] Storage costs and capacity planned for next year
- [ ] Legal holds documented and reviewed

## Compliance signoff

### Initial Deployment Approval

```
Prepared by: _________________ (Data/Security Officer)
Date: _________________
Approved by: _________________ (Compliance Officer)
Date: _________________
Legal Review: _________________ (General Counsel or Legal Team)
Date: _________________
```

### Annual Review Signoff

```
Reviewed by: _________________ (Data Protection Officer / CISO)
Date: _________________
Compliance status: [ ] Pass  [ ] Pass with exceptions  [ ] Fail
Exceptions noted: _________________________________
Approved by: _________________ (Compliance Lead)
Date: _________________
Next review scheduled: _________________
```

## Monitoring and alerting

### Retention compliance dashboards

Track via Grafana:
- Audit event table row count and daily growth rate
- Age distribution of events (% > 12 months)
- Failed deletion jobs (if automated purge fails)
- Storage usage and forecast

### Alerts

| Condition | Severity | Action |
|-----------|----------|--------|
| Audit table row count exceeds 10M | High | Check if purge job ran; may need to scale storage |
| Audit events > 12 months not deleted by cutoff date | Critical | Immediate investigation; may violate compliance |
| Deletion job fails 3x in a row | High | Page on-call DBA; restore from backup if necessary |
| PII detected in application logs (pattern match) | Critical | Security incident; review logging code |

## References and related documents

- [../security/asvs-5-checklist.md](../security/asvs-5-checklist.md) — ASVS V8.4 implementation evidence
- [../security/data-classification.md](../security/data-classification.md) — Data classification levels and inventory
- [../operations/deployment.md](../operations/deployment.md) — Backup and restore procedures
- [../security/incident-response.md](../security/incident-response.md) — Incident response and data preservation
- [secrets-management.md](./secrets-management.md) — Secrets retention (typically minimal)
- [bootstrap-rotation.md](./bootstrap-rotation.md) — Bootstrap credential removal procedures

## Questions and feedback

**Data privacy questions?** Contact: [privacy@example.com](mailto:privacy@example.com)  
**Compliance audit?** Contact: [compliance@example.com](mailto:compliance@example.com)  
**To update this policy:** File an issue or contact the Data Protection Officer
