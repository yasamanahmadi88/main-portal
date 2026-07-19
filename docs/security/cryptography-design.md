# Cryptography Design

## Cryptographic uses

| Use case | Mechanism | Notes |
|----------|-----------|-------|
| Password storage | Argon2id | Spring Security encoder with BouncyCastle provider. |
| Reset tokens | CSPRNG raw token + stored hash | Raw token shown/sent once; compare by hash. |
| MFA TOTP secrets | Authenticated encryption | Key supplied externally and versioned by key ID. |
| Recovery codes | CSPRNG code + stored hash | Display once; mark used atomically. |
| Audit integrity | Hash chain | `current_hash = H(previous_hash + canonical_event)`. |
| Transport | TLS | Enforced by deployment edge/load balancer. |

## Password hashing

- Use Argon2id, not bcrypt, PBKDF2, SHA, or reversible encryption.
- Calibrate memory, iterations, and parallelism on production-like hardware.
- Store algorithm parameters with each password hash.
- Rehash on login when parameters are upgraded.

## Randomness

- Use Java `SecureRandom` or framework-approved CSPRNG APIs.
- Tokens must have at least 128 bits of entropy; higher is preferred for reset and recovery flows.
- Do not derive tokens from user data, timestamps, UUIDv1, or predictable counters.

## MFA secret encryption

Target properties:

- Envelope-compatible design: stored record includes `key_id`, ciphertext, IV/nonce, and authentication tag.
- Key material comes from a secret manager or environment variable, not the database.
- Rotation supports decrypt-with-old and encrypt-with-new until migration completes.
- Plaintext secrets exist only in memory for enrollment and verification.

## Audit hash chain

- Hash canonical JSON fields in a deterministic order.
- Include event ID or sequence, timestamp, actor, action, target, outcome, metadata digest, and previous hash.
- Store `previous_hash` and `current_hash`.
- Verification recalculates the chain and reports the first mismatch.

## TLS and certificates

- Production must use TLS 1.2+; TLS 1.3 preferred.
- HSTS should be enabled after validating HTTPS-only deployment.
- Internal plaintext between nginx and backend is acceptable only on a private trusted network; otherwise use mTLS or TLS.

## Prohibited patterns

- Custom cryptographic algorithms.
- Reversible password encryption.
- Static IVs/nonces for encryption.
- Logging plaintext tokens or secrets.
- Committing keys, keystores, or production `.env` files.

## Evidence to add

- Unit tests for token hashing and one-time use.
- Argon2id encoder configuration test.
- MFA encryption/decryption and key-rotation tests.
- Audit canonicalization/hash-chain tests.
