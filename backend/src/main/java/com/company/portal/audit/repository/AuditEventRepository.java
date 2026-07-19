package com.company.portal.audit.repository;

import com.company.portal.audit.domain.AuditEventEntity;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEventRepository
        extends JpaRepository<AuditEventEntity, UUID>, JpaSpecificationExecutor<AuditEventEntity> {

    Optional<AuditEventEntity> findByStreamAndSequenceNumber(String stream, long sequenceNumber);

    /**
     * Dynamic search — avoids Hibernate 6 issues with typed {@code NULL} bind
     * parameters in {@code (:param IS NULL OR ...)} JPQL predicates.
     */
    default Page<AuditEventEntity> search(OffsetDateTime from,
                                          OffsetDateTime to,
                                          UUID actorId,
                                          String eventType,
                                          String category,
                                          Pageable pageable) {
        Specification<AuditEventEntity> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> preds = new ArrayList<>();
            if (from != null) {
                preds.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                preds.add(cb.lessThan(root.get("occurredAt"), to));
            }
            if (actorId != null) {
                preds.add(cb.equal(root.get("actorId"), actorId));
            }
            if (eventType != null && !eventType.isBlank()) {
                preds.add(cb.equal(root.get("eventType"), eventType));
            }
            if (category != null && !category.isBlank()) {
                preds.add(cb.equal(root.get("category"), category));
            }
            return cb.and(preds.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return findAll(spec, pageable);
    }

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
