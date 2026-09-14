# Production Secrets Management Framework

## Overview

This document provides a comprehensive framework for managing secrets in production deployments of the Enterprise Portal. It covers secret classification, storage requirements, rotation procedures, and CI/CD safety practices.

**Golden Rule:** Never commit `.env` files, credentials, API keys, encryption keys, or TLS certificates to version control. Use a dedicated secrets management platform (AWS Secrets Manager, HashiCorp Vault, Azure Key Vault, sealed-secrets, SOPS, etc.) for all production environments.

## Secret categories

### Database Credentials

| Secret | Purpose | Rotation | Notes |
|--------|---------|----------|-------|
| `POSTGRES_PASSWORD` | Superuser for initial setup | Annually | Used for migrations and DBA tasks. Store in production secret manager only. |
| `POSTGRES_MIGRATION_PASSWORD` | Flyway migration runner | Annually | Elevated privileges for schema changes. Separate from runtime user for principle of least privilege. |
| `POSTGRES_USER_PASSWORD` or `POSTGRES_APP_PASSWORD` | Runtime application user | Annually | Used by the backend at runtime. Should NOT have DROP/ALTER privileges. |

**Best Practice:** Use separate credentials for:
- Initial setup (superuser: `POSTGRES_SUPERUSER` / `POSTGRES_SUPERUSER_PASSWORD`)
- Schema migrations (migration user: `POSTGRES_MIGRATION_USER` / `POSTGRES_MIGRATION_PASSWORD`)
- Application runtime (app user: `POSTGRES_USER` / `POSTGRES_PASSWORD`)

### API Keys and External Service Credentials

| Secret | Service | Purpose | Rotation |
|--------|---------|---------|----------|
| `SMTP_USERNAME` / `SMTP_PASSWORD` | Mail provider | Outbound email delivery | Per provider policy (typically annually) |
| `REDIS_PASSWORD` | Redis cache/session store | Authentication | Annually |
| Future API integrations | Third-party services | Service-to-service auth | Per partner agreement |

### Encryption Keys

| Secret | Purpose | Classification | Rotation |
|--------|---------|-----------------|----------|
| `MFA_ENCRYPTION_KEY_BASE64` | Encrypt TOTP secrets at rest | Restricted | **Annually or on compromise** |
| `MFA_ENCRYPTION_KEY_ID` | Version identifier for active key | Restricted | With MFA key rotation |
| Future: `PAYLOAD_ENCRYPTION_KEY` | Encrypt sensitive data in transit | Restricted | **Annually or on compromise** |

**Key Ring Strategy:** Support multiple MFA encryption keys simultaneously:
- Store all keys as a versioned key ring (e.g., `v1`, `v2`)
- Set `MFA_ENCRYPTION_KEY_ID` to the active key for new encryptions
- Keep retired keys in the ring long enough to decrypt existing factors (see [bootstrap-rotation.md](./bootstrap-rotation.md))
- Remove retired keys after verification window

### Bootstrap Credentials

| Secret | Purpose | Lifetime | Notes |
|--------|---------|----------|-------|
| `BOOTSTRAP_ADMIN_EMAIL` | Initial admin account email | Remove after first login | Set only on first deployment. Read once at startup. |
| `BOOTSTRAP_ADMIN_PASSWORD` | Initial admin password | Remove after password change | Must meet application password policy. Never logged. |
| `BOOTSTRAP_ADMIN_DISPLAY_NAME` | Admin display name | Remove after setup | Set only on first deployment. |

**Critical:** Bootstrap secrets must be removed from environment/secret store immediately after successful first login and secondary admin creation. See [bootstrap-rotation.md](./bootstrap-rotation.md) for rotation and removal procedures.

### TLS Certificates and Keys

| Secret | Purpose | Rotation | Source |
|--------|---------|----------|--------|
| `TLS_CERT` (public) | HTTPS certificate | Per CA policy (≤ 398 days) | Automate via ACME or managed service |
| `TLS_KEY` (private) | HTTPS private key | Per CA policy | Automate renewal to avoid key reuse |
| CA bundle (if needed) | Certificate chain validation | Per CA policy | For client-side TLS verification |

