package com.company.portal.accesscontrol.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "permissions")
public class PermissionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 96)
    private String code;

    @Column(name = "resource", nullable = false, length = 64)
    private String resource;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "system_permission", nullable = false)
    private boolean systemPermission;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected PermissionEntity() { }

    public PermissionEntity(UUID id, String code, String resource, String action) {
        this.id = id;
        this.code = code;
        this.resource = resource;
        this.action = action;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getResource() { return resource; }
    public String getAction() { return action; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isSystemPermission() { return systemPermission; }
    public void setSystemPermission(boolean systemPermission) { this.systemPermission = systemPermission; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PermissionEntity that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}
