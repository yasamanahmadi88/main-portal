package com.company.portal.shared.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Read-only helper exposing the currently authenticated principal's user id
 * to callers throughout the application. Backed by the Spring Security context
 * and the current HTTP request. Placed in the shared platform so any module
 * can depend on it without creating cycles with the identity module.
 */
public interface CurrentUserAccessor {

    Optional<UUID> currentUserId();

    default UUID currentUserIdOrThrow() {
        return currentUserId().orElseThrow(
                () -> new com.company.portal.shared.error.PortalException.Unauthorized(
                        "Authentication required"));
    }

    Optional<String> currentSessionId();
}
