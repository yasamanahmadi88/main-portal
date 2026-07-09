-- V1: Core Identity
-- Provides users, preferences, login attempts, token tables, and encryption key metadata.
-- All primary keys are UUIDs; timestamps are TIMESTAMPTZ; optimistic locking via version BIGINT.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id                          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    email                       VARCHAR(320)    NOT NULL,
    email_normalized            VARCHAR(320)    NOT NULL,
    email_verified              BOOLEAN         NOT NULL DEFAULT FALSE,
    password_hash               VARCHAR(512),
    display_name                VARCHAR(200)    NOT NULL,
    given_name                  VARCHAR(200),
    family_name                 VARCHAR(200),
    locale                      VARCHAR(35)     NOT NULL DEFAULT 'en-US',
    time_zone                   VARCHAR(64)     NOT NULL DEFAULT 'UTC',
    status                      VARCHAR(32)     NOT NULL DEFAULT 'PENDING_VERIFICATION',
    mfa_enabled                 BOOLEAN         NOT NULL DEFAULT FALSE,
    mfa_enforced                BOOLEAN         NOT NULL DEFAULT FALSE,
    failed_login_count          INTEGER         NOT NULL DEFAULT 0,
    lockout_until               TIMESTAMPTZ,
    last_login_at               TIMESTAMPTZ,
    last_password_changed_at    TIMESTAMPTZ,
    password_expires_at         TIMESTAMPTZ,
    terms_accepted_at           TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by                  UUID,
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_by                  UUID,
    deleted_at                  TIMESTAMPTZ,
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT users_email_normalized_unique UNIQUE (email_normalized),
    CONSTRAINT users_status_check CHECK (
        status IN ('PENDING_VERIFICATION','ACTIVE','LOCKED','DISABLED','DELETED')
    )
);

CREATE INDEX idx_users_status         ON users (status) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_created_at     ON users (created_at DESC);
CREATE INDEX idx_users_last_login_at  ON users (last_login_at DESC);
CREATE INDEX idx_users_lockout_until  ON users (lockout_until) WHERE lockout_until IS NOT NULL;

CREATE TABLE user_preferences (
    id                          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                     UUID            NOT NULL,
    theme                       VARCHAR(32)     NOT NULL DEFAULT 'SYSTEM',
    notifications_email         BOOLEAN         NOT NULL DEFAULT TRUE,
    notifications_security      BOOLEAN         NOT NULL DEFAULT TRUE,
    marketing_opt_in            BOOLEAN         NOT NULL DEFAULT FALSE,
    preferences_json            JSONB           NOT NULL DEFAULT '{}'::jsonb,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT user_preferences_user_unique UNIQUE (user_id),
    CONSTRAINT user_preferences_user_fk FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE login_attempts (
    id                          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                     UUID,
    email_normalized            VARCHAR(320)    NOT NULL,
    ip_address                  INET,
    user_agent                  VARCHAR(512),
    outcome                     VARCHAR(32)     NOT NULL,
    failure_reason              VARCHAR(128),
    mfa_challenged              BOOLEAN         NOT NULL DEFAULT FALSE,
    mfa_passed                  BOOLEAN         NOT NULL DEFAULT FALSE,
    correlation_id              VARCHAR(64),
    attempted_at                TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT login_attempts_outcome_check CHECK (
        outcome IN ('SUCCESS','FAILURE','LOCKED','REQUIRES_MFA','MFA_FAILED')
    )
);

CREATE INDEX idx_login_attempts_email_time ON login_attempts (email_normalized, attempted_at DESC);
CREATE INDEX idx_login_attempts_user_time  ON login_attempts (user_id, attempted_at DESC);
CREATE INDEX idx_login_attempts_ip_time    ON login_attempts (ip_address, attempted_at DESC);

CREATE TABLE password_reset_tokens (
    id                          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                     UUID            NOT NULL,
    token_hash                  VARCHAR(128)    NOT NULL,
    issued_at                   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    expires_at                  TIMESTAMPTZ     NOT NULL,
    consumed_at                 TIMESTAMPTZ,
    request_ip                  INET,
    request_user_agent          VARCHAR(512),
    correlation_id              VARCHAR(64),
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT password_reset_tokens_hash_unique UNIQUE (token_hash),
    CONSTRAINT password_reset_tokens_user_fk FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_tokens_user     ON password_reset_tokens (user_id);
CREATE INDEX idx_password_reset_tokens_expires  ON password_reset_tokens (expires_at);

CREATE TABLE email_verification_tokens (
    id                          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                     UUID            NOT NULL,
    email                       VARCHAR(320)    NOT NULL,
    token_hash                  VARCHAR(128)    NOT NULL,
    issued_at                   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    expires_at                  TIMESTAMPTZ     NOT NULL,
    consumed_at                 TIMESTAMPTZ,
    correlation_id              VARCHAR(64),
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT email_verification_tokens_hash_unique UNIQUE (token_hash),
    CONSTRAINT email_verification_tokens_user_fk FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_email_verification_tokens_user    ON email_verification_tokens (user_id);
CREATE INDEX idx_email_verification_tokens_expires ON email_verification_tokens (expires_at);

CREATE TABLE encryption_key_metadata (
    id                          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    key_id                      VARCHAR(128)    NOT NULL,
    purpose                     VARCHAR(64)     NOT NULL,
    algorithm                   VARCHAR(64)     NOT NULL,
    status                      VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE',
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    activated_at                TIMESTAMPTZ,
    rotated_at                  TIMESTAMPTZ,
    retired_at                  TIMESTAMPTZ,
    notes                       VARCHAR(1024),
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT encryption_key_metadata_key_unique UNIQUE (key_id),
    CONSTRAINT encryption_key_metadata_status_check CHECK (
        status IN ('PENDING','ACTIVE','ROTATING','RETIRED','COMPROMISED')
    )
);

CREATE INDEX idx_encryption_key_metadata_purpose_status ON encryption_key_metadata (purpose, status);
