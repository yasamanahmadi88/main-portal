package com.company.portal.securityevent.api;

import java.util.Map;
import java.util.UUID;

/**
 * Fire-and-forget style API used by identity / RBAC / audit callers to
 * publish notable security signals. The implementation persists the event
 * synchronously so operators can query it immediately.
 */
public interface SecurityEventPublisher {

    UUID publish(String eventType, SecurityEventLevel severity,
                 UUID userId, String ipAddress, String userAgent,
                 String correlationId, String sessionId, Map<String, Object> details);

    enum SecurityEventLevel { LOW, MEDIUM, HIGH, CRITICAL }
}
