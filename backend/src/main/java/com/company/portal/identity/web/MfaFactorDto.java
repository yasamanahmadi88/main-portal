package com.company.portal.identity.web;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MfaFactorDto(
        UUID id,
        String type,
        String name,
        boolean enabled,
        OffsetDateTime createdAt,
        OffsetDateTime lastUsedAt
) { }
