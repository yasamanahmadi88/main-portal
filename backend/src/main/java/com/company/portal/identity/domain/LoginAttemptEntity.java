package com.company.portal.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "login_attempts")
public class LoginAttemptEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "email_normalized", nullable = false, length = 320)
    private String emailNormalized;

    @Column(name = "ip_address", columnDefinition = "inet")
    @org.hibernate.annotations.ColumnTransformer(write = "?::inet")
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "outcome", nullable = false, length = 32)
    private String outcome;

    @Column(name = "failure_reason", length = 128)
    private String failureReason;

    @Column(name = "mfa_challenged", nullable = false)
    private boolean mfaChallenged;

    @Column(name = "mfa_passed", nullable = false)
    private boolean mfaPassed;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "attempted_at", nullable = false, updatable = false)
    private OffsetDateTime attemptedAt;

    protected LoginAttemptEntity() { }

    public LoginAttemptEntity(UUID id, String emailNormalized, String outcome) {
        this.id = id;
        this.emailNormalized = emailNormalized;
        this.outcome = outcome;
        this.attemptedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID v) { this.userId = v; }
    public String getEmailNormalized() { return emailNormalized; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String v) { this.ipAddress = v; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String v) { this.userAgent = v; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String v) { this.outcome = v; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String v) { this.failureReason = v; }
    public boolean isMfaChallenged() { return mfaChallenged; }
    public void setMfaChallenged(boolean v) { this.mfaChallenged = v; }
    public boolean isMfaPassed() { return mfaPassed; }
    public void setMfaPassed(boolean v) { this.mfaPassed = v; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String v) { this.correlationId = v; }
    public OffsetDateTime getAttemptedAt() { return attemptedAt; }
}
