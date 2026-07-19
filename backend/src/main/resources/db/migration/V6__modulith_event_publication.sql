-- Spring Modulith JPA event publication registry (required when ddl-auto=validate).
CREATE TABLE IF NOT EXISTS event_publication (
    id                     UUID PRIMARY KEY,
    listener_id            VARCHAR(512)  NOT NULL,
    event_type             VARCHAR(512)  NOT NULL,
    serialized_event       TEXT          NOT NULL,
    publication_date       TIMESTAMPTZ   NOT NULL,
    completion_date        TIMESTAMPTZ,
    status                 VARCHAR(64),
    completion_attempts    INT,
    last_resubmission_date TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_event_publication_incomplete
    ON event_publication (completion_date)
    WHERE completion_date IS NULL;

CREATE INDEX IF NOT EXISTS idx_event_publication_status
    ON event_publication (status);
