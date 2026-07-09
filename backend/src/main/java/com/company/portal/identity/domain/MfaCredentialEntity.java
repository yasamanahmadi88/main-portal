package com.company.portal.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "mfa_credentials")
public class MfaCredentialEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "type", nullable = false, length = 32)
    private String type;

    @Column(name = "label", length = 128)
    private String label;

    @Column(name = "secret_ciphertext")
    private byte[] secretCiphertext;

    @Column(name = "secret_iv")
    private byte[] secretIv;

    @Column(name = "secret_tag")
    private byte[] secretTag;

    @Column(name = "encryption_key_id", nullable = false, length = 128)
    private String encryptionKeyId;

    @Column(name = "digits", nullable = false)
    private short digits = 6;

    @Column(name = "period_seconds", nullable = false)
    private short periodSeconds = 30;

    @Column(name = "algorithm", nullable = false, length = 32)
    private String algorithm = "HmacSHA1";

    @Column(name = "counter")
    private Long counter;

    @Column(name = "activated", nullable = false)
    private boolean activated;

    @Column(name = "activated_at")
    private OffsetDateTime activatedAt;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    @Column(name = "last_verified_step")
    private Long lastVerifiedStep;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected MfaCredentialEntity() { }

    public MfaCredentialEntity(UUID id, UUID userId, String type) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getType() { return type; }
    public String getLabel() { return label; }
    public void setLabel(String v) { this.label = v; }
    public byte[] getSecretCiphertext() { return secretCiphertext; }
    public void setSecretCiphertext(byte[] v) { this.secretCiphertext = v; }
    public byte[] getSecretIv() { return secretIv; }
    public void setSecretIv(byte[] v) { this.secretIv = v; }
    public byte[] getSecretTag() { return secretTag; }
    public void setSecretTag(byte[] v) { this.secretTag = v; }
    public String getEncryptionKeyId() { return encryptionKeyId; }
    public void setEncryptionKeyId(String v) { this.encryptionKeyId = v; }
    public short getDigits() { return digits; }
    public void setDigits(short v) { this.digits = v; }
    public short getPeriodSeconds() { return periodSeconds; }
    public void setPeriodSeconds(short v) { this.periodSeconds = v; }
    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String v) { this.algorithm = v; }
    public Long getCounter() { return counter; }
    public void setCounter(Long v) { this.counter = v; }
    public boolean isActivated() { return activated; }
    public void setActivated(boolean v) { this.activated = v; }
    public OffsetDateTime getActivatedAt() { return activatedAt; }
    public void setActivatedAt(OffsetDateTime v) { this.activatedAt = v; }
    public OffsetDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(OffsetDateTime v) { this.lastUsedAt = v; }
    public Long getLastVerifiedStep() { return lastVerifiedStep; }
    public void setLastVerifiedStep(Long v) { this.lastVerifiedStep = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime v) { this.updatedAt = v; }
    public long getVersion() { return version; }
}
