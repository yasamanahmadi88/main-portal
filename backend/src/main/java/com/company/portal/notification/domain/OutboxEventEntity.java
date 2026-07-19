package com.company.portal.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 96)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 128)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 96)
    private String eventType;

    @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    private String payloadJson;

    @Column(name = "headers_json", nullable = false, columnDefinition = "jsonb")
    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    private String headersJson = "{}";

    @Column(name = "status", nullable = false, length = 24)
    private String status = "PENDING";

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 10;

    @Column(name = "next_attempt_at", nullable = false)
    private OffsetDateTime nextAttemptAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "last_error", length = 2048)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected OutboxEventEntity() { }

    public OutboxEventEntity(UUID id, String aggregateType, String aggregateId,
                             String eventType, String payloadJson) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payloadJson = payloadJson;
        this.nextAttemptAt = OffsetDateTime.now();
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() { return id; }
    public String getAggregateType() { return aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayloadJson() { return payloadJson; }
    public String getHeadersJson() { return headersJson; }
    public void setHeadersJson(String v) { this.headersJson = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int v) { this.attemptCount = v; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int v) { this.maxAttempts = v; }
    public OffsetDateTime getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(OffsetDateTime v) { this.nextAttemptAt = v; }
    public OffsetDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(OffsetDateTime v) { this.publishedAt = v; }
    public String getLastError() { return lastError; }
    public void setLastError(String v) { this.lastError = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime v) { this.updatedAt = v; }
    public long getVersion() { return version; }
}
