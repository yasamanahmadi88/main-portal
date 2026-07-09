package com.company.portal.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_preferences")
public class UserPreferencesEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "theme", nullable = false, length = 32)
    private String theme = "SYSTEM";

    @Column(name = "notifications_email", nullable = false)
    private boolean notificationsEmail = true;

    @Column(name = "notifications_security", nullable = false)
    private boolean notificationsSecurity = true;

    @Column(name = "marketing_opt_in", nullable = false)
    private boolean marketingOptIn;

    @Column(name = "preferences_json", nullable = false, columnDefinition = "jsonb")
    private String preferencesJson = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected UserPreferencesEntity() { }

    public UserPreferencesEntity(UUID id, UUID userId) {
        this.id = id;
        this.userId = userId;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTheme() { return theme; }
    public void setTheme(String v) { this.theme = v; }
    public boolean isNotificationsEmail() { return notificationsEmail; }
    public void setNotificationsEmail(boolean v) { this.notificationsEmail = v; }
    public boolean isNotificationsSecurity() { return notificationsSecurity; }
    public void setNotificationsSecurity(boolean v) { this.notificationsSecurity = v; }
    public boolean isMarketingOptIn() { return marketingOptIn; }
    public void setMarketingOptIn(boolean v) { this.marketingOptIn = v; }
    public String getPreferencesJson() { return preferencesJson; }
    public void setPreferencesJson(String v) { this.preferencesJson = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime v) { this.updatedAt = v; }
    public long getVersion() { return version; }
}
