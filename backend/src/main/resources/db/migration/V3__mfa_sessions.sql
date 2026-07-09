-- V3: MFA + Session metadata

CREATE TABLE mfa_credentials (
    id                          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                     UUID            NOT NULL,
    type                        VARCHAR(32)     NOT NULL,
    label                       VARCHAR(128),
    secret_ciphertext           BYTEA,
    secret_iv                   BYTEA,
    secret_tag                  BYTEA,
    encryption_key_id           VARCHAR(128)    NOT NULL,
    digits                      SMALLINT        NOT NULL DEFAULT 6,
    period_seconds              SMALLINT        NOT NULL DEFAULT 30,
    algorithm                   VARCHAR(32)     NOT NULL DEFAULT 'HmacSHA1',
    counter                     BIGINT,
    activated                   BOOLEAN         NOT NULL DEFAULT FALSE,
    activated_at                TIMESTAMPTZ,
    last_used_at                TIMESTAMPTZ,
    last_verified_step          BIGINT,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT mfa_credentials_type_check CHECK (
        type IN ('TOTP','WEBAUTHN','RECOVERY_CODE_BUNDLE','EMAIL_OTP')
    ),
    CONSTRAINT mfa_credentials_user_fk FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_mfa_credentials_user       ON mfa_credentials (user_id);
CREATE INDEX idx_mfa_credentials_user_type  ON mfa_credentials (user_id, type);

CREATE TABLE mfa_recovery_codes (
    id                          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                     UUID            NOT NULL,
    code_hash                   VARCHAR(128)    NOT NULL,
    issued_at                   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    used_at                     TIMESTAMPTZ,
    used_ip                     INET,
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT mfa_recovery_codes_hash_unique UNIQUE (user_id, code_hash),
    CONSTRAINT mfa_recovery_codes_user_fk FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_mfa_recovery_codes_user_unused
    ON mfa_recovery_codes (user_id)
    WHERE used_at IS NULL;

CREATE TABLE user_session_metadata (
    id                          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                     UUID            NOT NULL,
    session_id                  VARCHAR(128)    NOT NULL,
    ip_address                  INET,
    user_agent                  VARCHAR(512),
    device_fingerprint          VARCHAR(256),
    mfa_verified                BOOLEAN         NOT NULL DEFAULT FALSE,
    mfa_verified_at             TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    last_seen_at                TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    expires_at                  TIMESTAMPTZ,
    revoked_at                  TIMESTAMPTZ,
    revoke_reason               VARCHAR(64),
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT user_session_metadata_session_unique UNIQUE (session_id),
    CONSTRAINT user_session_metadata_user_fk FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_session_metadata_user       ON user_session_metadata (user_id);
CREATE INDEX idx_user_session_metadata_last_seen  ON user_session_metadata (last_seen_at DESC);
CREATE INDEX idx_user_session_metadata_active
    ON user_session_metadata (user_id)
    WHERE revoked_at IS NULL;
