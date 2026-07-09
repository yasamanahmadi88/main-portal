package com.company.portal.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Append-only audit event. Application code must never {@code UPDATE} or
 * {@code DELETE} rows in this table — see {@code V5__db_grants_notes.sql}.
 * Hash-chain fields are populated by {@code AuditService}.
 */
@Entity
@Table(name = "audit_events")
public class AuditEventEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private OffsetDateTime recordedAt;

    @Column(name = "event_type", nullable = false, updatable = false, length = 96)
    private String eventType;

    @Column(name = "category", nullable = false, updatable = false, length = 64)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, updatable = false, length = 16)
    private AuditSeverity severity;

    @Column(name = "actor_type", nullable = false, updatable = false, length = 32)
    private String actorType;

    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    @Column(name = "actor_display", updatable = false, length = 256)
    private String actorDisplay;

    @Column(name = "target_type", updatable = false, length = 64)
    private String targetType;

    @Column(name = "target_id", updatable = false, length = 128)
    private String targetId;

    @Column(name = "target_display", updatable = false, length = 256)
    private String targetDisplay;

    @Column(name = "action", nullable = false, updatable = false, length = 96)
    private String action;

    @Column(name = "outcome", nullable = false, updatable = false, length = 32)
    private String outcome;

    @Column(name = "ip_address", updatable = false, columnDefinition = "inet")
    private String ipAddress;

    @Column(name = "user_agent", updatable = false, length = 512)
    private String userAgent;

    @Column(name = "correlation_id", updatable = false, length = 64)
    private String correlationId;

    @Column(name = "request_id", updatable = false, length = 64)
    private String requestId;

    @Column(name = "trace_id", updatable = false, length = 64)
    private String traceId;

    @Column(name = "session_id", updatable = false, length = 128)
    private String sessionId;

    @Column(name = "payload_json", nullable = false, updatable = false, columnDefinition = "jsonb")
    private String payloadJson;

    @Column(name = "previous_hash", updatable = false, length = 128)
    private String previousHash;

    @Column(name = "current_hash", nullable = false, updatable = false, length = 128)
    private String currentHash;

    @Column(name = "sequence_number", nullable = false, updatable = false)
    private long sequenceNumber;

    @Column(name = "stream", nullable = false, updatable = false, length = 64)
    private String stream;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected AuditEventEntity() { }

    public AuditEventEntity(UUID id) {
        this.id = id;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(OffsetDateTime occurredAt) { this.occurredAt = occurredAt; }
    public OffsetDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(OffsetDateTime recordedAt) { this.recordedAt = recordedAt; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public AuditSeverity getSeverity() { return severity; }
    public void setSeverity(AuditSeverity severity) { this.severity = severity; }
    public String getActorType() { return actorType; }
    public void setActorType(String actorType) { this.actorType = actorType; }
    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }
    public String getActorDisplay() { return actorDisplay; }
    public void setActorDisplay(String actorDisplay) { this.actorDisplay = actorDisplay; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getTargetDisplay() { return targetDisplay; }
    public void setTargetDisplay(String targetDisplay) { this.targetDisplay = targetDisplay; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public String getPreviousHash() { return previousHash; }
    public void setPreviousHash(String previousHash) { this.previousHash = previousHash; }
    public String getCurrentHash() { return currentHash; }
    public void setCurrentHash(String currentHash) { this.currentHash = currentHash; }
    public long getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(long sequenceNumber) { this.sequenceNumber = sequenceNumber; }
    public String getStream() { return stream; }
    public void setStream(String stream) { this.stream = stream; }
    public long getVersion() { return version; }
}
