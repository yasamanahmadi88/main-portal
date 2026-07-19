package com.company.portal.audit.application;

import com.company.portal.audit.api.AuditContext;
import com.company.portal.audit.api.AuditService;
import com.company.portal.audit.domain.AuditEventChainEntity;
import com.company.portal.audit.domain.AuditEventEntity;
import com.company.portal.audit.domain.AuditSeverity;
import com.company.portal.audit.repository.AuditEventChainRepository;
import com.company.portal.audit.repository.AuditEventRepository;
import com.company.portal.shared.config.PortalProperties;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Default {@link AuditService} that append-only writes to {@code audit_events}
 * under a pessimistic row lock on {@code audit_event_chain}.
 *
 * <p>The append runs in a REQUIRES_NEW transaction so a caller can commit
 * business changes independently of audit success. If audit persistence
 * fails the caller-provided exception handling decides whether to abort the
 * business operation.</p>
 */
@Service
public class DefaultAuditService implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(DefaultAuditService.class);
    private static final String DEFAULT_STREAM = "global";

    private final AuditEventRepository events;
    private final AuditEventChainRepository chain;
    private final AuditHashCalculator hashCalculator;
    private final ObjectMapper objectMapper;

    public DefaultAuditService(AuditEventRepository events,
                               AuditEventChainRepository chain,
                               PortalProperties properties,
                               ObjectMapper objectMapper) {
        this.events = events;
        this.chain = chain;
        this.hashCalculator = new AuditHashCalculator(properties.getAudit().getHashAlgorithm());
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID append(AuditContext ctx) {
        AuditEventChainEntity tail = chain.lockForUpdate(DEFAULT_STREAM)
                .orElseGet(() -> chain.save(new AuditEventChainEntity(DEFAULT_STREAM)));

        long nextSeq = tail.getLastSequence() + 1;
        UUID eventId = UUID.randomUUID();
        // Match PostgreSQL timestamptz microsecond precision so integrity
        // verification recomputes the same hash after a DB round-trip.
        OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);

        AuditEventEntity event = new AuditEventEntity(eventId);
        event.setOccurredAt(now);
        event.setRecordedAt(now);
        event.setEventType(ctx.eventType());
        event.setCategory(ctx.category() == null ? "SYSTEM" : ctx.category());
        event.setSeverity(AuditSeverity.valueOf(
                ctx.severity() == null ? "INFO" : ctx.severity().name()));
        event.setActorType(ctx.actorType() == null ? "SYSTEM" : ctx.actorType());
        event.setActorId(ctx.actorId());
        event.setActorDisplay(ctx.actorDisplay());
        event.setTargetType(ctx.targetType());
        event.setTargetId(ctx.targetId());
        event.setTargetDisplay(ctx.targetDisplay());
        // action is NOT NULL — default to event type when callers omit it.
        event.setAction(ctx.action() == null || ctx.action().isBlank()
                ? ctx.eventType() : ctx.action());
        event.setOutcome(ctx.outcome() == null ? "SUCCESS" : ctx.outcome().name());
        event.setIpAddress(ctx.ipAddress());
        event.setUserAgent(ctx.userAgent());
        event.setCorrelationId(ctx.correlationId());
        event.setRequestId(ctx.requestId());
        event.setTraceId(ctx.traceId());
        event.setSessionId(ctx.sessionId());
        event.setPayloadJson(serializePayload(ctx.payload()));
        event.setStream(DEFAULT_STREAM);
        event.setSequenceNumber(nextSeq);
        event.setPreviousHash(tail.getLastHash());

        String currentHash = hashCalculator.compute(event, tail.getLastHash());
        event.setCurrentHash(currentHash);

        events.save(event);

        tail.setLastSequence(nextSeq);
        tail.setLastHash(currentHash);
        tail.setLastEventId(eventId);
        tail.setUpdatedAt(now);
        chain.save(tail);

        if (log.isDebugEnabled()) {
            log.debug("Audit appended: seq={} type={} actor={} target={}/{}",
                    nextSeq, ctx.eventType(), ctx.actorId(),
                    ctx.targetType(), ctx.targetId());
        }
        return eventId;
    }

    private String serializePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (RuntimeException e) {
            log.warn("Audit payload could not be serialized; storing empty object", e);
            return "{}";
        }
    }
}
