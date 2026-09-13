# Bootstrap Credential Rotation Procedure

## Overview

This document describes procedures for rotating bootstrap admin credentials and managing MFA encryption key versions in production. Bootstrap credentials are single-use secrets that provision the initial admin account on first deployment. MFA encryption keys must be rotated annually or on compromise, with versioning support for seamless key ring transitions.

## Bootstrap credentials lifecycle

### First deployment (initial setup)

1. **Generate bootstrap credentials:**
   ```bash
   BOOTSTRAP_EMAIL="admin@example.com"
   BOOTSTRAP_PASSWORD=$(openssl rand -base64 32 | tr -d '=' | head -c 32)
   BOOTSTRAP_DISPLAY_NAME="Portal Administrator"
   
   # Validate password meets application policy (min 12 chars, complexity)
   ```

2. **Store credentials securely:**
   - Add to production secret manager (not `.env`):
     ```bash
     vault kv put secret/portal/prod/bootstrap \
       email="$BOOTSTRAP_EMAIL" \
       password="$BOOTSTRAP_PASSWORD" \
       display_name="$BOOTSTRAP_DISPLAY_NAME"
     ```
   - Or AWS Secrets Manager / Azure Key Vault equivalent

3. **Deploy with bootstrap enabled:**
   ```bash
   docker compose up -d
   # Backend starts, reads bootstrap variables once, creates admin account
   # Check logs: "SUPER_ADMIN user created successfully"
   ```

4. **Verify bootstrap account created:**
   ```bash
   # Log in to portal with BOOTSTRAP_EMAIL and BOOTSTRAP_PASSWORD
   # Check Audit Logs for successful login
   # Enable MFA on the initial account (required for production)
   # Create additional admin accounts
   ```

5. **Remove bootstrap credentials immediately:**
   ```bash
   # From secret manager:
   vault delete secret/portal/prod/bootstrap
   # Or: `aws secretsmanager delete-secret --secret-id portal/prod/bootstrap`
   
   # From environment (backend will not re-run bootstrap if users exist):
   unset BOOTSTRAP_ADMIN_EMAIL BOOTSTRAP_ADMIN_PASSWORD BOOTSTRAP_ADMIN_DISPLAY_NAME
   docker compose up -d backend  # Redeploy to clear from running container
   ```

6. **Verify bootstrap no longer runs:**
   ```bash
   # Check backend logs on next deployment
   docker compose logs backend | grep -i bootstrap
   # Should see: "Skipping bootstrap: users table is not empty"
   ```

### Secondary admin creation (after bootstrap)

Once bootstrap is removed, create additional admins through the portal UI:

1. **Log in as initial admin**
2. **Navigate to Admin → User Management → Create User**
3. **Set role to SUPER_ADMIN or appropriate role**
4. **Send temporary password to new admin (via secure channel)**
5. **New admin logs in, changes password immediately**
6. **New admin enables MFA**

## MFA encryption key versioning

### Key inventory and versioning scheme

The portal supports a **key ring** model where multiple MFA encryption keys can coexist:

| Key Version | Format | Status | Usage | Generated |
|-------------|--------|--------|-------|-----------|
| `v1` | Base64 (256-bit AES) | Active | New enrollments; decrypt existing | 2024-01-15 |
| `v2` | Base64 (256-bit AES) | Pending | Shadows v1 during rotation | 2024-06-15 |
| (retired) | Base64 (256-bit AES) | Archived | Decrypt only, no new encryptions | After 30-day window |

### Planned rotation (annual or scheduled)

**Goal:** Transition from active key v1 to v2 without disrupting user MFA access.

#### Step 1: Generate new key

```bash
# Generate a new 256-bit (32-byte) key
NEW_KEY=$(openssl rand -base64 32)
NEW_KEY_ID="v2"

echo "NEW_KEY=$NEW_KEY"
echo "NEW_KEY_ID=$NEW_KEY_ID"

# Store securely (do not commit to version control)
# Add to vault/Secrets Manager with metadata:
vault kv put secret/portal/prod/mfa-keys \
  "v2=$NEW_KEY" \
  "v2_created_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  "v2_purpose=rotation_2024_q3"
```

