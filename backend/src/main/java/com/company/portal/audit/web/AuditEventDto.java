package com.company.portal.audit.web;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * REST projection of an audit event. Matches the {@code AuditEvent} schema in
 * the OpenAPI contract; never expose {@code AuditEventEntity} directly.
 */
public record AuditEventDto(
        UUID id,
        OffsetDateTime occurredAt,
        Actor actor,
        String action,
        Resource resource,
        String outcome,
        String ipAddress,
        String userAgent,
        Map<String, Object> metadata,
        String previousHash,
        String hash
) {
    public record Actor(String type, UUID userId, String username) { }
    public record Resource(String type, String id, String displayName) { }
}
