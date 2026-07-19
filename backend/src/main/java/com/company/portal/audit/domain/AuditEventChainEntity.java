package com.company.portal.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tracks the tail of each audit stream so we can append events in a single
 * SELECT-FOR-UPDATE + INSERT + UPDATE transaction without scanning
 * {@code audit_events}.
 */
@Entity
@Table(name = "audit_event_chain")
public class AuditEventChainEntity {

    @Id
    @Column(name = "stream", nullable = false, length = 64)
    private String stream;

    @Column(name = "last_sequence", nullable = false)
    private long lastSequence;

    @Column(name = "last_hash", length = 128)
    private String lastHash;

    @Column(name = "last_event_id")
    private UUID lastEventId;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected AuditEventChainEntity() { }

    public AuditEventChainEntity(String stream) {
        this.stream = stream;
        this.lastSequence = 0L;
        this.updatedAt = OffsetDateTime.now();
    }

    public String getStream() { return stream; }
    public long getLastSequence() { return lastSequence; }
    public void setLastSequence(long lastSequence) { this.lastSequence = lastSequence; }
    public String getLastHash() { return lastHash; }
    public void setLastHash(String lastHash) { this.lastHash = lastHash; }
    public UUID getLastEventId() { return lastEventId; }
    public void setLastEventId(UUID lastEventId) { this.lastEventId = lastEventId; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public long getVersion() { return version; }
}
