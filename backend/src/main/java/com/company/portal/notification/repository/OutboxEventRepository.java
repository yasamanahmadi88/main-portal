package com.company.portal.notification.repository;

import com.company.portal.notification.domain.OutboxEventEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Query("""
            SELECT o FROM OutboxEventEntity o
            WHERE o.status = 'PENDING'
              AND o.nextAttemptAt <= :now
            ORDER BY o.nextAttemptAt ASC
            """)
    List<OutboxEventEntity> findDue(@Param("now") OffsetDateTime now, Pageable pageable);
}
