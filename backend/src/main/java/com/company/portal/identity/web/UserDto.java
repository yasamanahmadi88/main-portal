package com.company.portal.identity.web;

import com.company.portal.accesscontrol.web.RoleDto;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * OpenAPI {@code User} projection. {@code username} is the account email
 * (portal supports email login only for v1); {@code permissions} is the
 * flattened effective permission set.
 */
public record UserDto(
        UUID id,
        String username,
        String email,
        String displayName,
        String status,
        PreferencesDto preferences,
        List<RoleDto> roles,
        List<String> permissions,
        boolean mfaEnabled,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) { }
