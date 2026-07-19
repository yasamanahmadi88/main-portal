package com.company.portal.securityevent.application;

import com.company.portal.audit.api.AuditContext;
import com.company.portal.audit.api.AuditOutcome;
import com.company.portal.audit.api.AuditService;
import com.company.portal.audit.api.AuditSeverityLevel;
import com.company.portal.securityevent.domain.SecurityEventEntity;
import com.company.portal.securityevent.repository.SecurityEventRepository;
import com.company.portal.securityevent.web.SecurityEventDto;
import com.company.portal.shared.error.PortalException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class SecurityEventService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    private final SecurityEventRepository repository;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public SecurityEventService(SecurityEventRepository repository,
                                ObjectMapper objectMapper,
                                AuditService auditService) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<SecurityEventDto> search(OffsetDateTime from, OffsetDateTime to,
                                         String eventType, Boolean acknowledged, Pageable pageable) {
        return repository.search(from, to, eventType, acknowledged, pageable).map(this::toDto);
    }

    @Transactional
    public SecurityEventDto acknowledge(UUID id, UUID actorId, String note) {
        SecurityEventEntity entity = repository.findById(id)
                .orElseThrow(() -> new PortalException.NotFound("Security event not found"));
        if (entity.getAcknowledgedAt() == null) {
            entity.setAcknowledgedAt(OffsetDateTime.now());
            entity.setAcknowledgedBy(actorId);
            entity.setResolutionNotes(note);
            entity = repository.save(entity);
            auditService.append(AuditContext.builder()
                    .eventType("SECURITY_EVENT_ACKNOWLEDGED")
                    .category("SECURITY")
                    .severity(AuditSeverityLevel.NOTICE)
                    .outcome(AuditOutcome.SUCCESS)
                    .actorType("USER")
                    .actorId(actorId)
                    .action("ACKNOWLEDGE_SECURITY_EVENT")
                    .targetType("SECURITY_EVENT")
                    .targetId(id.toString())
                    .addPayload("note", note)
                    .build());
        }
        return toDto(entity);
    }

    public SecurityEventDto toDto(SecurityEventEntity e) {
        Map<String, Object> details;
        try {
            details = e.getPayloadJson() == null || e.getPayloadJson().isBlank()
                    ? Collections.emptyMap()
                    : objectMapper.readValue(e.getPayloadJson(), MAP_TYPE);
        } catch (Exception ex) {
            details = Collections.emptyMap();
        }
        return new SecurityEventDto(
                e.getId(),
                e.getOccurredAt(),
                e.getEventType(),
                mapSeverity(e.getSeverity()),
                e.getUserId(),
                e.getIpAddress(),
                details,
                e.getAcknowledgedAt() != null,
                e.getAcknowledgedBy(),
                e.getAcknowledgedAt(),
                e.getResolutionNotes()
        );
    }

    /**
     * Maps the six-level DB severity onto the four-level {@code
     * SecurityEventSeverity} enum used by the OpenAPI contract.
     */
    private static String mapSeverity(com.company.portal.securityevent.domain.SecurityEventSeverity s) {
        if (s == null) return "LOW";
        return switch (s) {
            case DEBUG, INFO, NOTICE -> "LOW";
            case WARN -> "MEDIUM";
            case ERROR -> "HIGH";
            case CRITICAL -> "CRITICAL";
        };
    }
}
