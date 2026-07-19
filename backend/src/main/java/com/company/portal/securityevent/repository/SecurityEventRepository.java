package com.company.portal.securityevent.repository;

import com.company.portal.securityevent.domain.SecurityEventEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SecurityEventRepository extends JpaRepository<SecurityEventEntity, UUID> {

    @Query("""
            SELECT e FROM SecurityEventEntity e
            WHERE (:from IS NULL OR e.occurredAt >= :from)
              AND (:to IS NULL OR e.occurredAt < :to)
              AND (:eventType IS NULL OR e.eventType = :eventType)
              AND (:acknowledged IS NULL
                    OR (:acknowledged = TRUE AND e.acknowledgedAt IS NOT NULL)
                    OR (:acknowledged = FALSE AND e.acknowledgedAt IS NULL))
            """)
    Page<SecurityEventEntity> search(@Param("from") OffsetDateTime from,
                                     @Param("to") OffsetDateTime to,
                                     @Param("eventType") String eventType,
                                     @Param("acknowledged") Boolean acknowledged,
                                     Pageable pageable);

    long countByAcknowledgedAtIsNull();

    long countByAcknowledgedAtIsNullAndSeverity(com.company.portal.securityevent.domain.SecurityEventSeverity severity);
}
