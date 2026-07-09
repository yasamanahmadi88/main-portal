package com.company.portal.dashboard.application;

import com.company.portal.audit.repository.AuditEventRepository;
import com.company.portal.identity.domain.UserStatus;
import com.company.portal.identity.repository.UserRepository;
import com.company.portal.identity.repository.UserSessionMetadataRepository;
import com.company.portal.securityevent.domain.SecurityEventSeverity;
import com.company.portal.securityevent.repository.SecurityEventRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardQueryService {

    private final UserRepository users;
    private final UserSessionMetadataRepository sessions;
    private final SecurityEventRepository securityEvents;
    private final AuditEventRepository auditEvents;

    public DashboardQueryService(UserRepository users,
                                 UserSessionMetadataRepository sessions,
                                 SecurityEventRepository securityEvents,
                                 AuditEventRepository auditEvents) {
        this.users = users;
        this.sessions = sessions;
        this.securityEvents = securityEvents;
        this.auditEvents = auditEvents;
    }

    public record DashboardSummary(long activeUsers, long activeSessions,
                                   SecuritySummary securityEvents, long auditEventsToday) { }

    public record SecuritySummary(long unacknowledged, long critical) { }

    @Transactional(readOnly = true)
    public DashboardSummary summary() {
        long activeUsers = users.countByStatusAndDeletedAtIsNull(UserStatus.ACTIVE);
        long activeSessions = sessions.countByRevokedAtIsNullAndLastSeenAtAfter(
                OffsetDateTime.now().minusHours(24));
        long unacknowledged = securityEvents.countByAcknowledgedAtIsNull();
        long critical = securityEvents.countByAcknowledgedAtIsNullAndSeverity(SecurityEventSeverity.CRITICAL);

        OffsetDateTime startOfToday = OffsetDateTime.now()
                .toLocalDate().atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        // Rough count: sequence numbers stay monotonic per-stream, so infer
        // "today" by loading a batch whose occurred_at is today. For accuracy
        // we use the repository search API.
        long auditToday = auditEvents.search(startOfToday, null, null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();

        return new DashboardSummary(activeUsers, activeSessions,
                new SecuritySummary(unacknowledged, critical), auditToday);
    }
}
