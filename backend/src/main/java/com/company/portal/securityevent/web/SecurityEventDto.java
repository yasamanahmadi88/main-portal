package com.company.portal.securityevent.web;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record SecurityEventDto(
        UUID id,
        OffsetDateTime occurredAt,
        String type,
        String severity,
        UUID userId,
        String ipAddress,
        Map<String, Object> details,
        boolean acknowledged,
        UUID acknowledgedBy,
        OffsetDateTime acknowledgedAt,
        String acknowledgementNote
) { }
