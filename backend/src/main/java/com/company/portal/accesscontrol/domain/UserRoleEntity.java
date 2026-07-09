package com.company.portal.accesscontrol.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Association row between {@code users} and {@code roles}. Uses a composite
 * embedded key so we can attach lifecycle metadata (assigned_by, expires_at)
 * without a synthetic id.
 */
@Entity
@Table(name = "user_roles")
public class UserRoleEntity {

    @EmbeddedId
    private UserRoleId id;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private OffsetDateTime assignedAt;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    protected UserRoleEntity() { }

    public UserRoleEntity(UUID userId, UUID roleId, UUID assignedBy) {
        this.id = new UserRoleId(userId, roleId);
        this.assignedAt = OffsetDateTime.now();
        this.assignedBy = assignedBy;
    }

    public UserRoleId getId() { return id; }
    public OffsetDateTime getAssignedAt() { return assignedAt; }
    public UUID getAssignedBy() { return assignedBy; }
    public void setAssignedBy(UUID v) { this.assignedBy = v; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime v) { this.expiresAt = v; }

    @Embeddable
    public static class UserRoleId implements Serializable {

        @Column(name = "user_id", nullable = false)
        private UUID userId;

        @Column(name = "role_id", nullable = false)
        private UUID roleId;

        protected UserRoleId() { }

        public UserRoleId(UUID userId, UUID roleId) {
            this.userId = userId;
            this.roleId = roleId;
        }

        public UUID getUserId() { return userId; }
        public UUID getRoleId() { return roleId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UserRoleId that)) return false;
            return Objects.equals(userId, that.userId) && Objects.equals(roleId, that.roleId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, roleId);
        }
    }
}
