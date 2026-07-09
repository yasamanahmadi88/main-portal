package com.company.portal.audit.repository;

import com.company.portal.audit.domain.AuditEventEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {

    Optional<AuditEventEntity> findByStreamAndSequenceNumber(String stream, long sequenceNumber);

    @Query("""
            SELECT e FROM AuditEventEntity e
            WHERE (:from IS NULL OR e.occurredAt >= :from)
              AND (:to   IS NULL OR e.occurredAt <  :to)
              AND (:actorId IS NULL OR e.actorId = :actorId)
              AND (:eventType IS NULL OR e.eventType = :eventType)
              AND (:category IS NULL OR e.category = :category)
            """)
    Page<AuditEventEntity> search(@Param("from") OffsetDateTime from,
                                  @Param("to") OffsetDateTime to,
                                  @Param("actorId") UUID actorId,
                                  @Param("eventType") String eventType,
                                  @Param("category") String category,
                                  Pageable pageable);

    @Query("""
            SELECT e FROM AuditEventEntity e
            WHERE e.stream = :stream
              AND e.sequenceNumber > :fromSeq
              AND e.sequenceNumber <= :toSeq
            ORDER BY e.sequenceNumber ASC
            """)
    List<AuditEventEntity> loadRange(@Param("stream") String stream,
                                     @Param("fromSeq") long fromSeq,
                                     @Param("toSeq") long toSeq);

    @Query("SELECT MAX(e.sequenceNumber) FROM AuditEventEntity e WHERE e.stream = :stream")
    Long maxSequence(@Param("stream") String stream);
}
