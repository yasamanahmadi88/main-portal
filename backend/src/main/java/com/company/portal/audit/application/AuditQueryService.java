package com.company.portal.audit.application;

import com.company.portal.audit.domain.AuditEventEntity;
import com.company.portal.audit.repository.AuditEventRepository;
import com.company.portal.audit.web.AuditEventDto;
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

/**
 * Read-side of the audit module. Wraps entity → DTO mapping and JSON
 * deserialization of the payload column.
 */
@Service
public class AuditQueryService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    private final AuditEventRepository events;
    private final ObjectMapper objectMapper;

    public AuditQueryService(AuditEventRepository events, ObjectMapper objectMapper) {
        this.events = events;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Page<AuditEventDto> search(OffsetDateTime from, OffsetDateTime to,
                                      UUID actorId, String eventType, String category,
                                      Pageable pageable) {
        return events.search(from, to, actorId, eventType, category, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public AuditEventDto findById(UUID id) {
        AuditEventEntity e = events.findById(id)
                .orElseThrow(() -> new PortalException.NotFound("Audit event not found"));
        return toDto(e);
    }

    public AuditEventDto toDto(AuditEventEntity e) {
        Map<String, Object> metadata;
        try {
            String payload = e.getPayloadJson();
            metadata = (payload == null || payload.isBlank())
                    ? Collections.emptyMap()
                    : objectMapper.readValue(payload, MAP_TYPE);
        } catch (Exception ex) {
            metadata = Collections.emptyMap();
        }
        return new AuditEventDto(
                e.getId(),
                e.getOccurredAt(),
                new AuditEventDto.Actor(e.getActorType(), e.getActorId(), e.getActorDisplay()),
                e.getAction(),
                new AuditEventDto.Resource(e.getTargetType(), e.getTargetId(), e.getTargetDisplay()),
                e.getOutcome(),
                e.getIpAddress(),
                e.getUserAgent(),
                metadata,
                e.getPreviousHash(),
                e.getCurrentHash()
        );
    }
}
