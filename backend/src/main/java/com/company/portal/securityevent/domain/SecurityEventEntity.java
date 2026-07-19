package com.company.portal.securityevent.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "security_events")
public class SecurityEventEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "event_type", nullable = false, updatable = false, length = 96)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 16)
    private SecurityEventSeverity severity;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "ip_address", columnDefinition = "inet")
    @org.hibernate.annotations.ColumnTransformer(write = "?::inet")
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "session_id", length = 128)
    private String sessionId;

    @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    private String payloadJson;

    @Column(name = "acknowledged_at")
    private OffsetDateTime acknowledgedAt;

    @Column(name = "acknowledged_by")
    private UUID acknowledgedBy;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "resolution_notes", length = 2048)
    private String resolutionNotes;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected SecurityEventEntity() { }

    public SecurityEventEntity(UUID id, String eventType, SecurityEventSeverity severity) {
        this.id = id;
        this.eventType = eventType;
        this.severity = severity;
        this.occurredAt = OffsetDateTime.now();
        this.payloadJson = "{}";
    }

    public UUID getId() { return id; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(OffsetDateTime v) { this.occurredAt = v; }
    public String getEventType() { return eventType; }
    public SecurityEventSeverity getSeverity() { return severity; }
    public void setSeverity(SecurityEventSeverity v) { this.severity = v; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID v) { this.userId = v; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String v) { this.ipAddress = v; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String v) { this.userAgent = v; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String v) { this.correlationId = v; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String v) { this.sessionId = v; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String v) { this.payloadJson = v; }
    public OffsetDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(OffsetDateTime v) { this.acknowledgedAt = v; }
    public UUID getAcknowledgedBy() { return acknowledgedBy; }
    public void setAcknowledgedBy(UUID v) { this.acknowledgedBy = v; }
    public OffsetDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(OffsetDateTime v) { this.resolvedAt = v; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String v) { this.resolutionNotes = v; }
    public long getVersion() { return version; }
}