#### Step 2: Deploy with both keys in the key ring

The application configuration (`application.yml` or environment mapping) defines the key ring:

```yaml
portal:
  mfa:
    encryption:
      keys:
        v1: "${MFA_ENCRYPTION_KEY_V1}"
        v2: "${MFA_ENCRYPTION_KEY_V2}"
      # Active key for new enrollments (changed after validation)
      activeKeyId: "v1"
```

Environment mapping (example for Spring Boot):
```
PORTAL_MFA_ENCRYPTION_KEYS_V1=<existing v1 key base64>
PORTAL_MFA_ENCRYPTION_KEYS_V2=<new v2 key base64>
PORTAL_MFA_ENCRYPTION_ACTIVEKEY_ID=v1  # Not changed yet
```

**Deploy new configuration:**
```bash
# Add v2 to secret manager alongside v1
vault kv put secret/portal/prod/mfa-keys \
  "v1=$(vault kv get -field=v1 secret/portal/prod/mfa-keys)" \
  "v2=$NEW_KEY"

# Update application config to include both keys
docker compose up -d backend
# Backend now has both keys; can decrypt all existing factors + new enrollments still use v1
```

#### Step 3: Validate decryption of existing factors

Test that all existing MFA factors still decrypt correctly:

```bash
# Test MFA challenge for a user with v1-encrypted factor
# In the portal:
# 1. Log in as test user
# 2. Navigate to Security → MFA
# 3. Trigger MFA challenge
# 4. Verify TOTP code is accepted

# Automated verification (if available):
scripts/test-mfa-factors.sh --validate-all

# Check audit log for successful challenge:
# SELECT * FROM audit_events WHERE event_type='MFA_CHALLENGE_SUCCESS' 
#   AND created_at > NOW() - INTERVAL '5 minutes'
```

#### Step 4: Update active key (promote v2)

Once validated, promote v2 to active for new enrollments:

```bash
# Update application configuration
# Change PORTAL_MFA_ENCRYPTION_ACTIVEKEY_ID to v2
export MFA_ENCRYPTION_KEY_ID="v2"

docker compose up -d backend
# Backend now encrypts new MFA enrollments with v2
# Existing factors still use v1 (stored in database key_id field)
```

**Verification:**
```bash
# New MFA enrollment should use v2:
# 1. Disable MFA on a test account
# 2. Re-enroll with TOTP (new factor)
# 3. Query database:
#    SELECT key_id, encrypted_secret FROM mfa_factors 
#      WHERE user_id = <test_user_id>
# 4. Verify new factor has key_id = 'v2'
```

#### Step 5: Optional—force re-encryption of privileged users

For high-privilege users, consider forcing re-enrollment to migrate all factors to v2:

```bash
# Admin action: Mark specific users for MFA re-enrollment
# In admin panel or via API:
POST /admin/api/users/<user-id>/mfa/force-reenroll

# User is forced to re-enroll MFA on next login
# New factors are encrypted with active key (v2)
```

#### Step 6: Retire old key (after 30-day validation window)

After 30 days of operation with v2 active and all privileged users migrated:

```bash
# Remove v1 from key ring
vault kv put secret/portal/prod/mfa-keys \
  "v2=$NEW_KEY" \
  # (omit v1)

# Update application configuration to remove v1
# PORTAL_MFA_ENCRYPTION_KEYS_V1 unset or removed

docker compose up -d backend

# Log retirement in audit system
vault audit log-write \
  "MFA key v1 retired. Last decryption: <date>. No factors remain with v1."
```

### Emergency rotation (key compromise)

**Threat:** A key is leaked, must be rotated immediately without the normal 30-day validation window.

#### Step 1: Declare SEV-1 incident

```bash
# Open incident ticket
# Notify on-call security team
# Initiate incident response (see ../security/incident-response.md)
```

#### Step 2: Generate and deploy emergency key immediately

