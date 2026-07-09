-- V4: Audit + Security + Outbox + Notifications + System Settings
-- Tamper-evident audit chain: previous_hash / current_hash per event, sequence per stream.

CREATE TABLE audit_events (
    id                          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    occurred_at                 TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    recorded_at                 TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    event_type                  VARCHAR(96)     NOT NULL,
    category                    VARCHAR(64)     NOT NULL,
    severity                    VARCHAR(16)     NOT NULL DEFAULT 'INFO',
    actor_type                  VARCHAR(32)     NOT NULL DEFAULT 'USER',
    actor_id                    UUID,
    actor_display               VARCHAR(256),
    target_type                 VARCHAR(64),
    target_id                   VARCHAR(128),
    target_display              VARCHAR(256),
    action                      VARCHAR(96)     NOT NULL,
    outcome                     VARCHAR(32)     NOT NULL DEFAULT 'SUCCESS',
    ip_address                  INET,
    user_agent                  VARCHAR(512),
    correlation_id              VARCHAR(64),
    request_id                  VARCHAR(64),
    trace_id                    VARCHAR(64),
    session_id                  VARCHAR(128),
    payload_json                JSONB           NOT NULL DEFAULT '{}'::jsonb,
    previous_hash               VARCHAR(128),
    current_hash                VARCHAR(128)    NOT NULL,
    sequence_number             BIGINT          NOT NULL,
    stream                      VARCHAR(64)     NOT NULL DEFAULT 'global',
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT audit_events_outcome_check CHECK (
        outcome IN ('SUCCESS','FAILURE','DENIED','PARTIAL','UNKNOWN')
    ),
    CONSTRAINT audit_events_severity_check CHECK (
        severity IN ('DEBUG','INFO','NOTICE','WARN','ERROR','CRITICAL')
    ),
    CONSTRAINT audit_events_stream_seq_unique UNIQUE (stream, sequence_number)
);

CREATE INDEX idx_audit_events_occurred      ON audit_events (occurred_at DESC);
CREATE INDEX idx_audit_events_actor         ON audit_events (actor_id);
CREATE INDEX idx_audit_events_type_time     ON audit_events (event_type, occurred_at DESC);
CREATE INDEX idx_audit_events_category_time ON audit_events (category, occurred_at DESC);
CREATE INDEX idx_audit_events_target        ON audit_events (target_type, target_id);
CREATE INDEX idx_audit_events_correlation   ON audit_events (correlation_id);

CREATE TABLE audit_event_chain (
    stream                      VARCHAR(64)     PRIMARY KEY,
    last_sequence               BIGINT          NOT NULL DEFAULT 0,
    last_hash                   VARCHAR(128),
    last_event_id               UUID,
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    version                     BIGINT          NOT NULL DEFAULT 0
);

INSERT INTO audit_event_chain (stream, last_sequence, last_hash, last_event_id)
VALUES ('global', 0, NULL, NULL)
ON CONFLICT (stream) DO NOTHING;

CREATE TABLE security_events (
    id                          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    occurred_at                 TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    event_type                  VARCHAR(96)     NOT NULL,
    severity                    VARCHAR(16)     NOT NULL DEFAULT 'INFO',
    user_id                     UUID,
    ip_address                  INET,
    user_agent                  VARCHAR(512),
    correlation_id              VARCHAR(64),
    session_id                  VARCHAR(128),
    payload_json                JSONB           NOT NULL DEFAULT '{}'::jsonb,
    acknowledged_at             TIMESTAMPTZ,
    acknowledged_by             UUID,
    resolved_at                 TIMESTAMPTZ,
    resolution_notes            VARCHAR(2048),
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT security_events_severity_check CHECK (
        severity IN ('DEBUG','INFO','NOTICE','WARN','ERROR','CRITICAL')
    )
);

CREATE INDEX idx_security_events_occurred   ON security_events (occurred_at DESC);
CREATE INDEX idx_security_events_type_time  ON security_events (event_type, occurred_at DESC);
CREATE INDEX idx_security_events_user       ON security_events (user_id);
CREATE INDEX idx_security_events_open       ON security_events (occurred_at DESC)
    WHERE resolved_at IS NULL;

CREATE TABLE outbox_events (
    id                          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type              VARCHAR(96)     NOT NULL,
    aggregate_id                VARCHAR(128)    NOT NULL,
    event_type                  VARCHAR(96)     NOT NULL,
    payload_json                JSONB           NOT NULL,
    headers_json                JSONB           NOT NULL DEFAULT '{}'::jsonb,
    status                      VARCHAR(24)     NOT NULL DEFAULT 'PENDING',
    attempt_count               INTEGER         NOT NULL DEFAULT 0,
    max_attempts                INTEGER         NOT NULL DEFAULT 10,
    next_attempt_at             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    published_at                TIMESTAMPTZ,
    last_error                  VARCHAR(2048),
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT outbox_events_status_check CHECK (
        status IN ('PENDING','IN_FLIGHT','PUBLISHED','FAILED','DEAD_LETTER')
    )
);

CREATE INDEX idx_outbox_events_status_next
    ON outbox_events (status, next_attempt_at)
    WHERE status IN ('PENDING','IN_FLIGHT');
CREATE INDEX idx_outbox_events_aggregate ON outbox_events (aggregate_type, aggregate_id);

CREATE TABLE notification_deliveries (
    id                          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    outbox_event_id             UUID,
    channel                     VARCHAR(32)     NOT NULL,
    template_code               VARCHAR(96)     NOT NULL,
    recipient                   VARCHAR(320)    NOT NULL,
    subject                     VARCHAR(256),
    status                      VARCHAR(24)     NOT NULL DEFAULT 'PENDING',
    provider_message_id         VARCHAR(256),
    attempt_count               INTEGER         NOT NULL DEFAULT 0,
    last_error                  VARCHAR(2048),
    scheduled_at                TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    sent_at                     TIMESTAMPTZ,
    delivered_at                TIMESTAMPTZ,
    correlation_id              VARCHAR(64),
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT notification_deliveries_channel_check CHECK (
        channel IN ('EMAIL','SMS','WEBHOOK','IN_APP','PUSH')
    ),
    CONSTRAINT notification_deliveries_status_check CHECK (
        status IN ('PENDING','SENDING','SENT','DELIVERED','FAILED','BOUNCED','SKIPPED')
    ),
    CONSTRAINT notification_deliveries_outbox_fk FOREIGN KEY (outbox_event_id)
        REFERENCES outbox_events (id) ON DELETE SET NULL
);

CREATE INDEX idx_notification_deliveries_recipient ON notification_deliveries (recipient);
CREATE INDEX idx_notification_deliveries_status    ON notification_deliveries (status, scheduled_at);
CREATE INDEX idx_notification_deliveries_template  ON notification_deliveries (template_code, created_at DESC);

CREATE TABLE system_settings (
    id                          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    setting_key                 VARCHAR(128)    NOT NULL,
    setting_value               JSONB           NOT NULL DEFAULT '{}'::jsonb,
    setting_type                VARCHAR(32)     NOT NULL DEFAULT 'STRING',
    description                 VARCHAR(512),
    is_secret                   BOOLEAN         NOT NULL DEFAULT FALSE,
    updated_by                  UUID,
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT system_settings_key_unique UNIQUE (setting_key)
);

CREATE INDEX idx_system_settings_updated ON system_settings (updated_at DESC);
