package com.company.portal.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_session_metadata")
public class UserSessionMetadataEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "session_id", nullable = false, unique = true, length = 128)
    private String sessionId;

    @Column(name = "ip_address", columnDefinition = "inet")
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "device_fingerprint", length = 256)
    private String deviceFingerprint;

    @Column(name = "mfa_verified", nullable = false)
    private boolean mfaVerified;

    @Column(name = "mfa_verified_at")
    private OffsetDateTime mfaVerifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private OffsetDateTime lastSeenAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "revoke_reason", length = 64)
    private String revokeReason;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected UserSessionMetadataEntity() { }

    public UserSessionMetadataEntity(UUID id, UUID userId, String sessionId) {
        this.id = id;
        this.userId = userId;
        this.sessionId = sessionId;
        this.createdAt = OffsetDateTime.now();
        this.lastSeenAt = this.createdAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getSessionId() { return sessionId; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String v) { this.ipAddress = v; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String v) { this.userAgent = v; }
    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String v) { this.deviceFingerprint = v; }
    public boolean isMfaVerified() { return mfaVerified; }
    public void setMfaVerified(boolean v) { this.mfaVerified = v; }
    public OffsetDateTime getMfaVerifiedAt() { return mfaVerifiedAt; }
    public void setMfaVerifiedAt(OffsetDateTime v) { this.mfaVerifiedAt = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(OffsetDateTime v) { this.lastSeenAt = v; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime v) { this.expiresAt = v; }
    public OffsetDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(OffsetDateTime v) { this.revokedAt = v; }
    public String getRevokeReason() { return revokeReason; }
    public void setRevokeReason(String v) { this.revokeReason = v; }
    public long getVersion() { return version; }
}
