package com.company.portal.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetTokenEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Column(name = "request_ip", columnDefinition = "inet")
    private String requestIp;

    @Column(name = "request_user_agent", length = 512)
    private String requestUserAgent;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected PasswordResetTokenEntity() { }

    public PasswordResetTokenEntity(UUID id, UUID userId, String tokenHash, OffsetDateTime expiresAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.issuedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public OffsetDateTime getIssuedAt() { return issuedAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public OffsetDateTime getConsumedAt() { return consumedAt; }
    public void setConsumedAt(OffsetDateTime v) { this.consumedAt = v; }
    public String getRequestIp() { return requestIp; }
    public void setRequestIp(String v) { this.requestIp = v; }
    public String getRequestUserAgent() { return requestUserAgent; }
    public void setRequestUserAgent(String v) { this.requestUserAgent = v; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String v) { this.correlationId = v; }
    public long getVersion() { return version; }
}
