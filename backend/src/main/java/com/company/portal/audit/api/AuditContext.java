package com.company.portal.audit.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable snapshot of the details required to append one audit event.
 * Builder is used for readability at call-sites which may set many optional
 * fields.
 */
public final class AuditContext {

    private final String eventType;
    private final String category;
    private final AuditSeverityLevel severity;
    private final AuditOutcome outcome;
    private final String actorType;
    private final UUID actorId;
    private final String actorDisplay;
    private final String targetType;
    private final String targetId;
    private final String targetDisplay;
    private final String action;
    private final String ipAddress;
    private final String userAgent;
    private final String correlationId;
    private final String requestId;
    private final String traceId;
    private final String sessionId;
    private final Map<String, Object> payload;

    private AuditContext(Builder b) {
        this.eventType = b.eventType;
        this.category = b.category;
        this.severity = b.severity;
        this.outcome = b.outcome;
        this.actorType = b.actorType;
        this.actorId = b.actorId;
        this.actorDisplay = b.actorDisplay;
        this.targetType = b.targetType;
        this.targetId = b.targetId;
        this.targetDisplay = b.targetDisplay;
        this.action = b.action;
        this.ipAddress = b.ipAddress;
        this.userAgent = b.userAgent;
        this.correlationId = b.correlationId;
        this.requestId = b.requestId;
        this.traceId = b.traceId;
        this.sessionId = b.sessionId;
        this.payload = Map.copyOf(b.payload);
    }

    public String eventType() { return eventType; }
    public String category() { return category; }
    public AuditSeverityLevel severity() { return severity; }
    public AuditOutcome outcome() { return outcome; }
    public String actorType() { return actorType; }
    public UUID actorId() { return actorId; }
    public String actorDisplay() { return actorDisplay; }
    public String targetType() { return targetType; }
    public String targetId() { return targetId; }
    public String targetDisplay() { return targetDisplay; }
    public String action() { return action; }
    public String ipAddress() { return ipAddress; }
    public String userAgent() { return userAgent; }
    public String correlationId() { return correlationId; }
    public String requestId() { return requestId; }
    public String traceId() { return traceId; }
    public String sessionId() { return sessionId; }
    public Map<String, Object> payload() { return payload; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String eventType;
        private String category = "SYSTEM";
        private AuditSeverityLevel severity = AuditSeverityLevel.INFO;
        private AuditOutcome outcome = AuditOutcome.SUCCESS;
        private String actorType = "SYSTEM";
        private UUID actorId;
        private String actorDisplay;
        private String targetType;
        private String targetId;
        private String targetDisplay;
        private String action;
        private String ipAddress;
        private String userAgent;
        private String correlationId;
        private String requestId;
        private String traceId;
        private String sessionId;
        private final Map<String, Object> payload = new LinkedHashMap<>();

        public Builder eventType(String v) { this.eventType = v; return this; }
        public Builder category(String v) { this.category = v; return this; }
        public Builder severity(AuditSeverityLevel v) { this.severity = v; return this; }
        public Builder outcome(AuditOutcome v) { this.outcome = v; return this; }
        public Builder actorType(String v) { this.actorType = v; return this; }
        public Builder actorId(UUID v) { this.actorId = v; return this; }
        public Builder actorDisplay(String v) { this.actorDisplay = v; return this; }
        public Builder targetType(String v) { this.targetType = v; return this; }
        public Builder targetId(String v) { this.targetId = v; return this; }
        public Builder targetDisplay(String v) { this.targetDisplay = v; return this; }
        public Builder action(String v) { this.action = v; return this; }
        public Builder ipAddress(String v) { this.ipAddress = v; return this; }
        public Builder userAgent(String v) { this.userAgent = v; return this; }
        public Builder correlationId(String v) { this.correlationId = v; return this; }
        public Builder requestId(String v) { this.requestId = v; return this; }
        public Builder traceId(String v) { this.traceId = v; return this; }
        public Builder sessionId(String v) { this.sessionId = v; return this; }
        public Builder payload(Map<String, Object> v) {
            if (v != null) { this.payload.putAll(v); }
            return this;
        }
        public Builder addPayload(String key, Object value) {
            if (value != null) { this.payload.put(key, value); }
            return this;
        }
        public AuditContext build() {
            if (eventType == null || eventType.isBlank()) {
                throw new IllegalArgumentException("eventType is required");
            }
            if (action == null || action.isBlank()) {
                this.action = eventType;
            }
            return new AuditContext(this);
        }
    }
}
