# Key Rotation

Rotation procedures for cryptographic keys and secrets used by the Enterprise Portal (`main-portal`).

## Key inventory

| Secret | Purpose | Rotation frequency |
|--------|---------|-------------------|
| `MFA_ENCRYPTION_KEY_BASE64` / key ring | AES-GCM encryption of TOTP secrets | Annually or on compromise |
| `MFA_ENCRYPTION_KEY_ID` | Active key id for new enrollments | With MFA key rotation |
| `POSTGRES_PASSWORD` / `POSTGRES_MIGRATION_PASSWORD` | Database access | Annually |
| `REDIS_PASSWORD` | Redis AUTH | Annually |
| `PORTAL_BOOTSTRAP_ADMIN_PASSWORD` | One-time bootstrap | Remove after provisioning |
| TLS certificates | HTTPS | Per CA policy (≤ 398 days) |
| SMTP credentials | Outbound mail | Per provider policy |

## MFA encryption key rotation

TOTP secrets are encrypted with AES-256-GCM. The portal supports a **key ring**: decrypt with any configured key id; encrypt new secrets with the active key (`MFA_ENCRYPTION_KEY_ID`).

### Planned rotation

1. Generate a new key:
   ```bash
   NEW_KEY=$(openssl rand -base64 32)
   NEW_ID=v2
   ```
2. Add the new key to the key ring **alongside** the previous key (see `application.yml` / env mapping for `portal.mfa.encryption.keys`).
3. Set `MFA_ENCRYPTION_KEY_ID=v2` so new enrollments use the new key.
4. Redeploy backend; existing factors decrypt with the old key id stored on each factor.
5. Optionally force re-enrollment for privileged users, or run a controlled re-encrypt migration.
6. After verification, remove the retired key from the ring and secret stores.
7. Invalidate sessions if compromise is suspected: flush Redis session namespace or revoke sessions via admin APIs.

### Emergency rotation (compromise)

1. Open a SEV-1 incident — see [incident-response.md](./incident-response.md).
2. Deploy a new active key immediately; keep prior key only if decrypt of existing factors is still required during the window.
3. Prefer forced MFA re-enrollment for all users over temporary MFA bypass.
4. Force password reset for privileged accounts.
5. Full audit and security-event review.

## Database password rotation

```bash
# 1. Alter roles (names from .env)
docker compose exec postgres \
  psql -U portal_superuser -d portal \
  -c "ALTER USER portal_app PASSWORD 'new-app-secret';"

# 2. Update .env POSTGRES_PASSWORD / POSTGRES_MIGRATION_PASSWORD as needed
# 3. Rolling restart backend
docker compose --env-file .env up -d --no-deps backend
```

## Redis password rotation

```bash
# Update REDIS_PASSWORD in .env and redis service configuration
docker compose --env-file .env up -d redis backend
```

Changing Redis password drops existing connections; session data remains if the Redis volume persists.

## TLS certificate renewal

- Automate via ACME or load-balancer managed certificates.
- After renewal, verify HSTS and `SESSION_COOKIE_SECURE=true` under the `prod` profile.
- Confirm session cookie naming (`__Host-PORTAL_SESSION` when Secure + Path=/).

## Bootstrap admin password

After first successful admin login and creation of secondary administrators:

1. Change password via Profile → Security.
2. Remove `PORTAL_BOOTSTRAP_*` from `.env` / secret manager (`PORTAL_BOOTSTRAP_ENABLED=false`).
3. Redeploy backend (second start is a no-op when users already exist).

## Verification checklist

- [ ] Application starts healthy after secret change
- [ ] Login + MFA challenge succeed for a test account
- [ ] Audit integrity verification still passes
- [ ] Old secrets removed from CI logs, images, and secret managers
- [ ] Incident ticket closed with rotation evidence
