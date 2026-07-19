package com.company.portal.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "mfa_recovery_codes")
public class MfaRecoveryCodeEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "code_hash", nullable = false, length = 128)
    private String codeHash;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "used_ip", columnDefinition = "inet")
    @org.hibernate.annotations.ColumnTransformer(write = "?::inet")
    private String usedIp;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected MfaRecoveryCodeEntity() { }

    public MfaRecoveryCodeEntity(UUID id, UUID userId, String codeHash) {
        this.id = id;
        this.userId = userId;
        this.codeHash = codeHash;
        this.issuedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getCodeHash() { return codeHash; }
    public OffsetDateTime getIssuedAt() { return issuedAt; }
    public OffsetDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(OffsetDateTime v) { this.usedAt = v; }
    public String getUsedIp() { return usedIp; }
    public void setUsedIp(String v) { this.usedIp = v; }
    public long getVersion() { return version; }
}
