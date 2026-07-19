package com.company.portal.settings.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "system_settings")
public class SystemSettingEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "setting_key", nullable = false, unique = true, length = 128)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, columnDefinition = "jsonb")
    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    private String settingValue = "{}";

    @Column(name = "setting_type", nullable = false, length = 32)
    private String settingType = "STRING";

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "is_secret", nullable = false)
    private boolean secret;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected SystemSettingEntity() { }

    public SystemSettingEntity(UUID id, String settingKey) {
        this.id = id;
        this.settingKey = settingKey;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() { return id; }
    public String getSettingKey() { return settingKey; }
    public String getSettingValue() { return settingValue; }
    public void setSettingValue(String v) { this.settingValue = v; }
    public String getSettingType() { return settingType; }
    public void setSettingType(String v) { this.settingType = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public boolean isSecret() { return secret; }
    public void setSecret(boolean v) { this.secret = v; }
    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID v) { this.updatedBy = v; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime v) { this.updatedAt = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }
}
