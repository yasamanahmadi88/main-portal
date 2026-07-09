package com.company.portal.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Authoritative record of a user account. Fields mirror V1__core_identity.sql
 * exactly so Hibernate's {@code ddl-auto: validate} can boot successfully.
 *
 * <p>Note that {@code email} carries the display form and {@code emailNormalized}
 * is the lowercase-trimmed form used for uniqueness / login lookup. Both are
 * stored so we can preserve the exact form the user provided.</p>
 */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "email_normalized", nullable = false, length = 320, unique = true)
    private String emailNormalized;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "password_hash", length = 512)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "given_name", length = 200)
    private String givenName;

    @Column(name = "family_name", length = 200)
    private String familyName;

    @Column(name = "locale", nullable = false, length = 35)
    private String locale = "en-US";

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone = "UTC";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled;

    @Column(name = "mfa_enforced", nullable = false)
    private boolean mfaEnforced;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Column(name = "lockout_until")
    private OffsetDateTime lockoutUntil;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "last_password_changed_at")
    private OffsetDateTime lastPasswordChangedAt;

    @Column(name = "password_expires_at")
    private OffsetDateTime passwordExpiresAt;

    @Column(name = "terms_accepted_at")
    private OffsetDateTime termsAcceptedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected UserEntity() { }

    public UserEntity(UUID id, String email, String emailNormalized, String displayName) {
        this.id = id;
        this.email = email;
        this.emailNormalized = emailNormalized;
        this.displayName = displayName;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getEmailNormalized() { return emailNormalized; }
    public void setEmailNormalized(String v) { this.emailNormalized = v; }
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean v) { this.emailVerified = v; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String v) { this.passwordHash = v; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String v) { this.displayName = v; }
    public String getGivenName() { return givenName; }
    public void setGivenName(String v) { this.givenName = v; }
    public String getFamilyName() { return familyName; }
    public void setFamilyName(String v) { this.familyName = v; }
    public String getLocale() { return locale; }
    public void setLocale(String v) { this.locale = v; }
    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String v) { this.timeZone = v; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus v) { this.status = v; }
    public boolean isMfaEnabled() { return mfaEnabled; }
    public void setMfaEnabled(boolean v) { this.mfaEnabled = v; }
    public boolean isMfaEnforced() { return mfaEnforced; }
    public void setMfaEnforced(boolean v) { this.mfaEnforced = v; }
    public int getFailedLoginCount() { return failedLoginCount; }
    public void setFailedLoginCount(int v) { this.failedLoginCount = v; }
    public OffsetDateTime getLockoutUntil() { return lockoutUntil; }
    public void setLockoutUntil(OffsetDateTime v) { this.lockoutUntil = v; }
    public OffsetDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(OffsetDateTime v) { this.lastLoginAt = v; }
    public OffsetDateTime getLastPasswordChangedAt() { return lastPasswordChangedAt; }
    public void setLastPasswordChangedAt(OffsetDateTime v) { this.lastPasswordChangedAt = v; }
    public OffsetDateTime getPasswordExpiresAt() { return passwordExpiresAt; }
    public void setPasswordExpiresAt(OffsetDateTime v) { this.passwordExpiresAt = v; }
    public OffsetDateTime getTermsAcceptedAt() { return termsAcceptedAt; }
    public void setTermsAcceptedAt(OffsetDateTime v) { this.termsAcceptedAt = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime v) { this.createdAt = v; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID v) { this.createdBy = v; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime v) { this.updatedAt = v; }
    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID v) { this.updatedBy = v; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime v) { this.deletedAt = v; }
    public long getVersion() { return version; }
}
