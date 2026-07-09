package com.company.portal.identity.web;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SessionDto(
        String id,
        UUID userId,
        String username,
        String ipAddress,
        String userAgent,
        OffsetDateTime createdAt,
        OffsetDateTime lastAccessedAt,
        OffsetDateTime expiresAt,
        OffsetDateTime revokedAt,
        boolean current
) { }