**Best Practice:** Use automated certificate management (Let's Encrypt with ACME automation, AWS Certificate Manager, or equivalent). Never hardcode renewal dates; monitor expiration.

## Storage requirements

### Development (local compose)

- Use `.env` file for local secrets (DO NOT commit to version control)
- Include `.env` in `.gitignore`
- Use `.env.example` as a template with placeholder values
- Share `.env.example` in documentation; developers fill in their own local values

### Staging and Production

Implement one of the following architectures:

#### Option 1: Platform-managed secrets (AWS, Azure, GCP)

```bash
# AWS Secrets Manager example
aws secretsmanager get-secret-value --secret-id portal/prod/db-password

# Inject at container startup via init script
export POSTGRES_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id portal/prod/db-password \
  --query SecretString --output text)
docker compose up
```

**Advantages:**
- Secrets never stored on disk or in .env files
- Audit trail of access
- Automatic rotation hooks
- KMS encryption at rest

#### Option 2: Sealed Secrets (Kubernetes native)

```bash
# Seal secrets for Kubernetes deployment
kubectl create secret generic portal-db \
  --from-literal=password=xxx \
  -o yaml | kubeseal > sealed-db.yaml

# Deploy sealed secret; controller decrypts in-cluster
kubectl apply -f sealed-db.yaml
```

#### Option 3: SOPS (Secrets Operations)

```bash
# Encrypt .env.prod with SOPS
sops --encrypt .env.prod > .env.prod.enc

# Decrypt for deployment
sops --decrypt .env.prod.enc | docker compose --file - up
```

#### Option 4: HashiCorp Vault

```bash
# Store and retrieve secrets
vault kv put secret/portal/prod/db password=xxx
vault kv get secret/portal/prod/db

# Inject via init container or agent
```

**Recommendation for production:** Use your cloud provider's managed secret service (AWS Secrets Manager, Azure Key Vault, Google Secret Manager) or Vault if running on-premises. All options support audit logging, encryption at rest, and automated rotation.

## CI/CD log sanitization

### Threat model

Secrets may leak into CI logs if:
1. Environment variables are echoed or logged
2. Configuration files with embedded secrets are printed
3. Error output contains stack traces with secrets
4. Test output prints credentials

### Mitigation checklist

- [ ] **No echo of sensitive env vars:** Never run `echo $POSTGRES_PASSWORD` or `printenv`
- [ ] **Mask secrets in logs:** Use CI/CD platform's secret masking feature
  - GitHub Actions: `add-mask` step
  - GitLab CI: `MASKED` variable flag
  - Jenkins: CloudBees credentials binding
- [ ] **Redact config dumps:** If logging `.env` or config files, strip secrets first
- [ ] **Filter test output:** Sanitize test reports that may contain credentials
- [ ] **Review CI logs on secret exposure:** If a secret appears in logs, rotate it immediately
- [ ] **Scan for secrets in commits:** Use `git-secrets`, `truffleHog`, or `detect-secrets` in pre-commit hooks
- [ ] **Do not commit `.env` files** — ever. Ensure `.gitignore` includes:
  ```
  .env
  .env.*.local
  .env.prod
  secrets/
  *.key
  *.pem
  *.pfx
  ```

### Example GitHub Actions secret masking

```yaml
- name: Sanitize logs
  env:
    DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
  run: |
    # GitHub Actions automatically masks known secrets
    # For additional masking:
    echo "::add-mask::$(echo $DB_PASSWORD)"
```

## Environment variable conventions

| Variable | Format | Required | Example |
|----------|--------|----------|---------|
| `POSTGRES_PASSWORD` | Base64-encoded if special chars | Yes | `MyP@ssw0rd!` or base64 for special chars |
| `MFA_ENCRYPTION_KEY_BASE64` | Base64 (256-bit = 32 bytes, 44 chars base64) | Yes | `(openssl rand -base64 32)` |
| `MFA_ENCRYPTION_KEY_ID` | Version string (alphanumeric + underscore) | Yes | `v1`, `v2`, `prod-2024-q1` |
| `REDIS_PASSWORD` | Alphanumeric, min 32 chars | If Redis AUTH enabled | `(openssl rand -base64 32)` |
| `BOOTSTRAP_ADMIN_PASSWORD` | Must satisfy application policy | Only on first deployment | See password policy docs |
| `SMTP_USERNAME` / `SMTP_PASSWORD` | Provider-specific | If using SMTP | From mail provider |
| `SESSION_COOKIE_SECURE` | Boolean | Yes in production | `true` |
| `PORTAL_ENV` | `development`, `staging`, `production` | Yes | `production` |

## Access control

### Principle of least privilege

- **Database:** 
  - Migration user: GRANT USAGE ON SCHEMA, CREATE TABLE, ALTER TABLE, DROP TABLE (only on new tables)
  - App user: GRANT SELECT, INSERT, UPDATE on application tables only (NOT `audit_events` DELETE/UPDATE)
  - Separate users prevent accidental data loss
  
- **Secrets manager:**
  - Developers: Read `.env.example` only (no secrets)
  - CI/CD pipelines: Read only necessary secrets (not all credentials)
  - Ops/SRE: Full read-write on secrets with audit logging
  - Application runtime: Read-only on secrets assigned to that environment

- **Encryption keys:**
  - MFA encryption keys: Read-only by application runtime
  - Rotation: Only ops/SRE can generate and promote new keys
  - Retire: Only ops/SRE can remove old keys from ring

### Audit and compliance

- **Secrets access:** Log and alert on any access to production secrets
- **Rotation events:** Document all key/credential rotations with dates and approvers
- **Exposure incidents:** Mandatory rotation within 24 hours if a secret is compromised
- **Periodic review:** Quarterly audit of active secrets and key ring contents

## Checklist for production deployment

- [ ] All secrets stored in a managed secret store (not `.env` or committed files)
- [ ] Database passwords are unique, complex, and separate for migration/app users
- [ ] MFA encryption key(s) generated and stored with version identifiers
- [ ] Bootstrap credentials configured for first deployment only
- [ ] TLS certificates from a trusted CA, auto-renewal configured
- [ ] SMTP/mail credentials securely stored and not hardcoded
- [ ] Redis authentication enabled (if Redis exposed beyond localhost)
- [ ] All CI/CD pipelines mask secrets in logs
- [ ] `.env` and `*.key` / `*.pem` in `.gitignore`
- [ ] Access to production secrets limited to CI/CD service accounts and on-call ops
- [ ] Audit log configured to track secret access and rotations
- [ ] Incident response plan in place for secret compromise (see [../security/incident-response.md](../security/incident-response.md))
- [ ] Documented rotation procedures for all secret categories
- [ ] Annual secrets rotation scheduled and tracked

## Related documentation

- [bootstrap-rotation.md](./bootstrap-rotation.md) — Bootstrap credential rotation and key versioning
- [../operations/key-rotation.md](../operations/key-rotation.md) — Detailed key and credential rotation procedures
- [../security/production-hardening.md](../security/production-hardening.md) — End-to-end production hardening checklist
- [../compliance/data-retention-policy.md](../compliance/data-retention-policy.md) — Retention periods for secrets and audit logs
