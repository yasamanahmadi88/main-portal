package com.company.portal.securityevent.application;

import com.company.portal.securityevent.api.SecurityEventPublisher;
import com.company.portal.securityevent.domain.SecurityEventEntity;
import com.company.portal.securityevent.domain.SecurityEventSeverity;
import com.company.portal.securityevent.repository.SecurityEventRepository;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class DefaultSecurityEventPublisher implements SecurityEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DefaultSecurityEventPublisher.class);

    private final SecurityEventRepository repository;
    private final ObjectMapper objectMapper;

    public DefaultSecurityEventPublisher(SecurityEventRepository repository,
                                         ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID publish(String eventType, SecurityEventLevel level,
                        UUID userId, String ipAddress, String userAgent,
                        String correlationId, String sessionId, Map<String, Object> details) {
        UUID id = UUID.randomUUID();
        SecurityEventEntity entity = new SecurityEventEntity(id, eventType, toSeverity(level));
        entity.setUserId(userId);
        entity.setIpAddress(ipAddress);
        entity.setUserAgent(userAgent);
        entity.setCorrelationId(correlationId);
        entity.setSessionId(sessionId);
        entity.setPayloadJson(serialize(details));
        repository.save(entity);
        if (log.isDebugEnabled()) {
            log.debug("Security event published: type={} severity={} user={}",
                    eventType, level, userId);
        }
        return id;
    }

    private static SecurityEventSeverity toSeverity(SecurityEventLevel level) {
        if (level == null) return SecurityEventSeverity.INFO;
        return switch (level) {
            case LOW -> SecurityEventSeverity.INFO;
            case MEDIUM -> SecurityEventSeverity.WARN;
            case HIGH -> SecurityEventSeverity.ERROR;
            case CRITICAL -> SecurityEventSeverity.CRITICAL;
        };
    }

    private String serialize(Map<String, Object> details) {
        if (details == null || details.isEmpty()) return "{}";
        try {
            return objectMapper.writeValueAsString(details);
        } catch (RuntimeException ex) {
            log.warn("Security event payload serialization failed; storing empty object", ex);
            return "{}";
        }
    }
}
