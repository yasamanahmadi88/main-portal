package com.company.portal.identity.domain;

/**
 * Internal user lifecycle status. Persisted values must match the CHECK
 * constraint on {@code users.status} in V1__core_identity.sql. Mapping to
 * the API-level {@code UserStatus} enum happens in the identity web layer.
 */
public enum UserStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    LOCKED,
    DISABLED,
    DELETED;

    /**
     * Maps to the four-value OpenAPI enum: ACTIVE, INACTIVE, LOCKED, PENDING.
     */
    public String toApiValue() {
        return switch (this) {
            case ACTIVE -> "ACTIVE";
            case LOCKED -> "LOCKED";
            case PENDING_VERIFICATION -> "PENDING";
            case DISABLED, DELETED -> "INACTIVE";
        };
    }
}