```bash
EMERGENCY_KEY=$(openssl rand -base64 32)
EMERGENCY_KEY_ID="v3_emergency_$(date +%s)"

# Deploy with emergency key
vault kv put secret/portal/prod/mfa-keys \
  "v1=$(vault kv get -field=v1 secret/portal/prod/mfa-keys)" \
  "$EMERGENCY_KEY_ID=$EMERGENCY_KEY" \
  "emergency_rotation_date=$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  "emergency_rotation_reason=Suspected key compromise"

# Update app config
export MFA_ENCRYPTION_KEY_ID="$EMERGENCY_KEY_ID"
docker compose up -d backend
```

#### Step 3: Assess MFA bypass vs. re-enrollment

**Option A: Temporary MFA bypass (if immediate availability required)**
- Disable MFA for all users (if authorized by incident commander)
- Send password reset notices
- Users re-enroll MFA once MFA service is stable
- **Risk:** Users may skip re-enrollment

**Option B: Forced MFA re-enrollment (recommended for security)**
- Keep MFA active but mark all factors invalid
- Users re-enroll on next login
- New factors encrypt with emergency key
- Audit logs clearly mark forced re-enrollment
- **Downside:** Temporary access friction

**Recommendation:** Choose Option B (forced re-enrollment) unless business impact requires Option A.

#### Step 4: Full security review

```bash
# Audit and compliance review:
# - Who had access to compromised key v1?
# - When was it accessed? (check secrets manager audit log)
# - Was key ever used in CI/CD logs?
# - Were any TOTP secrets extracted?

# Expand investigation:
# - Check for unauthorized MFA factors created
# - Review login logs for suspicious activity
# - Verify no lateral movement occurred
# - Check for data exfiltration (audit logs, export downloads)
```

#### Step 5: Root cause and remediation

- Determine how key was exposed (leaked in CI log? shared accidentally? code repository?)
- Implement fixes (CI log masking, stricter access controls, rotation automation)
- Document in postmortem

## Bootstrap + MFA key rotation coordination

When rotating both bootstrap and MFA keys in the same maintenance window:

1. **First:** Rotate MFA encryption keys (per steps above)
2. **Then:** Remove bootstrap credentials
3. **Document:** Timestamp and approved-by for audit

```bash
# Single maintenance action log
vault kv put secret/portal/prod/maintenance-log \
  "rotation_timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  "actions_taken=mfa_key_v1_to_v2_rotation,bootstrap_credentials_removed" \
  "approved_by=security@example.com" \
  "jira_ticket=SEC-1234"
```

## Verification checklist

- [ ] **Bootstrap removal:** Secret manager no longer has bootstrap credentials
- [ ] **Bootstrap verification:** Backend logs confirm "Skipping bootstrap: users table is not empty"
- [ ] **MFA key v2 active:** New MFA enrollments use v2 (check database)
- [ ] **MFA backward compatibility:** Old factors (v1) still decrypt and validate correctly
- [ ] **Admin access:** Log in with at least two admin accounts, verify full portal access
- [ ] **MFA challenge:** Trigger MFA challenge, verify TOTP codes accepted
- [ ] **Audit logs:** Rotation events recorded with timestamps and approver
- [ ] **Incident tickets:** Related security tickets resolved or marked as completed
- [ ] **CI/CD logs reviewed:** No secrets exposed in build/deployment logs
- [ ] **Secrets manager audit:** Access logs reviewed for unauthorized access

## Automated rotation (future)

Consider implementing automated annual MFA key rotation:

```bash
#!/usr/bin/env bash
# scripts/rotate-mfa-keys.sh
# Scheduled via cron or CI/CD pipeline

# 1. Generate new key
# 2. Add to key ring (shadow mode)
# 3. Validate for 24 hours
# 4. Promote to active
# 5. Retire old key after 30-day window
# 6. Audit log with automation signature
```

## Related documentation

- [secrets-management.md](./secrets-management.md) — Comprehensive secrets framework
- [../operations/key-rotation.md](../operations/key-rotation.md) — General key rotation procedures
- [../security/incident-response.md](../security/incident-response.md) — Incident response and SEV escalation
- [../security/production-hardening.md](../security/production-hardening.md) — Production security checklist
